package ninja.common.types

import indigo.*

/** Environment zones (Godot's `EnvironmentArea` / `ResourceEnvironment`): entering one changes the
  * music, the weather and the screen's colour grading.
  */
object Ambience:

  enum Meteo derives CanEqual:
    case Rain, Snow, Fog, Cloud, Leaf, Ray

  enum Music derives CanEqual:
    case Dream, Swamp

  /** The colour grading gradient (Godot's `ColorCorrection`, see `Gradings`): `Initial` is the one
    * set on Godot's world scene, shown until the first zone's grading fades in.
    */
  enum Grading derives CanEqual:
    case Initial, Neutral, Swamp

  final case class Environment(music: Option[Music], meteo: Set[Meteo], grading: Grading)
      derives CanEqual

  object Environment:
    /** Before the first zone. */
    val calm: Environment = Environment(None, Set.empty, Grading.Initial)

  final case class Zone(area: Rectangle, environment: Environment) derives CanEqual
