package game.common.types

import indigo.*

/** The tile map, as decoded from the Godot map (`common/constant/VillageTiles.scala`). */
object Tiles:

  /** One tile: its cell, and where it comes from (Godot tileset source and atlas coordinates). */
  final case class TileCell(cell: Point, source: Int, atlas: Point) derives CanEqual

  /** Per-tile data from the Godot tileset. `solid` is the collision polygon, relative to the tile's
    * centre; `ySortOrigin` and `zIndex` are Godot's drawing order settings; `frames` > 1 means an
    * animated tile (frames laid out to the right, one per second).
    */
  final case class TileKind(
      solid: Option[Batch[Vector2]],
      ySortOrigin: Int,
      zIndex: Int,
      frames: Int
  ) derives CanEqual

  object TileKind:
    val plain: TileKind = TileKind(None, 0, 0, 1)

  /** A Godot 3 tile, already placed (Godot 3 tiles can be bigger than a cell, and flipped): `crop`
    * in its texture, `position` of its top-left in the world, and its drawing `layer` (0 under the
    * characters, 1 y-sorted with them by `sortY`, 2 over them).
    */
  final case class PlacedTile(
      source: Int,
      crop: Rectangle,
      position: Point,
      flipH: Boolean,
      flipV: Boolean,
      layer: Int,
      sortY: Int
  ) derives CanEqual

  /** The four Godot layers: walls (y-sorted with characters), floor detail and floor. */
  final case class TileLayers(
      wall: Batch[TileCell],
      wall2: Batch[TileCell],
      floorDetail: Batch[TileCell],
      floor: Batch[TileCell],
      kinds: Map[(Int, Int, Int), TileKind]
  )
