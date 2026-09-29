package ninja.scene.worldscene.subui

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.Layout.tileSize
import ninja.common.constant.{Village, VillageTiles}
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

  /** Must be added to the scene (`addCloneBlanks`) for the tiles to draw. */
  val tilesCloneBlanksUI: Batch[CloneBlank] =
    Batch.fromList(VillageTiles.sources.toList.map { case (source, asset) =>
      CloneBlank(cloneId(source), Graphic(tileSize, tileSize, Material.Bitmap(asset)))
    })

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
