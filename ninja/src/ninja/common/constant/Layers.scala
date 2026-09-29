package ninja.common.constant

import indigo.*

/** Drawing layers, back to front. */
object Layers:
  val world: LayerKey   = LayerKey("world")
  val weather: LayerKey = LayerKey("weather")
  val ui: LayerKey      = LayerKey("ui")
  val screen: LayerKey  = LayerKey("screen")
  val fps: LayerKey     = LayerKey("fps")

  /** The letterbox: black bars around the view, over everything. */
  val bars: LayerKey = LayerKey("bars")
