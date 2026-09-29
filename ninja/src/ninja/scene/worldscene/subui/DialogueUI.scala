package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.asset.GameAssets
import ninja.common.constant.{Layout, NorthVillage}
import ninja.generated.Assets
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.Update.{isTyped, typingSpeed}
import ninja.theme.Palette

object DialogueUI:

  /** Characters per line of the text area (the pixel font is about 6 pixels a character). */
  private val lineLength: Int = 40

  /** Words wrapped into lines of at most `lineLength` characters. */
  def wrap(text: String): Batch[String] =
    Batch.fromList(
      text
        .split(" ")
        .toList
        .foldLeft(List.empty[String]) {
          case (Nil, word) => List(word)
          case (line :: rest, word) if line.length + 1 + word.length <= lineLength =>
            (line + " " + word) :: rest
          case (lines, word) => word :: lines
        }
        .reverse
    )

  /** The Godot 3 version's dialogue box (`DialogBoxFaceset.png`, as in the asset pack's example
    * screenshot): the villager's portrait, its name on the tab, the line typing out, and a bobbing
    * arrow once it has.
    */
  def dialogueUI(talk: Option[Talk], now: Seconds): Batch[SceneNode] =
    talk match
      case None => Batch.empty
      case Some(t) =>
        val villager = NorthVillage.villagers(t.villager)
        val box      = Point((Layout.screen.width - 300) / 2, Layout.screen.height - 58 - 4)
        val line     = villager.lines(t.line)
        val shown    = ((now - t.since).toDouble * typingSpeed).toInt.min(line.length)
        val textUI = wrap(line.take(shown)).zipWithIndex.map { case (text, row) =>
          GameAssets.textUI(text, box.x + 54, box.y + 16 + row * 12, Palette.ink): SceneNode
        }
        val bob = if (now.toDouble * 4).toInt % 2 == 0 then 0 else 1
        Batch(
          Graphic(300, 58, Material.Bitmap(Assets.assets.dialogBox)).moveTo(box),
          Graphic(38, 38, Material.Bitmap(villager.faceset)).moveTo(box + Point(6, 14)),
          GameAssets.textUI(villager.name, box.x + 8, box.y - 2, Palette.text)
        ) ++ textUI ++ Batch.fromOption(
          Option.when(isTyped(t, now))(
            Graphic(13, 13, Material.Bitmap(Assets.assets.dialogArrow))
              .moveTo(box + Point(280, 40 + bob))
          )
        )
