package ninja.common.util

import indigo.*
import ninja.common.Types.*

/** Movement helpers, curried with the thing they work on last. */
object Motion:

  /** Godot's `Vector2.move_toward`: steps towards `target` by at most `maxDelta`. */
  def moveToward(target: Vector2, maxDelta: Double)(from: Vector2): Vector2 =
    val gap      = target - from
    val distance = gap.length
    if distance <= maxDelta || distance == 0 then target
    else from + gap / distance * maxDelta

  /** Which way a direction faces: the nearest of the four (Godot's `direction_to_frame`). */
  def facingOf(direction: Vector2): Facing =
    val quarter = Math.round(Math.toDegrees(Math.atan2(direction.y, direction.x)) / 90).toInt
    Math.floorMod(quarter, 4) match
      case 0 => Facing.Right
      case 1 => Facing.Down
      case 2 => Facing.Left
      case _ => Facing.Up

  /** Unit vector from `from` towards `to` (zero if they coincide). */
  def directionTo(to: Vector2)(from: Vector2): Vector2 =
    (to - from).normalise
