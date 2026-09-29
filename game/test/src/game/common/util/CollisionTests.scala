package game.common.util

import indigo.*
import game.common.constant.Village
import game.common.util.Collision.*

class CollisionTests extends munit.FunSuite {

  val square: Polygon.Closed =
    Polygon.Closed(Batch(Vertex(0, 0), Vertex(16, 0), Vertex(16, 16), Vertex(0, 16)))

  def circle(x: Double, y: Double): BoundingCircle = BoundingCircle(Vertex(x, y), 7)

  test("the closest point on a segment is clamped to its ends") {
    assertEquals(closestOnSegment(Vector2(0, 0), Vector2(10, 0))(Vector2(5, 3)), Vector2(5, 0))
    assertEquals(closestOnSegment(Vector2(0, 0), Vector2(10, 0))(Vector2(-4, 3)), Vector2(0, 0))
  }

  test("a circle clear of a polygon isn't pushed") {
    assertEquals(penetration(square)(circle(30, 8)), None)
  }

  test("a circle overlapping an edge is pushed straight out of it") {
    val Some((normal, depth)) = penetration(square)(circle(20, 8)): @unchecked
    assertEquals(normal, Vector2(1, 0))
    assertEqualsDouble(depth, 3, 1e-9)
  }

  test("walking into a wall stops; walking along it slides") {
    val (stopped, v1) = moveAndSlide(Vector2(100, 0), 0.1, 7, Vector2.zero, Batch(square), Batch.empty)(Vector2(-12, 8))
    assertEqualsDouble(stopped.x, -7, 1e-9)
    assertEqualsDouble(v1.x, 0, 1e-9)

    val (slid, v2) = moveAndSlide(Vector2(50, 50), 0.1, 7, Vector2.zero, Batch(square), Batch.empty)(Vector2(-8, 8))
    assertEqualsDouble(slid.x, -7, 1e-9)
    assert(slid.y > 8, "keeps moving along the wall")
    assertEqualsDouble(v2.y, 50, 1e-9)
  }

  test("round obstacles push the body out too") {
    val (at, _) =
      moveAndSlide(Vector2(100, 0), 0.1, 7, Vector2.zero, Batch.empty, Batch(BoundingCircle(Vertex(10, 0), 8)))(Vector2(-10, 0))
    assertEqualsDouble(at.x, -5, 1e-9)
  }

  test("the open path by the ruined wall isn't blocked (diagonal polygon, not its bounding box)") {
    // The spot where a box approximation used to stop the player: walking west at y = 13.
    val (at, _) =
      moveAndSlide(Vector2(-100, 0), 0.1, 7, Vector2(0, -1), Village.solids, Batch.empty)(Vector2(-19, 13))
    assert(at.x < -25, s"moved west past the diagonal wall, got $at")
  }
}
