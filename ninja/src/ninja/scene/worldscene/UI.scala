package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*
import ninja.common.asset.GameAssets
import ninja.common.constant.{Layers, Layout}
import ninja.common.util.Screen
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.common.Util.viewTopLeft
import ninja.common.constant.Combat
import ninja.scene.worldscene.subui.ActorsUI.{actorsUI, impactsUI}
import ninja.scene.worldscene.subui.TutorialUI.tutorialUI
import ninja.scene.worldscene.subui.TilemapUI.{
  floorUI,
  northFloorUI,
  northRowsUI,
  northTopUI,
  tilesCloneBlanksUI,
  topUI,
  wallRowsUI
}
import ninja.scene.worldscene.subui.WeatherUI.{groundWeatherUI, skyWeatherUI}
import ninja.theme.Palette
import ninja.ui.ColorGradingUI.gradingUI
import ninja.ui.FadeInUI.fadeInUI
import ninja.ui.HeartBarUI.heartBarUI

object UI:

  /** Godot's transition on teleport: instantly black, then (after the 0.1s teleport) a 0.3s fade.
    */
  private def teleportUI(teleportedAt: Option[Seconds], now: Seconds): Batch[SceneNode] =
    teleportedAt match
      case Some(at) if now < at + Seconds(0.4) =>
        val t     = (now - at).toDouble
        val alpha = if t < 0.1 then 1.0 else 1 - (t - 0.1) / 0.3
        Batch(Quad(Rectangle(Layout.screen), Fill.Color(Palette.ink.withAlpha(alpha))))

      case _ =>
        Batch.empty

  /** Godot 3's revive fade: black for 0.3s, clear by 0.5s. */
  private def reviveUI(revivedAt: Option[Seconds], now: Seconds): Batch[SceneNode] =
    revivedAt.map(at => (now - at).toDouble) match
      case Some(t) if t >= 0 && t < Combat.fadeOut.toDouble =>
        val hold  = Combat.fadeHold.toDouble
        val alpha = if t < hold then 1.0 else 1 - (t - hold) / (Combat.fadeOut.toDouble - hold)
        Batch(Quad(Rectangle(Layout.screen), Fill.Color(RGBA.Black.withAlpha(alpha))))
      case _ =>
        Batch.empty

  /** The debug overlay (F3): the player's world position, top right. */
  private def debugUI(model: Model): Batch[SceneNode] =
    if !model.debug then Batch.empty
    else
      val p = model.player.position
      Batch(
        GameAssets
          .textUI(s"${p.x.toInt},${p.y.toInt}", Layout.screen.width - 3, 3, RGBA.White)
          .alignRight
      )

  /** The zone's music: the previous track fades out over a second, then the new one plays. */
  private def musicUI(model: Model, now: Seconds): SceneAudio =
    val t = (now - model.musicSince).toDouble
    def source(music: Music, volume: Double): SceneAudioSource =
      SceneAudioSource(
        BindingKey(s"music-$music"),
        PlaybackPattern.SingleTrackLoop(Track(GameAssets.musicAsset(music))),
        Volume(volume)
      )
    (model.previousMusic, model.music) match
      case (Some(old), _) if t < 1 => SceneAudio(source(old, 1 - t))
      case (_, Some(current))      => SceneAudio(source(current, 1))
      case _                       => SceneAudio.Mute

  def ui(shared: Shared, model: Model): SceneUpdateFragment =
    val now = shared.now

    // Walls and characters, drawn back to front by their feet (Godot's y-sort).
    val sortedUI =
      ((wallRowsUI ++ northRowsUI)
        .map { case (y, node) => (y, Batch(node)) } ++ actorsUI(model, now))
        .sortBy(_._1)
        .flatMap(_._2)

    // Rain and cloud shadows sit on the floor, moving with the camera (they're in screen space).
    val groundUI =
      Group(groundWeatherUI(model.environment, model.environmentSince, now))
        .moveTo(viewTopLeft(now)(model.camera))

    SceneUpdateFragment(
      Layers.world -> Layer
        .Content(
          (floorUI(now) ++ northFloorUI :+ groundUI) ++ sortedUI ++ topUI ++ northTopUI ++
            impactsUI(model, now)
        )
        .withCamera(Screen.centredCamera(shared.viewport)(viewTopLeft(now)(model.camera))),
      Layers.weather -> Layer.Content(skyWeatherUI(model.environment, model.environmentSince, now)),
      Layers.ui -> Layer.Content(
        heartBarUI(model.lifeShown.toInt, Update.maxLife, Point(3, 3)) ++
          tutorialUI(model.tutorial, now) ++ debugUI(model)
      ),
      Layers.screen -> Layer.Content(
        teleportUI(model.teleportedAt, now) ++ reviveUI(model.revivedAt, now)
      )
    ).addCloneBlanks(tilesCloneBlanksUI)
      .withAudio(musicUI(model, now)) |+| fadeInUI(model.enteredAt, now) |+|
      gradingUI(model.previousGrading, model.environment.grading, model.environmentSince, now)
