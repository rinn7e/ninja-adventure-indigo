package game.common.util

import indigo.*
import game.common.constant.Layout

/** Fitting the fixed 320x180 view into any window (browser or desktop), like Godot's
  * `stretch/mode = "viewport"`: scaled up by a whole number, centred, with black bars around it.
  * `viewport` is the window's size in screen pixels.
  */
object Screen:

  /** The largest whole-number scale at which the view still fits (at least 1). */
  def scaleFor(viewport: Size): Int =
    Math.min(viewport.width / Layout.screen.width, viewport.height / Layout.screen.height).max(1)

  /** The window's size in game pixels at that scale. */
  def windowFor(viewport: Size): Size =
    viewport / scaleFor(viewport)

  /** Where the view's top-left corner sits in the window, in game pixels. */
  def offsetFor(viewport: Size): Point =
    ((windowFor(viewport) - Layout.screen) / 2).toPoint.max(Point.zero)

  /** The camera that puts `topLeft` (in game pixels) at the view's top-left corner, centred in the
    * window. Screen-space layers use `topLeft = Point.zero`; the world follows the player.
    */
  def centredCamera(viewport: Size)(topLeft: Point): Camera =
    Camera.Fixed(topLeft - offsetFor(viewport))
