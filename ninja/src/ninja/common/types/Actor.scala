package ninja.common.types

import indigo.*

/** A character moving freely (Godot's `Character`): world position of its feet, velocity in pixels
  * per second, facing, and whether it is walking. `animSince` is when it last started or stopped
  * walking, which restarts its animation.
  */
final case class Actor(
    position: Vector2,
    velocity: Vector2,
    facing: Facing,
    moving: Boolean,
    animSince: Seconds
) derives CanEqual

object Actor:

  def at(position: Vector2): Actor =
    Actor(position, Vector2.zero, Facing.Down, moving = false, animSince = Seconds.zero)
