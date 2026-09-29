package game.common.constant

import indigo.*

/** Drawing layers, back to front. */
object Layers:
  val world: LayerKey   = LayerKey("world")
  val weather: LayerKey = LayerKey("weather")
  val ui: LayerKey      = LayerKey("ui")
  val screen: LayerKey  = LayerKey("screen")

  /** Colour grading of everything below (Godot's `ColorCorrection`, on its top screen layer). */
  val grading: LayerKey = LayerKey("grading")
  val fps: LayerKey     = LayerKey("fps")

  /** The letterbox: black bars around the view, over everything. */
  val bars: LayerKey = LayerKey("bars")
