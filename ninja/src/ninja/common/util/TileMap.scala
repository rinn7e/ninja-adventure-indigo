package ninja.common.util

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.Layout.tileSize

/** Decodes the generated village data and answers questions about it. */
object TileMap:

  private def rows(data: String): Batch[Batch[Int]] =
    if data.isEmpty then Batch.empty
    else Batch.fromArray(data.split(";")).map(row => Batch.fromArray(row.split(",")).map(_.toInt))

  def decodeCells(data: String): Batch[TileCell] =
    rows(data).map(r => TileCell(Point(r(0), r(1)), r(2), Point(r(3), r(4))))

  /** Props placed as scene tiles: their cell and Godot scene id. */
  def decodeProps(data: String): Batch[(Point, Int)] =
    rows(data).map(r => (Point(r(0), r(1)), r(2)))

  /** Tile kinds: source, atlasX, atlasY, ySortOrigin, zIndex, frames, then polygon points. */
  def decodeKinds(data: String): Map[(Int, Int, Int), TileKind] =
    Batch
      .fromArray(data.split(";"))
      .map { row =>
        val f = Batch.fromArray(row.split(","))
        val points = Batch.fromList(
          f.drop(6).map(_.toDouble).toList.grouped(2).map(p => Vector2(p(0), p(1))).toList
        )
        val solid = if points.isEmpty then None else Some(points)
        (f(0).toInt, f(1).toInt, f(2).toInt) -> TileKind(solid, f(3).toInt, f(4).toInt, f(5).toInt)
      }
      .toList
      .toMap

  def kindOf(cell: TileCell)(layers: TileLayers): TileKind =
    layers.kinds.getOrElse((cell.source, cell.atlas.x, cell.atlas.y), TileKind.plain)

  /** World position of a cell's centre. */
  def centreOf(cell: Point, origin: Point): Vector2 =
    Vector2(
      origin.x + cell.x * tileSize + tileSize / 2,
      origin.y + cell.y * tileSize + tileSize / 2
    )

  /** Every tile's collision polygon in world coordinates, from all layers. */
  def solids(origin: Point)(layers: TileLayers): Batch[Polygon.Closed] =
    (layers.floor ++ layers.floorDetail ++ layers.wall ++ layers.wall2).flatMap { cell =>
      kindOf(cell)(layers).solid match
        case Some(points) =>
          val centre = centreOf(cell.cell, origin)
          Batch(Polygon.Closed(points.map(p => Vertex.fromVector2(p + centre))))
        case None =>
          Batch.empty
    }
