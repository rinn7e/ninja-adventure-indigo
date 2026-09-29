package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.Types.*
import ninja.generated.Assets
import ninja.scene.worldscene.Type.*
import ninja.ui.CharacterSpriteUI.{characterUI, pigUI}

/** Everything that moves or breaks, as (sort y, nodes) so the scene can y-sort it with the wall
  * tiles.
  */
object ActorsUI:

  /** Godot's `SpriteCharacter`: 6 frames per second; walking cycles rows 0-3, standing is row 0. */
  private def row(actor: Actor, now: Seconds): Int =
    if actor.moving then ((now - actor.animSince).toDouble * 6).toInt % 4 else 0

  private def characterAtUI(
      sheet: AssetName,
      actor: Actor,
      now: Seconds
  ): (Double, Batch[SceneNode]) =
    (actor.position.y, characterUI(sheet, actor.facing, row(actor, now), actor.position))

  def actorsUI(model: Model, now: Seconds): Batch[(Double, Batch[SceneNode])] =
    val pig      = model.pig
    val pigFrame = if pig.moving then ((now - pig.animSince).toDouble * 6).toInt % 2 else 0
    Batch(
      characterAtUI(Assets.assets.ninjaBlue, model.player, now),
      characterAtUI(Assets.assets.samuraiGreen, model.follower, now),
      characterAtUI(Assets.assets.samuraiBlue, model.patroller.actor, now),
      (pig.position.y, pigUI(pigFrame, pig.flip, pig.position))
    ) ++ model.props.zipWithIndex.map { case (prop, i) => (prop.position.y, propUI(prop, i, now)) }

  // --- Props --------------------------------------------------------------

  private final case class PropLook(
      sprite: AssetName,
      size: Size,
      particles: AssetName,
      frames: Int,
      frame: Size
  )

  private def look(kind: PropKind): PropLook =
    kind match
      case PropKind.Grass =>
        PropLook(Assets.assets.grass, Size(16, 16), Assets.assets.particleGrass, 6, Size(12, 13))
      case PropKind.Pot =>
        PropLook(Assets.assets.pot, Size(14, 16), Assets.assets.particlePot, 6, Size(14, 14))
      case PropKind.Crate =>
        PropLook(Assets.assets.crate, Size(14, 15), Assets.assets.particleRock, 5, Size(16, 16))

  /** How long the sprite stays (flashing and shaking) once broken, and how long the burst lasts. */
  private val shakeTime: Double = 0.1
  private val burstTime: Double = 0.8 / 1.5

  private def propUI(prop: Prop, seed: Int, now: Seconds): Batch[SceneNode] =
    val l = look(prop.kind)
    val topLeft =
      Point(prop.position.x.toInt - l.size.width / 2, prop.position.y.toInt - l.size.height / 2)
    prop.brokenAt match
      case None =>
        Batch(Graphic(l.size.width, l.size.height, Material.Bitmap(l.sprite)).moveTo(topLeft))

      case Some(at) =>
        val t = (now - at).toDouble
        // Godot's `damage_fx`: flash white and shake for 0.1s, then hide; the particles burst.
        val shake =
          if t < shakeTime / 3 then Point(-1, -1)
          else if t < shakeTime * 2 / 3 then Point(-1, 0)
          else Point.zero
        val flashing =
          if t < shakeTime then
            Batch(
              Graphic(
                l.size.width,
                l.size.height,
                Material.ImageEffects(l.sprite).withOverlay(Fill.Color(RGBA.White))
              )
                .moveTo(topLeft + shake)
            )
          else Batch.empty
        flashing ++ burstUI(prop, l, seed, t)

  /** Deterministic pseudo-random number in [0, 1) for a particle. */
  private def noise(seed: Int, n: Int): Double =
    val x = Math.sin(seed * 12.9898 + n * 78.233) * 43758.5453
    x - Math.floor(x)

  /** Godot's `particle_process_explosion`: 8 particles from a circle of radius 10, thrown up within
    * 60 degrees at 30-100 px/s, slowed by damping (50-100) and pulled down by gravity (100); each
    * shows a random frame of the sheet and fades out at the end of its life.
    */
  private def burstUI(prop: Prop, l: PropLook, seed: Int, time: Double): Batch[SceneNode] =
    if time > burstTime then Batch.empty
    else
      Batch.fromIndexedSeq((0 until 8).flatMap { i =>
        def rnd(n: Int) = noise(seed * 8 + i, n)
        val lifetime    = burstTime * (1 - 0.4 * rnd(0))
        if time > lifetime then None
        else
          val t        = time * 1.5
          val angle    = -Math.PI / 2 + (rnd(1) * 2 - 1) * Math.toRadians(60)
          val speed    = 30 + 70 * rnd(2)
          val damping  = 50 + 50 * rnd(3)
          val moving   = Math.min(t, speed / damping)
          val distance = speed * moving - damping * moving * moving / 2
          val start =
            Vector2(Math.cos(rnd(4) * 2 * Math.PI), Math.sin(rnd(4) * 2 * Math.PI)) * (10 * rnd(5))
          val offset =
            start + Vector2(Math.cos(angle), Math.sin(angle)) * distance + Vector2(0, 50 * t * t)
          val at    = prop.position + Vector2(0, -3) + offset
          val life  = time / lifetime
          val alpha = if life < 0.785 then 1.0 else 1 - (life - 0.785) / 0.215
          val frame = (rnd(6) * l.frames).toInt
          Some(
            Graphic(
              l.frame.width,
              l.frame.height,
              Material.ImageEffects(l.particles).withAlpha(alpha)
            )
              .withCrop(frame * l.frame.width, 0, l.frame.width, l.frame.height)
              .moveTo(Point(at.x.toInt - l.frame.width / 2, at.y.toInt - l.frame.height / 2))
          )
      })
