package ninja.common.constant

import indigo.*
import ninja.common.Types.*
import ninja.common.util.TileMap

/** The Godot 3 version's world (MIT, © 2020 Emilio Coppola): its village, house interior and
  * dungeon, placed north of the V4 village and joined to it through a gap in its southern tree line
  * (see `ninja/tools/import_godot.py`). The tiles are generated into `NorthVillageTiles`.
  */
object NorthVillage:

  /** Where the Godot 3 world's (0, 0) is in ours. */
  val offset: Vector2 = Vector2(-456, -1128)

  val tiles: Batch[PlacedTile] =
    TileMap.decodePlacedTiles(NorthVillageTiles.tiles)

  /** Its tiles' collision polygons, and walls along every edge of the map that opens onto nothing
    * (both villages').
    */
  val solids: Batch[Polygon.Closed] =
    TileMap.decodePolygons(NorthVillageTiles.solids)
