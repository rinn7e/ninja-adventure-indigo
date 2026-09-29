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

  // --- Objects, transcribed from the Godot 3 scenes (in Godot 3 coordinates, moved by `offset`) ---

  private def at(x: Double, y: Double): Vector2 = Vector2(x, y) + offset

  /** A Godot 3 area: centre and half-size (`RectangleShape2D.extents`). */
  private def area(x: Double, y: Double, halfWidth: Int, halfHeight: Int): Rectangle =
    Rectangle(
      (x + offset.x).toInt - halfWidth,
      (y + offset.y).toInt - halfHeight,
      halfWidth * 2,
      halfHeight * 2
    )

  /** `World.tscn`'s teleporters (a 12x2 area each): the village house door and the interior, and a
    * village door and the dungeon. Godot 3 lands the player 10 pixels along the teleporter it
    * *entered*'s `teleport_direction`, so each one here holds its partner's direction.
    */
  val teleporters: Batch[Teleporter] =
    def door(x: Double, y: Double, landing: Vector2, target: Int): Teleporter =
      Teleporter(at(x, y), area(x, y, 6, 1), landing, target, 10, keepsOffset = false)
    Batch(
      door(729, 422, Vector2(0, 1), 1),   // house door (Godot: Teleporter)
      door(1118, -67, Vector2(0, -1), 0), // interior (Teleporter2)
      door(452, -407, Vector2(0, -1), 3), // dungeon (Teleporter3)
      door(1080, 664, Vector2(0, -1), 2)  // dungeon entrance (Teleporter4)
    )

  /** Godot 3 has music areas (`MusicArea`, entered, never left) and weather emitters placed in the
    * world. Here each music area is a zone, with the weather of the emitters inside it; the
    * dungeon, which has neither, gets a quiet zone of its own.
    */
  val zones: Batch[Zone] =
    def zone(area: Rectangle, music: Option[Music], meteo: Set[Meteo]): Zone =
      Zone(area, Environment(music, meteo, Grading.Neutral))
    Batch(
      // Village centre: Road, falling leaves (720, 408) and clouds (760, 592).
      zone(area(800, 530, 152, 168), Some(Music.Road), Set(Meteo.Leaf, Meteo.Cloud)),
      // South-west forest: Aquatic, rain (480, 768), clouds (472, 784) and leaves (616, 806).
      zone(area(480, 800, 152, 88), Some(Music.Aquatic), Set(Meteo.Rain, Meteo.Cloud, Meteo.Leaf)),
      // East, by the dungeon entrance: Mystical, snow (1112, 616) and sparks (1088, 641).
      zone(area(1120, 608, 152, 88), Some(Music.Mystical), Set(Meteo.Snow, Meteo.Spark)),
      // South: no music (Godot 3 fades it out), the falling leaves' wide band (616, 806).
      zone(area(801, 800, 152, 88), None, Set(Meteo.Leaf)),
      // The house interior: Melancholia.
      zone(area(1143, -118, 152, 88), Some(Music.Melancholia), Set.empty),
      // The dungeon: Mystical carries on, no weather.
      zone(area(612, -552, 368, 216), Some(Music.Mystical), Set.empty)
    )

  /** Bamboo monsters: three in the dungeon (`World.tscn`), three in the rainy forest (`Village`).
    */
  val monsterStarts: Batch[Vector2] =
    Batch(at(496, -464), at(464, -496), at(432, -464), at(472, 752), at(504, 784), at(472, 808))
