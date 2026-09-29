package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Combat, NorthVillage}
import ninja.common.util.Motion.directionOf
import ninja.generated.Assets
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.Update.{isGone, isSwinging}
import ninja.scene.worldscene.common.Util.{VillagerPose, spriteCentre, villagerPose}
import ninja.ui.CharacterSpriteUI.{characterUI, pigUI, shadowUI}

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
      (model.player.position.y, playerUI(model, now)),
      characterAtUI(Assets.assets.samuraiGreen, model.follower, now),
      characterAtUI(Assets.assets.samuraiBlue, model.patroller.actor, now),
      (pig.position.y, pigUI(pigFrame, pig.flip, pig.position))
    ) ++ model.props.zipWithIndex.map { case (prop, i) =>
      (prop.position.y, propUI(prop, i, now))
    } ++
      model.monsters.filterNot(isGone(now)).map(m => (m.position.y, monsterUI(m, now))) ++
      NorthVillage.smokes.filterNot(_.onTop).map(s => (s.position.y, smokeUI(s, now))) ++
      NorthVillage.villagers.zip(model.villagers).map { case (v, s) =>
        val pose = villagerPose(now)(v, s)
        (pose.position.y, villagerUI(v, pose))
      }

  // --- Smoke (from the Godot 3 version) -------------------------------------

  /** Godot 3's `Smoke`: each puff lives 2s (lifetime 1 at speed scale 0.5), rising at 30 px/s
    * within 20° of straight up, slowed by damping (5), playing its 6 frames of 32x32.
    */
  def smokeUI(smoke: SmokeEmitter, now: Seconds): Batch[SceneNode] =
    Batch.fromIndexedSeq((0 until smoke.puffs).map { i =>
      val clock = now.toDouble / 2 + i.toDouble / smoke.puffs
      val life  = clock - Math.floor(clock)
      val angle =
        -Math.PI / 2 + (noise(i * 31 + Math.floor(clock).toInt, 3) * 2 - 1) * Math.toRadians(20)
      val distance = 30 * life - 2.5 * life * life
      val at       = smoke.position + Vector2(Math.cos(angle), Math.sin(angle)) * distance
      Graphic(32, 32, Material.ImageEffects(Assets.assets.fxSmoke).withTint(smoke.tint))
        .withCrop((life * 6).toInt.min(5) * 32, 0, 32, 32)
        .moveTo(Math.round(at.x).toInt - 16, Math.round(at.y).toInt - 16)
    })

  /** The smoke plumes drawn over everything (chimneys). */
  def smokeOnTopUI(now: Seconds): Batch[SceneNode] =
    NorthVillage.smokes.filter(_.onTop).flatMap(smokeUI(_, now))

  // --- Villagers (from the Godot 3 version) --------------------------------

  /** A villager, 8 pixels above its feet (Godot 3's `Npc/Sprite`), at 5 frames a second. */
  private def villagerUI(villager: Villager, pose: VillagerPose): Batch[SceneNode] =
    val step = (pose.clock * 5).toInt
    val (crop, extraY) =
      villager.sheetKind match
        case SheetKind.Standard =>
          (Point(pose.facing.column * 16, if pose.walking then (step % 4) * 16 else 0), 0)
        case SheetKind.BreathingIdle =>
          (Point(pose.facing.column * 16, (step % 2) * 16), 0)
        case SheetKind.Strip =>
          (Point(if pose.walking then (step % 2) * 16 else 0, 0), 3) // the dog's `offset` (0, 3)
    val feet = Point(Math.round(pose.position.x).toInt, Math.round(pose.position.y).toInt)
    Batch(
      shadowUI(pose.position),
      Graphic(16, 16, Material.Bitmap(villager.sheet))
        .withCrop(crop.x, crop.y, 16, 16)
        .withRef(8, 8)
        .flipHorizontal(pose.flip)
        .moveTo(feet + Point(0, -8 + extraY))
    )

  /** Godot 3's `DialogInfo`: a speech bubble (4 frames, 5 a second) 24 pixels above the villager in
    * talking range, popping up with an elastic ease over 0.5s. Hidden during a conversation.
    */
  def speechBubbleUI(model: Model, now: Seconds): Batch[SceneNode] =
    model.nearVillager match
      case Some((i, since)) if model.talk.isEmpty =>
        val villager = NorthVillage.villagers(i)
        val pose     = villagerPose(now)(villager, model.villagers(i))
        val t        = ((now - since).toDouble / 0.5).min(1)
        val elastic =
          if t >= 1 then 1.0
          else Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1
        val at = pose.position + villager.talkOffset + Vector2(0, -24 + 10 * (1 - elastic))
        Batch(
          Graphic(20, 16, Material.Bitmap(Assets.assets.dialogInfo))
            .withCrop(((now.toDouble * 5).toInt % 4) * 20, 0, 20, 16)
            .moveTo(Math.round(at.x).toInt - 10, Math.round(at.y).toInt - 8)
        )
      case _ =>
        Batch.empty

  // --- Combat (from the Godot 3 version) ----------------------------------

  /** Godot 3's `hit_fx`: over 0.2s (circular ease-out) the sprite shrinks from 2x to 1x and fades
    * from white. Returns the scale and the white overlay's strength.
    */
  private def hitEffect(since: Option[Seconds], now: Seconds): (Double, Double) =
    since.map(at => (now - at).toDouble / Combat.hitFlash.toDouble).filter(_ < 1) match
      case Some(t) =>
        val eased = Math.sqrt(1 - (1 - t.max(0)) * (1 - t.max(0)))
        (2 - eased, 1 - eased)
      case None =>
        (1, 0)

  /** A 16x16 sheet frame centred on `centre`, scaled and flashed white. */
  private def effectSpriteUI(
      sheet: AssetName,
      crop: Point,
      centre: Vector2,
      scale: Double,
      flash: Double
  ): Graphic[Material.ImageEffects] =
    Graphic(
      16,
      16,
      Material.ImageEffects(sheet).withOverlay(Fill.Color(RGBA.White.withAlpha(flash)))
    )
      .withCrop(crop.x, crop.y, 16, 16)
      .withRef(8, 8)
      .scaleBy(scale, scale)
      .moveTo(Math.round(centre.x).toInt, Math.round(centre.y).toInt)

  /** The player: dead (the death frame, row 6), swinging (the attack frame, row 4, with the lance),
    * or walking; flashing when hurt.
    */
  private def playerUI(model: Model, now: Seconds): Batch[SceneNode] =
    val player = model.player
    val (facing, frame) =
      if model.diedAt.isDefined then (Facing.Down, 6)
      else if isSwinging(now)(model) then (player.facing, 4)
      else (player.facing, row(player, now))
    val (scale, flash) = hitEffect(model.hurtAt, now)
    val spriteUI = effectSpriteUI(
      Assets.assets.ninjaBlue,
      Point(facing.column * 16, frame * 16),
      spriteCentre(player.position),
      scale,
      flash
    )
    val weaponUI = model.swingAt.filter(_ => isSwinging(now)(model)).map(lanceUI(player, _, now))
    // The lance is behind the player unless it points down (Godot 3's `Weapon.z_index`).
    if player.facing == Facing.Down then
      Batch(shadowUI(player.position), spriteUI) ++ Batch.fromOption(weaponUI)
    else Batch(shadowUI(player.position)) ++ Batch.fromOption(weaponUI) :+ spriteUI

  /** Godot 3's `Weapon.on_attack`: the lance, rotated to the facing, stabs from (px, py) to (px,
    * 10) in its own frame over the swing, with a quartic ease out-in.
    */
  private def lanceUI(player: Actor, swingAt: Seconds, now: Seconds): SceneNode =
    val direction = directionOf(player.facing)
    val (px, py) =
      player.facing match
        case Facing.Down  => (-2.0, 22.0)
        case Facing.Up    => (4.0, 16.0)
        case Facing.Left  => (4.0, 16.0)
        case Facing.Right => (-3.0, 16.0)
    val t = ((now - swingAt).toDouble / Combat.swingTime.toDouble).min(1)
    val eased =
      if t < 0.5 then (1 - Math.pow(1 - 2 * t, 4)) / 2 else Math.pow(2 * t - 1, 4) / 2 + 0.5
    val offset   = Vector2(px, py + (10 - py) * eased)
    val rotation = Math.atan2(direction.y, direction.x) - Math.PI / 2
    val rotated = Vector2(
      offset.x * Math.cos(rotation) - offset.y * Math.sin(rotation),
      offset.x * Math.sin(rotation) + offset.y * Math.cos(rotation)
    )
    val at = spriteCentre(player.position) + rotated
    Graphic(6, 16, Material.Bitmap(Assets.assets.weaponLance))
      .withRef(3, 8)
      .rotateTo(Radians(rotation))
      .moveTo(Math.round(at.x).toInt, Math.round(at.y).toInt)

  /** A Bamboo monster (Godot 3 only ever plays its "down" walk, 5 fps), 7 pixels above its origin,
    * with its shadow and mini life bar. Hit: flash and shrink from 2x; dead: shrink to nothing.
    */
  private def monsterUI(monster: Monster, now: Seconds): Batch[SceneNode] =
    val frame = (now.toDouble * 5).toInt % 4
    val (scale, flash) =
      monster.diedAt match
        case Some(at) =>
          val t = ((now - at).toDouble / Combat.hitFlash.toDouble).min(1)
          (2 - 2 * Math.sqrt(1 - (1 - t) * (1 - t)), 0.0)
        case None =>
          hitEffect(monster.hitAt, now)
    val origin = Point(Math.round(monster.position.x).toInt, Math.round(monster.position.y).toInt)
    val filled = Math.round(18.0 * monster.life.max(0) / Combat.monsterLife).toInt
    Batch(
      shadowUI(monster.position),
      effectSpriteUI(
        Assets.assets.monsterBamboo,
        Point(0, frame * 16),
        monster.position + Vector2(0, -7),
        scale,
        flash
      ),
      Graphic(18, 4, Material.Bitmap(Assets.assets.lifeBarMiniUnder))
        .moveTo(origin + Point(-9, -23))
    ) ++ Batch.fromOption(
      Option.when(filled > 0)(
        Graphic(18, 4, Material.Bitmap(Assets.assets.lifeBarMiniProgress))
          .withCrop(0, 0, filled, 4)
          .moveTo(origin + Point(-9, -23))
      )
    )

  /** Where the lance hit something: `Fx.png`, 4 frames of 32x32 over 0.1s. */
  def impactsUI(model: Model, now: Seconds): Batch[SceneNode] =
    model.impacts.flatMap { case (at, since) =>
      val t = (now - since).toDouble / Combat.impactTime.toDouble
      if t < 0 || t >= 1 then Batch.empty
      else
        Batch(
          Graphic(32, 32, Material.Bitmap(Assets.assets.fxImpact))
            .withCrop((t * 4).toInt * 32, 0, 32, 32)
            .moveTo(Math.round(at.x).toInt - 16, Math.round(at.y).toInt - 16)
        )
    }

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
      case PropKind.Plant =>
        PropLook(Assets.assets.plant, Size(16, 14), Assets.assets.particleGrass, 6, Size(12, 13))
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
