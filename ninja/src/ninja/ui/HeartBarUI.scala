package ninja.ui

import indigo.*
import ninja.generated.Assets

object HeartBarUI:

  /** Life points per heart (Godot's `receptacle_size`). */
  val perHeart: Int = 4

  /** Godot's receptacle bar: one heart per 4 life points, each drawn from `heart.png`, whose five
    * frames show 0 to 4 quarters.
    */
  def heartBarUI(life: Int, maxLife: Int, at: Point): Batch[SceneNode] =
    val hearts = Math.ceil(maxLife / perHeart.toDouble).toInt
    Batch.fromIndexedSeq((0 until hearts).map { i =>
      val quarters = (life - i * perHeart).max(0).min(perHeart)
      Graphic(16, 16, Material.Bitmap(Assets.assets.heart))
        .withCrop(quarters * 16, 0, 16, 16)
        .moveTo(at + Point(i * 16, 0))
    })
