package game.scene.worldscene.subui

import indigo.*
import game.common.constant.Combat
import game.generated.Assets
import game.scene.worldscene.Type.*

object TutorialUI:

  /** Godot 3's `Tuto`: a translucent grey band with the controls (`tutorial.png`), which slides
    * down 100 pixels over 1s (cubic ease-out) once hiding, then is gone.
    */
  def tutorialUI(tutorial: Tutorial, now: Seconds): Batch[SceneNode] =
    val t =
      tutorial.hidingAt.map(at => ((now - at).toDouble / Combat.tutorialSlide.toDouble).max(0))
    if t.exists(_ >= 1) then Batch.empty
    else
      val drop = Math.round(t.fold(0.0)(x => 100 * (1 - Math.pow(1 - x, 3)))).toInt
      Batch(
        Quad(
          Rectangle(-7, 122 + drop, 331, 66),
          Fill.Color(RGBA(0.321569, 0.321569, 0.321569, 0.654902))
        ),
        Graphic(226, 40, Material.Bitmap(Assets.assets.tutorial)).moveTo(46, 128 + drop)
      )
