package ninja.common.constant

import indigo.*
import ninja.common.Types.*

/** The colour grading gradients, transcribed from Godot: each channel of the screen is mapped
  * through the gradient (red through its red, and so on). Stops are `(offset, colour)`, ascending.
  */
object Gradings:

  /** Most stops a gradient may have (the shader's fixed array size). */
  val maxStops: Int = 8

  def stopsOf(grading: Grading): Batch[(Double, RGB)] =
    grading match
      // `world.tscn`'s `ColorCorrection.gradient` (Gradient_kshit).
      case Grading.Initial =>
        Batch(
          0.0      -> RGB(0, 0, 0),
          0.723172 -> RGB(0.57992, 0.278237, 0.497348),
          1.0      -> RGB(1, 1, 1)
        )

      // `system/color_correction/gradient_neutral.tres`: Godot's default gradient, black to white,
      // which leaves every colour as it is.
      case Grading.Neutral =>
        Batch(0.0 -> RGB(0, 0, 0), 1.0 -> RGB(1, 1, 1))

      // `content/environment/swamp/gradient.tres`.
      case Grading.Swamp =>
        Batch(
          0.0      -> RGB(0.0364818, 0.0364818, 0.0364818),
          0.14802  -> RGB(0.222, 0.222, 0.222),
          0.315533 -> RGB(0.345988, 0.334159, 0.146841),
          0.665787 -> RGB(0.512765, 0.534101, 0.500432),
          0.868833 -> RGB(0.955279, 0.775057, 0.461007),
          1.0      -> RGB(0.95, 0.8626, 0.7315)
        )

  /** Godot tweens the old gradient into the new one over this. */
  val fade: Seconds = Seconds(0.5)
