package game.common.constant

import indigo.*

/** The fixed screen of the Godot original: a 1280x720 window showing 320x180 game pixels at 4x
  * (Godot: viewport 1280x720, `CameraGrid` zoom 4).
  */
object Layout:

  val magnification: Int = 4

  /** What the camera shows, in game pixels. */
  val screen: Size = Size(320, 180)

  val tileSize: Int = 16

  /** The camera moves screen by screen, on this grid (Godot `CameraGrid.grid_size`). */
  val cameraGrid: Size = Size(320, 176)

  /** Godot `Camera2D.offset`: the view is centred on the grid cell's corner plus this. */
  val cameraOffset: Point = Point(24, 16)

  /** How long the camera takes to slide to the next screen. */
  val cameraSlide: Seconds = Seconds(0.8)
