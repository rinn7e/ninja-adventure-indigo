package game.scene.titlescene

import indigo.*
import game.common.util.Input
import game.scene.titlescene.Type.*

object Subscription:

  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    case e if Input.isConfirm(e)                 => Some(Msg.Start)
    case FrameTick if Input.isConfirmHeld(input) => Some(Msg.Start)
    case _                                       => None
