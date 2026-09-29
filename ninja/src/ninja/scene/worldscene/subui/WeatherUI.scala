package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.Layout
import ninja.generated.Assets

/** The zone's weather (Godot: particle emitters attached to the camera, and a fog overlay), drawn
  * in screen space. Each effect is a pure function of the time, and fades in over 2 seconds after
  * the player walks into a zone that has it.
  */
object WeatherUI:

  private val fadeIn: Double = 2

  private def noise(n: Int, k: Int): Double =
    val x = Math.sin(n * 12.9898 + k * 78.233) * 43758.5453
    x - Math.floor(x)

  /** Where the i-th particle of an emitter is in its current life: its spawn point (random in
    * Godot's emission box, re-rolled every life) and how far through its life it is.
    */
  private def particle(
      i: Int,
      count: Int,
      lifetime: Double,
      box: Size,
      time: Double,
      salt: Int
  ): (Vector2, Double) =
    val clock  = time / lifetime + i.toDouble / count
    val life   = Math.floor(clock).toInt
    val centre = Vector2(Layout.screen.width / 2.0, Layout.screen.height / 2.0)
    val spawn = centre + Vector2(
      (noise(i + salt, life) * 2 - 1) * box.width,
      (noise(i + salt, life + 999) * 2 - 1) * box.height
    )
    (spawn, clock - life)

  private def fade(life: Double, in: Double, out: Double): Double =
    if life < in then life / in else if life > out then (1 - life) / (1 - out) else 1.0

  /** Rain: 30 streaks at 100 px/s, down and to the left. */
  private def rainUI(time: Double, strength: Double): Batch[SceneNode] =
    val direction = Vector2(-0.5, 1).normalise
    Batch.fromIndexedSeq((0 until 30).map { i =>
      val (spawn, life) = particle(i, 30, 1.0, Size(200, 140), time, 1)
      val at            = spawn + direction * (100 * life)
      Graphic(2, 4, Material.ImageEffects(Assets.assets.fxRain).withAlpha(strength))
        .moveTo(at.x.toInt, at.y.toInt)
    })

  /** Leaves: 10 drifting, spinning (6 frames) leaves, living 3 seconds each. */
  private def leafUI(time: Double, strength: Double): Batch[SceneNode] =
    val direction = Vector2(-0.5, 1).normalise
    Batch.fromIndexedSeq((0 until 10).map { i =>
      val (spawn, life) = particle(i, 10, 3.0, Size(200, 140), time, 2)
      val at            = spawn + direction * (30 * noise(i, 7) * 3 * life)
      val frame         = (life * 6 * 3).toInt % 6
      Graphic(
        12,
        7,
        Material.ImageEffects(Assets.assets.fxLeaf).withAlpha(strength * fade(life, 0.1, 0.9))
      )
        .withCrop(frame * 12, 0, 12, 7)
        .moveTo(at.x.toInt, at.y.toInt)
    })

  /** Cloud shadows: 6 slow, faint clouds drifting down and to the right, living 10 seconds. */
  private def cloudUI(time: Double, strength: Double): Batch[SceneNode] =
    val direction = Vector2(1, 1).normalise
    Batch.fromIndexedSeq((0 until 6).map { i =>
      val (spawn, life) = particle(i, 6, 10.0, Size(200, 160), time, 3)
      val at            = spawn + direction * (10 * 10 * life)
      Graphic(
        80,
        36,
        Material
          .ImageEffects(Assets.assets.fxCloud)
          .withAlpha(0.23 * strength * fade(life, 0.2, 0.8))
      )
        .moveTo(at.x.toInt - 40, at.y.toInt - 18)
    })

  /** Light rays: 10 faint, slowly shimmering shafts of light, living 10 seconds. */
  private def rayUI(time: Double, strength: Double): Batch[SceneNode] =
    Batch.fromIndexedSeq((0 until 10).map { i =>
      val (spawn, life) = particle(i, 10, 10.0, Size(160, 120), time, 4)
      val frame         = i % 3
      Graphic(
        72,
        102,
        Material
          .ImageEffects(Assets.assets.fxRaylight)
          .withAlpha(0.15 * strength * fade(life, 0.18, 0.86))
      )
        .withCrop(frame * 72, 0, 72, 102)
        .moveTo(spawn.x.toInt - 36, spawn.y.toInt - 51)
    })

  /** Fog: the fog texture tiled over the screen, scrolling slowly. */
  private def fogUI(time: Double, strength: Double): Batch[SceneNode] =
    val size   = Layout.cameraGrid
    val scroll = Point((time * 3.2).toInt % size.width, (time * 3.2).toInt % size.height)
    Batch.fromIndexedSeq(
      for {
        x <- -1 to 1
        y <- -1 to 1
      } yield Graphic(
        size.width,
        size.height,
        Material.ImageEffects(Assets.assets.fxFog).withAlpha(0.36 * strength)
      )
        .moveTo(x * size.width + scroll.x, y * size.height + scroll.y)
    )

  def weatherUI(environment: Environment, since: Seconds, now: Seconds): Batch[SceneNode] =
    val time     = now.toDouble
    val strength = ((now - since).toDouble / fadeIn).min(1.0)
    val meteo    = environment.meteo
    (if meteo.contains(Meteo.Cloud) then cloudUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Fog) then fogUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Rain) then rainUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Leaf) then leafUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Ray) then rayUI(time, strength) else Batch.empty)
