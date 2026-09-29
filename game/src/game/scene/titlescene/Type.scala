package game.scene.titlescene

import indigo.*

object Type:

  final case class Model(enteredAt: Seconds) derives CanEqual

  enum Msg derives CanEqual:
    /** Intercepted by the parent, which starts the game. */
    case Start
