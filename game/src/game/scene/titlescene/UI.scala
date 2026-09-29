package game.scene.titlescene

import indigo.*
import game.common.Types.*
import game.common.asset.GameAssets
import game.common.constant.{Layers, Layout}
import game.common.util.Screen
import game.generated.Assets
import game.scene.titlescene.Type.*
import game.theme.Palette
import game.ui.CharacterSpriteUI.characterUI
import game.ui.FadeInUI.fadeInUI
import game.ui.LabelUI.blinkingLabelUI

object UI:

  def ui(shared: Shared, model: Model): SceneUpdateFragment =
    val now  = shared.now
    val walk = (now.toDouble * 6).toInt % 4

    val titleUI =
      Group(GameAssets.textUI("Ninja Adventure", 0, 0, Palette.text).alignCenter)
        .scaleBy(2, 2)
        .moveTo(Layout.screen.width / 2, 40)

    SceneUpdateFragment(
      Layers.world -> Layer
        .Content(
          Batch(Quad(Rectangle(Layout.screen), Fill.Color(Palette.ink)), titleUI) ++
            characterUI(Assets.assets.ninjaGreen, Facing.Down, walk, Vector2(160, 104))
        )
        .withCamera(Screen.centredCamera(shared.viewport)(Point.zero)),
      Layers.ui -> Layer.Content(blinkingLabelUI("Press Start", 134, Palette.gold, now))
    ) |+| fadeInUI(model.enteredAt, now)
