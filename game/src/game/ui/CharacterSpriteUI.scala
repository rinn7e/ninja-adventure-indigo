package game.ui

import indigo.*
import game.common.Types.*
import game.generated.Assets

/** Characters are drawn from the pack's sprite sheets: 16x16 frames, one column per facing and one
  * row per animation frame (Godot's `SpriteCharacter`), with a shadow at their feet.
  */
object CharacterSpriteUI:

  val frameSize: Int = 16

  private def pixel(at: Vector2): Point =
    Point(Math.round(at.x).toInt, Math.round(at.y).toInt)

  /** The shadow under a character, centred on its feet. */
  def shadowUI(feet: Vector2): Graphic[Material.Bitmap] =
    Graphic(12, 8, Material.Bitmap(Assets.assets.shadow)).moveTo(pixel(feet) - Point(6, 4))

  /** A character sheet frame; the sprite sits 6 pixels above the feet (Godot's `offset`). */
  def characterSpriteUI(
      sheet: AssetName,
      facing: Facing,
      row: Int,
      feet: Vector2
  ): Graphic[Material.Bitmap] =
    Graphic(frameSize, frameSize, Material.Bitmap(sheet))
      .withCrop(facing.column * frameSize, row * frameSize, frameSize, frameSize)
      .moveTo(pixel(feet) - Point(8, 14))

  /** A character with its shadow. */
  def characterUI(sheet: AssetName, facing: Facing, row: Int, feet: Vector2): Batch[SceneNode] =
    Batch(shadowUI(feet), characterSpriteUI(sheet, facing, row, feet))

  /** The pig: a two-frame strip, flipped when walking right, drawn 5 pixels above its feet. */
  def pigUI(frame: Int, flip: Boolean, feet: Vector2): Batch[SceneNode] =
    Batch(
      shadowUI(feet),
      Graphic(frameSize, frameSize, Material.Bitmap(Assets.assets.pig))
        .withCrop(frame * frameSize, 0, frameSize, frameSize)
        .withRef(8, 8)
        .flipHorizontal(flip)
        .moveTo(pixel(feet) - Point(0, 5))
    )
