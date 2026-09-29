package ninja.common.asset

import indigo.*
import ninja.common.Types.*
import ninja.generated.{Assets, NormalFont}

object GameAssets:

  /** Everything in `ninja/assets/`. The generator lists each subfolder separately, so the generated
    * font sheet (`assets/generated/`) is added explicitly.
    */
  def assets(baseUrl: String): Set[AssetType] =
    Assets.assets.assetSetRelativeTo(baseUrl) ++
      Assets.assets.generated.assetSetRelativeTo(baseUrl)

  /** The pack's pixel font, rasterised at build time (`embedFont` in build.mill). Its space is 1px
    * wide, which runs words together ("PressStart"), so it's widened to 3px (the sheet's space cell
    * is blank).
    */
  val fontInfo: FontInfo =
    NormalFont.fontInfo.addChar(FontChar(" ", 81, 12, 3, 12))

  private val fontMaterial: Material.ImageEffects =
    Material.ImageEffects(Assets.assets.generated.NormalFont)

  def textUI(message: String, x: Int, y: Int, color: RGBA): Text[Material.ImageEffects] =
    Text(message, x, y, NormalFont.fontKey, fontMaterial.withTint(color))

  def musicAsset(music: Music): AssetName =
    music match
      case Music.Dream => Assets.assets.musicDream
      case Music.Swamp => Assets.assets.musicSwamp
