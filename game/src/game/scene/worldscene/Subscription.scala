package game.scene.worldscene

import indigo.*
import game.common.util.Input
import game.scene.worldscene.Type.*

object Subscription:

  /** Every frame, with the direction the player is pushing and whether attack is held (Godot reads
    * input every frame too).
    */
  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    case FrameTick =>
      Some(
        Msg.Tick(Input.moveVector(input), Input.isAttackHeld(input), Input.isTalkHeld(input))
      )
    case KeyboardEvent.KeyUp(Key.F3) => Some(Msg.ToggleDebug)
    case _                           => None
