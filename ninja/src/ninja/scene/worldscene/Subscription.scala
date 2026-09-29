package ninja.scene.worldscene

import indigo.*
import ninja.common.util.Input
import ninja.scene.worldscene.Type.*

object Subscription:

  /** Every frame, with the direction the player is pushing and whether attack is held (Godot reads
    * input every frame too).
    */
  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    case FrameTick => Some(Msg.Tick(Input.moveVector(input), Input.isAttackHeld(input)))
    case _         => None
