package ninja.scene.titlescene

import indigo.*
import ninja.common.Types.Shared
import ninja.scene.titlescene.Type.*

object Update:

  // -----------------------------------------------------------------
  // Init
  // -----------------------------------------------------------------

  /** Entering the title: fade in (`enteredAt`). */
  def init(shared: Shared): Outcome[Model] =
    Outcome(Model(enteredAt = shared.now))

  // -----------------------------------------------------------------
  // Update
  // -----------------------------------------------------------------

  def update(shared: Shared, msg: Msg, model: Model): Outcome[Model] =
    msg match
      case Msg.Start =>
        Outcome(model)
