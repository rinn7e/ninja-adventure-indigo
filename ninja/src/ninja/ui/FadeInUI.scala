package ninja.ui

import indigo.*
import ninja.common.constant.{Layers, Layout}
import ninja.theme.Palette

object FadeInUI:

  val duration: Seconds = Seconds(0.3)

  /** A veil over the whole screen that fades out over `duration` after `since`. Merge it into a
    * scene's UI with `|+|`.
    */
  def fadeInUI(since: Seconds, now: Seconds): SceneUpdateFragment =
    val progress = (now - since).toDouble / duration.toDouble
    if progress >= 1 then SceneUpdateFragment.empty
    else
      SceneUpdateFragment(
        Layers.screen -> Layer.Content(
          Quad(Rectangle(Layout.screen), Fill.Color(Palette.ink.withAlpha(1 - progress.max(0))))
        )
      )
