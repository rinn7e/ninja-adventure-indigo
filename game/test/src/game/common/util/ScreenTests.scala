package game.common.util

import indigo.*
import game.common.util.Screen.*

class ScreenTests extends munit.FunSuite {

  test("the Godot window (1280x720) shows the view at 4x, filling it exactly") {
    assertEquals(scaleFor(Size(1280, 720)), 4)
    assertEquals(offsetFor(Size(1280, 720)), Point.zero)
  }

  test("full HD scales to 6x, with no bars") {
    assertEquals(scaleFor(Size(1920, 1080)), 6)
    assertEquals(offsetFor(Size(1920, 1080)), Point.zero)
  }

  test("a window between whole scales keeps the smaller one, centred with bars") {
    assertEquals(scaleFor(Size(1366, 768)), 4)
    assertEquals(windowFor(Size(1366, 768)), Size(341, 192))
    assertEquals(offsetFor(Size(1366, 768)), Point(10, 6))
  }

  test("the narrower side decides the scale") {
    assertEquals(scaleFor(Size(800, 600)), 2)
    assertEquals(offsetFor(Size(800, 600)), Point(40, 60))
    assertEquals(scaleFor(Size(1000, 800)), 3)
    assertEquals(offsetFor(Size(1000, 800)), Point(6, 43))
  }

  test("a window smaller than the view stays at 1x, pinned to the top-left") {
    assertEquals(scaleFor(Size(200, 100)), 1)
    assertEquals(offsetFor(Size(200, 100)), Point.zero)
  }
}
