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
    * wide, which runs words together ("PressStart"), so it's widened to 4px (the sheet's space cell
    * is blank).
    */
  val fontInfo: FontInfo =
    val space = FontChar(" ", 81, 12, 4, 12)
    // Replaced, not added: the font looks characters up in order, so the first space would win.
    NormalFont.fontInfo.copy(
      unknownChar = space,
      fontChars = NormalFont.fontInfo.fontChars.map(c => if c.character == " " then space else c)
    )

  private val fontMaterial: Material.ImageEffects =
    Material.ImageEffects(Assets.assets.generated.NormalFont)

  def textUI(message: String, x: Int, y: Int, color: RGBA): Text[Material.ImageEffects] =
    Text(message, x, y, NormalFont.fontKey, fontMaterial.withTint(color))

  def musicAsset(music: Music): AssetName =
    music match
      case Music.Dream       => Assets.assets.musicDream
      case Music.Swamp       => Assets.assets.musicSwamp
      case Music.Road        => Assets.assets.musicRoad
      case Music.Aquatic     => Assets.assets.musicAquatic
      case Music.Mystical    => Assets.assets.musicMystical
      case Music.Melancholia => Assets.assets.musicMelancholia
