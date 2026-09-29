package ninja.common.types

import indigo.*

/** Environment zones (Godot's `EnvironmentArea` / `ResourceEnvironment`): entering one changes the
  * music and the weather.
  */
object Ambience:

  enum Meteo derives CanEqual:
    case Rain, Snow, Fog, Cloud, Leaf, Ray

  enum Music derives CanEqual:
    case Dream, Swamp

  final case class Environment(music: Option[Music], meteo: Set[Meteo]) derives CanEqual

  object Environment:
    val calm: Environment = Environment(None, Set.empty)

  final case class Zone(area: Rectangle, environment: Environment) derives CanEqual
