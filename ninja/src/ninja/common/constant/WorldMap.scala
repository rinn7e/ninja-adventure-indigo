package ninja.common.constant

import indigo.*
import ninja.common.Types.*

/** The whole world: the V4 village (`Village`) and the Godot 3 world north of it (`NorthVillage`).
  * Walls are indexed by area, so collision only checks nearby ones.
  */
object WorldMap:

  /** Both villages' teleporters; the Godot 3 ones' targets move past the V4 village's. */
  val teleporters: Batch[Teleporter] =
    Village.teleporters ++
      NorthVillage.teleporters.map(t => t.copy(target = t.target + Village.teleporters.length))

  /** Both villages' zones; the V4 village's come first (the first zone touched wins). */
  val zones: Batch[Zone] =
    Village.zones ++ NorthVillage.zones

  val monsterStarts: Batch[Vector2] =
    NorthVillage.monsterStarts

  val solids: Batch[Polygon.Closed] =
    Village.solids ++ NorthVillage.solids

  private val bucket: Int = 64

  private def bucketOf(x: Double, y: Double): (Int, Int) =
    (Math.floor(x / bucket).toInt, Math.floor(y / bucket).toInt)

  /** Each polygon, filed under every bucket its bounds touch. */
  private val index: Map[(Int, Int), Batch[Polygon.Closed]] =
    solids.toList
      .flatMap { polygon =>
        val b    = polygon.bounds
        val from = bucketOf(b.left, b.top)
        val to   = bucketOf(b.right, b.bottom)
        for
          bx <- (from._1 to to._1).toList
          by <- (from._2 to to._2).toList
        yield ((bx, by), polygon)
      }
      .groupBy(_._1)
      .map { case (key, entries) => key -> Batch.fromList(entries.map(_._2)) }

  /** The walls near a point: those in its bucket and the eight around it (enough for bodies smaller
    * than a bucket moving less than a bucket per frame).
    */
  def solidsNear(point: Vector2): Batch[Polygon.Closed] =
    val (bx, by) = bucketOf(point.x, point.y)
    Batch.fromList(
      (for
        dx <- (-1 to 1).toList
        dy <- (-1 to 1).toList
      yield index.getOrElse((bx + dx, by + dy), Batch.empty).toList).flatten.distinct
    )
