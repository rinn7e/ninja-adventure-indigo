package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.Layout.tileSize
import ninja.common.constant.{NorthVillage, NorthVillageTiles, Village, VillageTiles}
import ninja.common.util.TileMap.*

/** The village tiles, drawn with `CloneTiles` (hardware instancing): one clone blank per tileset
  * texture, each tile cropped from it. Drawing order follows the Godot TileMap: the floor layers at
  * the back, wall tiles y-sorted with the characters, and tiles with a positive z-index (roofs) on
  * top.
  */
object TilemapUI:

  /** Godot's wall layers sort 5 pixels above each tile's centre, plus the tile's own origin. */
  private val layerYSortOrigin: Int = -5

  private def cloneId(source: Int): CloneId = CloneId(s"tiles-$source")

  // --- The Godot 3 world (NorthVillage) -------------------------------------------------------

  /** Godot 3 tiles come in several sizes: one clone blank per texture and size. */
  private def northCloneId(source: Int, size: Size): CloneId =
    CloneId(s"north-$source-${size.width}x${size.height}")

  private val northSizes: Batch[(Int, Size)] =
    Batch.fromList(NorthVillage.tiles.map(t => (t.source, t.crop.size)).toList.distinct)

  /** A flipped tile is mirrored around its top-left corner, so it starts one size further on. */
  private def placedData(tile: PlacedTile): CloneTileData =
    CloneTileData(
      tile.position.x + (if tile.flipH then tile.crop.width else 0),
      tile.position.y + (if tile.flipV then tile.crop.height else 0),
      Radians.zero,
      if tile.flipH then -1.0 else 1.0,
      if tile.flipV then -1.0 else 1.0,
      tile.crop.x,
      tile.crop.y,
      tile.crop.width,
      tile.crop.height
    )

  /** Static batches, one per texture and size, in order of first appearance (so later Godot layers,
    * such as floor details, draw over earlier ones).
    */
  private def placedUI(tiles: Batch[PlacedTile], key: String): Batch[SceneNode] =
    val groups = tiles.toList.zipWithIndex.groupBy { case (t, _) => (t.source, t.crop.size) }
    Batch.fromList(groups.toList.sortBy(_._2.map(_._2).min).map { case ((source, size), group) =>
      CloneTiles(northCloneId(source, size), Batch.fromList(group.map(g => placedData(g._1))))
        .withStaticBatchKey(BindingKey(s"$key-$source-${size.width}x${size.height}")): SceneNode
    })

  /** The Godot 3 world's floor, water and wall layers, under the characters. */
  val northFloorUI: Batch[SceneNode] =
    placedUI(NorthVillage.tiles.filter(_.layer == 0), "north-floor")

  /** Its y-sorted tiles (trees, houses, fences), as (sort y, node): one static batch per row. */
  val northRowsUI: Batch[(Double, SceneNode)] =
    Batch.fromList(
      NorthVillage.tiles
        .filter(_.layer == 1)
        .groupBy(_.sortY)
        .toList
        .flatMap { case (sortY, row) =>
          placedUI(row, s"north-row-$sortY").toList.map(node => (sortY.toDouble, node))
        }
    )

  /** Its tiles drawn over everything. */
  val northTopUI: Batch[SceneNode] =
    placedUI(NorthVillage.tiles.filter(_.layer == 2), "north-top")

  /** Must be added to the scene (`addCloneBlanks`) for the tiles to draw. */
  val tilesCloneBlanksUI: Batch[CloneBlank] =
    Batch.fromList(VillageTiles.sources.toList.map { case (source, asset) =>
      CloneBlank(cloneId(source), Graphic(tileSize, tileSize, Material.Bitmap(asset)))
    }) ++ northSizes.map { case (source, size) =>
      CloneBlank(
        northCloneId(source, size),
        Graphic(size.width, size.height, Material.Bitmap(NorthVillageTiles.sources(source)))
      )
    }

  private def kind(cell: TileCell): TileKind =
    kindOf(cell)(Village.tiles)

  private def tileData(cell: TileCell, frame: Int): CloneTileData =
    val topLeft = centreOf(cell.cell, VillageTiles.origin) - Vector2(tileSize / 2, tileSize / 2)
    CloneTileData(
      topLeft.x.toInt,
      topLeft.y.toInt,
      (cell.atlas.x + frame) * tileSize,
      cell.atlas.y * tileSize,
      tileSize,
      tileSize
    )

  /** One `CloneTiles` per tileset texture. Static batches are cached by the renderer. */
  private def batchesUI(
      cells: Batch[TileCell],
      frame: Int,
      staticKey: Option[String]
  ): Batch[CloneTiles] =
    Batch.fromList(cells.groupBy(_.source).toList.sortBy(_._1).map { case (source, group) =>
      val tiles = CloneTiles(cloneId(source), group.map(tileData(_, frame)))
      staticKey.fold(tiles)(key => tiles.withStaticBatchKey(BindingKey(s"$key-$source")))
    })

  private val animated: TileCell => Boolean = kind(_).frames > 1

  private val walls: Batch[TileCell] = Village.tiles.wall ++ Village.tiles.wall2

  private val background: Batch[TileCell] =
    Village.tiles.floor ++ Village.tiles.floorDetail ++ walls.filter(kind(_).zIndex < 0)

  /** The floor layers (and any below-floor wall tiles), static but for animated water. */
  def floorUI(now: Seconds): Batch[SceneNode] =
    val water = background.filter(animated)
    val frame = now.toDouble.toInt
    batchesUI(background.filterNot(animated), 0, Some("floor")) ++
      Batch.fromList(water.groupBy(kind(_).frames).toList.flatMap { case (frames, cells) =>
        batchesUI(cells, frame % frames, None).toList
      })

  /** Wall tiles to sort with the characters, as (sort y, node): one static batch per row. */
  val wallRowsUI: Batch[(Double, SceneNode)] =
    val sortable = walls.filter(kind(_).zIndex == 0)
    Batch.fromList(
      sortable
        .groupBy(c =>
          centreOf(c.cell, VillageTiles.origin).y + layerYSortOrigin + kind(c).ySortOrigin
        )
        .toList
        .flatMap { case (sortY, row) =>
          batchesUI(row, 0, Some(s"wall-row-$sortY")).toList.map(node => (sortY, node: SceneNode))
        }
    )

  /** Tiles drawn above everything else (roofs, tree tops). */
  val topUI: Batch[SceneNode] =
    batchesUI(walls.filter(kind(_).zIndex > 0), 0, Some("top"))
