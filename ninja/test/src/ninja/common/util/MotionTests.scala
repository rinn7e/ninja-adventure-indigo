package ninja.common.util

import indigo.*
import ninja.common.Types.*
import ninja.common.util.Motion.*

class MotionTests extends munit.FunSuite {

  test("moveToward steps by at most maxDelta, and stops on the target") {
    assertEquals(moveToward(Vector2(10, 0), 3)(Vector2.zero), Vector2(3, 0))
    assertEquals(moveToward(Vector2(10, 0), 30)(Vector2.zero), Vector2(10, 0))
  }

  test("facing is the nearest of the four directions") {
    assertEquals(facingOf(Vector2(1, 0)), Facing.Right)
    assertEquals(facingOf(Vector2(0, 1)), Facing.Down)
    assertEquals(facingOf(Vector2(-1, 0)), Facing.Left)
    assertEquals(facingOf(Vector2(0, -1)), Facing.Up)
    assertEquals(facingOf(Vector2(1, 0.9).normalise), Facing.Right)
  }

  test("the move vector is normalised, so diagonals aren't faster") {
    assertEquals(Input.moveVector(Keyboard.default), Vector2.zero)
  }
}
