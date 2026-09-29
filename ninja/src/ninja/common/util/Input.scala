package ninja.common.util

import indigo.*

object Input:

  /** Godot's input map: an action's deadzone (`project.godot`, `move_*`). */
  val deadzone: Double = 0.5

  private def held(keyboard: Keyboard, keys: Key*): Double =
    if keys.exists(keyboard.keysAreDown(_)) then 1 else 0

  private def pressed(button: Boolean): Double =
    if button then 1 else 0

  /** The direction the player is pushing, as Godot's `Input.get_vector` computes it from the
    * `move_*` actions (arrows, the D-pad, the left stick; WASD added here). Each direction is its
    * strongest input; the result is zero inside the deadzone, then rescaled so it reaches 1 at the
    * edge (a light tilt walks slowly, and diagonals aren't faster).
    */
  def moveVector(input: InputState): Vector2 =
    val keyboard = input.keyboard
    val pad      = input.gamepad
    val stick    = pad.analog.left

    val right = held(keyboard, Key.ARROW_RIGHT, Key.KEY_D).max(pressed(pad.dpad.right)).max(stick.x)
    val left  = held(keyboard, Key.ARROW_LEFT, Key.KEY_A).max(pressed(pad.dpad.left)).max(-stick.x)
    val down  = held(keyboard, Key.ARROW_DOWN, Key.KEY_S).max(pressed(pad.dpad.down)).max(stick.y)
    val up    = held(keyboard, Key.ARROW_UP, Key.KEY_W).max(pressed(pad.dpad.up)).max(-stick.y)

    val raw    = Vector2(right - left, down - up)
    val length = raw.length
    if length <= deadzone then Vector2.zero
    else raw * (((length - deadzone) / (1 - deadzone)).min(1) / length)

  /** Godot 3's `action`: Space, or the gamepad's Cross (A). */
  def isAttackHeld(input: InputState): Boolean =
    input.keyboard.keysAreDown(Key.SPACE) || input.gamepad.buttons.Cross

  def isConfirm(event: GlobalEvent): Boolean =
    event match
      case KeyboardEvent.KeyUp(Key.SPACE | Key.ENTER | Key.KEY_Z) => true
      case _                                                      => false

  /** The gamepad's confirm buttons, held (Cross / Options, i.e. A / Start on an Xbox pad). */
  def isConfirmHeld(input: InputState): Boolean =
    input.gamepad.buttons.Cross || input.gamepad.buttons.Options
