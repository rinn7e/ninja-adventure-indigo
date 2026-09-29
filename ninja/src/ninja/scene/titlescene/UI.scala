package ninja.scene.titlescene

import indigo.*
import ninja.common.Types.*
import ninja.common.asset.GameAssets
import ninja.common.constant.{Layers, Layout}
import ninja.generated.Assets
import ninja.scene.titlescene.Type.*
import ninja.theme.Palette
import ninja.ui.CharacterSpriteUI.characterUI
import ninja.ui.FadeInUI.fadeInUI
import ninja.ui.LabelUI.blinkingLabelUI

object UI:

  def ui(shared: Shared, model: Model): SceneUpdateFragment =
    val now  = shared.now
    val walk = (now.toDouble * 6).toInt % 4

    val titleUI =
      Group(GameAssets.textUI("Ninja Adventure", 0, 0, Palette.text).alignCenter)
        .scaleBy(2, 2)
        .moveTo(Layout.screen.width / 2, 40)

    SceneUpdateFragment(
      Layers.world -> Layer.Content(
        Batch(Quad(Rectangle(Layout.screen), Fill.Color(Palette.ink)), titleUI) ++
          characterUI(Assets.assets.ninjaBlue, Facing.Down, walk, Vector2(160, 104))
      ),
      Layers.ui -> Layer.Content(blinkingLabelUI("Press Start", 134, Palette.gold, now))
    ) |+| fadeInUI(model.enteredAt, now)
