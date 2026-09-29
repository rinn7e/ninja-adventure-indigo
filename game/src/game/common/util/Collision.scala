package game.common.util

import indigo.*

/** Collision for characters, standing in for Godot's `CharacterBody2D.move_and_slide`: a body is a
  * circle that moves, is pushed out of any wall polygon or round obstacle it overlaps, and keeps
  * only the part of its velocity that slides along what it hit. Built on Indigo's geometry types
  * (`BoundingCircle`, `Polygon`, `BoundingBox`).
  */
object Collision:

  /** Closest point to `point` on the segment from `a` to `b`. */
  def closestOnSegment(a: Vector2, b: Vector2)(point: Vector2): Vector2 =
    val ab     = b - a
    val length = ab.dot(ab)
    if length == 0 then a
    else a + ab * ((point - a).dot(ab) / length).max(0).min(1)

  private def edges(polygon: Polygon.Closed): Batch[(Vector2, Vector2)] =
    val points = polygon.vertices.map(_.toVector2)
    points.zip(points.drop(1) :+ points(0))

  /** How to get a circle out of a polygon, if they overlap: the direction to push (the surface
    * normal) and how far.
    */
  def penetration(polygon: Polygon.Closed)(circle: BoundingCircle): Option[(Vector2, Double)] =
    if !circle.overlaps(polygon.bounds) then None
    else
      val centre = circle.position.toVector2
      val closest = edges(polygon)
        .map { case (a, b) => closestOnSegment(a, b)(centre) }
        .minBy(_.distanceTo(centre))
      val gap    = centre - closest
      val inside = polygon.contains(circle.position)
      if inside then Some((gap.normalise.invert, circle.radius + gap.length))
      else if gap.length < circle.radius && gap.length > 0 then
        Some((gap.normalise, circle.radius - gap.length))
      else None

  /** How to get a circle out of another circle (a round obstacle), if they overlap. */
  def penetrationOfCircle(obstacle: BoundingCircle)(
      circle: BoundingCircle
  ): Option[(Vector2, Double)] =
    val gap   = circle.position.toVector2 - obstacle.position.toVector2
    val reach = circle.radius + obstacle.radius
    if gap.length >= reach || gap.length == 0 then None
    else Some((gap.normalise, reach - gap.length))

  /** Moves a circular body by `velocity * dt`, then pushes it out of the walls and obstacles it
    * overlaps (a few passes, for corners), and removes the part of its velocity that goes into
    * them. `offset` is the circle's centre relative to the body's position (its feet).
    */
  def moveAndSlide(
      velocity: Vector2,
      dt: Double,
      radius: Double,
      offset: Vector2,
      walls: Batch[Polygon.Closed],
      obstacles: Batch[BoundingCircle]
  )(position: Vector2): (Vector2, Vector2) =
    def pass(state: (Vector2, Vector2)): (Vector2, Vector2) =
      val (at, v) = state
      val circle  = BoundingCircle(Vertex.fromVector2(at + offset), radius)
      val hits = walls.flatMap(w => Batch.fromOption(penetration(w)(circle))) ++
        obstacles.flatMap(o => Batch.fromOption(penetrationOfCircle(o)(circle)))
      hits.headOption match
        case None => state
        case Some((normal, depth)) =>
          val into = v.dot(normal)
          (at + normal * depth, if into < 0 then v - normal * into else v)

    (1 to 4).foldLeft((position + velocity * dt, velocity))((state, _) => pass(state))

  /** Whether a circular body touches an area (teleporters, environment zones). */
  def touches(area: Rectangle)(circle: BoundingCircle): Boolean =
    circle.overlaps(BoundingBox.fromRectangle(area))
