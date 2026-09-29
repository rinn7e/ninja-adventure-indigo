package game.ui

import indigo.*
import game.common.asset.GameAssets
import game.common.constant.Layout

object LabelUI:

  def labelUI(message: String, x: Int, y: Int, color: RGBA): SceneNode =
    GameAssets.textUI(message, x, y, color)

  /** Centred horizontally on the screen. */
  def centeredLabelUI(message: String, y: Int, color: RGBA): SceneNode =
    GameAssets.textUI(message, Layout.screen.width / 2, y, color).alignCenter

  /** A prompt that blinks, driven purely by the running time. */
  def blinkingLabelUI(message: String, y: Int, color: RGBA, time: Seconds): Batch[SceneNode] =
    if (time.toDouble * 2).toInt % 2 == 0 then Batch(centeredLabelUI(message, y, color))
    else Batch.empty
