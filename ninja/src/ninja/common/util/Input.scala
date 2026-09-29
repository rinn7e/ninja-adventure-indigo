package ninja.common.util

import indigo.*

object Input:

  private def held(keyboard: Keyboard, keys: Key*): Boolean =
    keys.exists(keyboard.keysAreDown(_))

  /** The direction the player is pushing (Godot's `Input.get_vector`): arrows or WASD, normalised
    * so diagonals aren't faster.
    */
  def moveVector(keyboard: Keyboard): Vector2 =
    val x = (if held(keyboard, Key.ARROW_RIGHT, Key.KEY_D) then 1 else 0) -
      (if held(keyboard, Key.ARROW_LEFT, Key.KEY_A) then 1 else 0)
    val y = (if held(keyboard, Key.ARROW_DOWN, Key.KEY_S) then 1 else 0) -
      (if held(keyboard, Key.ARROW_UP, Key.KEY_W) then 1 else 0)
    Vector2(x, y).normalise

  def isConfirm(event: GlobalEvent): Boolean =
    event match
      case KeyboardEvent.KeyUp(Key.SPACE | Key.ENTER | Key.KEY_Z) => true
      case _                                                      => false
