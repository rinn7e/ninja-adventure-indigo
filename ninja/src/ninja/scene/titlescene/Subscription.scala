package ninja.scene.titlescene

import indigo.*
import ninja.common.util.Input
import ninja.scene.titlescene.Type.*

object Subscription:

  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    case e if Input.isConfirm(e)                 => Some(Msg.Start)
    case FrameTick if Input.isConfirmHeld(input) => Some(Msg.Start)
    case _                                       => None
