package ninja.scene.worldscene

import indigo.*
import ninja.common.util.Input
import ninja.scene.worldscene.Type.*

object Subscription:

  /** Every frame, with the direction the player is pushing (Godot reads input every frame too). */
  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    case FrameTick => Some(Msg.Tick(Input.moveVector(input)))
    case _         => None
