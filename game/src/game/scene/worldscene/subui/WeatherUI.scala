package game.scene.worldscene.subui

import indigo.*
import game.common.Types.*
import game.common.constant.Layout
import game.generated.Assets

/** The zone's weather (Godot: particle emitters attached to the camera, and a fog overlay), laid
  * out in screen space. Each effect is a pure function of the time, and fades in over 2 seconds
  * after the player walks into a zone that has it.
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

  private val rainDirection: Vector2 = Vector2(-0.5, 1).normalise

  /** Where the i-th raindrop is at `time`: 30 streaks at 100 px/s, down and to the left. */
  private def raindrop(i: Int, time: Double): Vector2 =
    val (spawn, life) = particle(i, 30, 1.0, Size(200, 140), time, 1)
    spawn + rainDirection * (100 * life)

  private def rainUI(time: Double, strength: Double): Batch[SceneNode] =
    Batch.fromIndexedSeq((0 until 30).map { i =>
      val at = raindrop(i, time)
      Graphic(2, 4, Material.ImageEffects(Assets.assets.fxRain).withAlpha(strength))
        .moveTo(at.x.toInt, at.y.toInt)
    })

  /** Rain on the floor (Godot's `RainOnFloor` sub-emitter): drops leave splashes where they are,
    * each playing its 3 frames over 0.5s. Godot emits 4 a second per drop but keeps at most 30
    * alive, so about one per drop at a time: here, one every 0.5s.
    */
  private def rainOnFloorUI(time: Double, strength: Double): Batch[SceneNode] =
    val splashLife = 0.5
    Batch.fromIndexedSeq((0 until 30).map { i =>
      // Each drop's splashes are staggered like the drops themselves.
      val offset  = i.toDouble / 30
      val emitted = Math.floor((time + offset) / splashLife) * splashLife - offset
      val age     = (time - emitted) / splashLife
      val at      = raindrop(i, emitted) + Vector2(1, 2) - Vector2(4, 4)
      Graphic(8, 8, Material.ImageEffects(Assets.assets.fxRainOnFloor).withAlpha(strength))
        .withCrop((age * 3).toInt.min(2) * 8, 0, 8, 8)
        .moveTo(at.x.toInt, at.y.toInt)
    })

  /** Snow (Godot 4's unused `Snow` emitter): 20 flakes a second in a 400x280 box, drifting down and
    * to the left at 5-30 px/s, each a random frame of `snow.png`, fading in and out.
    */
  private def snowUI(time: Double, strength: Double): Batch[SceneNode] =
    val direction = Vector2(-0.5, 1).normalise
    Batch.fromIndexedSeq((0 until 20).map { i =>
      val (spawn, life) = particle(i, 20, 1.0, Size(200, 140), time, 5)
      val at            = spawn + direction * ((5 + 25 * noise(i, 11)) * life)
      val alpha =
        if life < 0.128 then life / 0.128 else if life > 0.884 then (1 - life) / 0.116 else 1
      Graphic(8, 8, Material.ImageEffects(Assets.assets.fxSnow).withAlpha(strength * alpha))
        .withCrop((noise(i, 13) * 7).toInt * 8, 0, 8, 8)
        .moveTo(at.x.toInt - 4, at.y.toInt - 4)
    })

  /** Sparks (the Godot 3 version's `Spark` emitter): 10 motes rising at 5 px/s (±50%) in a 200x200
    * box, playing `Spark.png`'s 7 frames over their 1s life from a random frame, fading at the end.
    */
  private def sparkUI(time: Double, strength: Double): Batch[SceneNode] =
    Batch.fromIndexedSeq((0 until 10).map { i =>
      val (spawn, life) = particle(i, 10, 1.0, Size(100, 100), time, 6)
      val at            = spawn + Vector2(0, -(5 * (0.5 + noise(i, 17))) * life)
      val alpha         = if life > 2.0 / 3 then (1 - life) * 3 else 1
      val frame         = ((noise(i, 19) + life) * 7).toInt % 7
      Graphic(10, 8, Material.ImageEffects(Assets.assets.fxSpark).withAlpha(strength * alpha))
        .withCrop(frame * 10, 0, 10, 8)
        .moveTo(at.x.toInt - 5, at.y.toInt - 4)
    })

  /** Leaves: 10 drifting, spinning (6 frames) leaves, living 3 seconds each. */
  private def leafUI(time: Double, strength: Double): Batch[SceneNode] =
    val direction = rainDirection
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

  private def strengthOf(since: Seconds, now: Seconds): Double =
    ((now - since).toDouble / fadeIn).min(1.0)

  /** The effects Godot draws at z 0 before the map, so on the floor but under walls, trees and
    * characters, in this order: snow, rain, its splashes, cloud shadows.
    */
  def groundWeatherUI(environment: Environment, since: Seconds, now: Seconds): Batch[SceneNode] =
    val time     = now.toDouble
    val strength = strengthOf(since, now)
    val meteo    = environment.meteo
    (if meteo.contains(Meteo.Snow) then snowUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Rain) then rainUI(time, strength) ++ rainOnFloorUI(time, strength)
       else Batch.empty) ++
      (if meteo.contains(Meteo.Cloud) then cloudUI(time, strength) else Batch.empty)

  /** The effects Godot draws above the map: leaves (z 3), light rays (z 6), fog (z 10). */
  def skyWeatherUI(environment: Environment, since: Seconds, now: Seconds): Batch[SceneNode] =
    val time     = now.toDouble
    val strength = strengthOf(since, now)
    val meteo    = environment.meteo
    (if meteo.contains(Meteo.Leaf) then leafUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Ray) then rayUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Spark) then sparkUI(time, strength) else Batch.empty) ++
      (if meteo.contains(Meteo.Fog) then fogUI(time, strength) else Batch.empty)
