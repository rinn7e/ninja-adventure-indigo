package ninja.common.util

import indigo.*
import ninja.common.util.Input.*

class InputTests extends munit.FunSuite {

  private def withStick(x: Double, y: Double): InputState =
    val default = InputState.default
    InputState(
      default.mouse,
      default.keyboard,
      Gamepad(
        connected = true,
        GamepadAnalogControls(AnalogAxis(x, y, false), AnalogAxis.default, 4),
        GamepadDPad.default,
        GamepadButtons.default
      ),
      default.wheel,
      default.pen,
      default.touch,
      default.pointer
    )

  private def close(a: Vector2, b: Vector2): Boolean =
    (a - b).length < 1e-9

  test("no input, no movement") {
    assertEquals(moveVector(InputState.default), Vector2.zero)
  }

  test("a stick inside Godot's 0.5 deadzone doesn't move the player") {
    assertEquals(moveVector(withStick(0.4, 0.2)), Vector2.zero)
  }

  test("past the deadzone the stick is rescaled, so a light tilt walks slowly") {
    assert(close(moveVector(withStick(0.75, 0)), Vector2(0.5, 0)))
    assert(close(moveVector(withStick(0, -1)), Vector2(0, -1)))
  }

  test("a full diagonal is capped at 1, so diagonals aren't faster") {
    assert(close(moveVector(withStick(1, 1)), Vector2(1, 1).normalise))
  }
}
