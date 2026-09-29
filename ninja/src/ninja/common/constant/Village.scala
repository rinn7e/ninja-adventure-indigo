package ninja.common.constant

import indigo.*
import ninja.common.Types.*
import ninja.common.util.TileMap

/** The village map's characters, teleporters and environment zones, transcribed from the Godot
  * project's `content/map/map_village.tscn` (world coordinates; characters there are children of
  * the TileMap node, so their positions include `VillageTiles.origin`). The tiles and props are
  * generated into `VillageTiles`.
  */
object Village:

  private val origin: Vector2 = Vector2(VillageTiles.origin.x, VillageTiles.origin.y)

  // --- Tiles (decoded once from the generated data) -------------------------

  val tiles: TileLayers =
    TileLayers(
      wall = TileMap.decodeCells(VillageTiles.wall),
      wall2 = TileMap.decodeCells(VillageTiles.wall2),
      floorDetail = TileMap.decodeCells(VillageTiles.floorDetail),
      floor = TileMap.decodeCells(VillageTiles.floor),
      kinds = TileMap.decodeKinds(VillageTiles.tiles)
    )

  /** Every wall's collision polygon, in world coordinates. */
  val solids: Batch[Polygon.Closed] =
    TileMap.solids(VillageTiles.origin)(tiles)

  /** The destroyable props, whole. */
  val props: Batch[Prop] =
    TileMap.decodeProps(VillageTiles.props).map { case (cell, scene) =>
      val kind =
        scene match
          case 1 => PropKind.Crate
          case 3 => PropKind.Pot
          case _ => PropKind.Grass
      Prop(kind, TileMap.centreOf(cell, VillageTiles.origin), brokenAt = None)
    }

  // --- Characters -----------------------------------------------------------

  /** The player (Godot 4: NinjaBlue; drawn as the Godot 3 version's green ninja): speed 100. */
  val playerStart: Vector2 = origin + Vector2(56, 53)
  val playerSpeed: Double  = 100

  /** SamuraiGreen follows the player (speed 90, 8 to 24 pixels away). */
  val followerStart: Vector2 = origin + Vector2(-8, 5)
  val followerSpeed: Double  = 90

  /** The pig follows SamuraiGreen; its follow behaviour sits 8 pixels left of and 5 below it. */
  val pigStart: Vector2        = origin
  val pigFollowOffset: Vector2 = Vector2(-8, 5)
  val pigSpeed: Double         = 100

  /** SamuraiBlue walks back and forth along a two-point path (speed 50), waiting 3 seconds at each
    * end. Like the Godot script, the path points are relative to the TileMap, not the world.
    */
  val patrollerStart: Vector2 = origin + Vector2(-88, -123)
  val patrollerSpeed: Double  = 50
  val patrolPath: Batch[Vector2] =
    Batch(Vector2(-100 + 80, -131 - 16), Vector2(-100 + 208, -131 - 16))
  val patrolWait: Seconds     = Seconds(3)
  val patrolPrecision: Double = 5

  /** The house interior sits north of the village in the Godot map, where the Godot 3 world now is
    * (`NorthVillage`), so the importer moves it two screens east; its teleporter and zone move too.
    */
  private val interiorShift: Int = 640

  /** Two linked teleporters. The first's collision shape is 7 pixels below it. */
  val teleporters: Batch[Teleporter] =
    Batch(
      Teleporter(
        Vector2(16, -176),
        Rectangle(16 - 10, -176 + 7 - 5, 20, 10),
        Vector2(0, 1),
        1,
        25,
        keepsOffset = true
      ),
      Teleporter(
        Vector2(640 + interiorShift, -624),
        Rectangle(640 + interiorShift - 10, -624 - 5, 20, 10),
        Vector2(0, -1),
        0,
        25,
        keepsOffset = true
      )
    )

  private def centred(x: Int, y: Int, width: Int, height: Int): Rectangle =
    Rectangle(x - width / 2, y - height / 2, width, height)

  /** Environment zones: music, weather and grading change when the player walks into one. */
  val zones: Batch[Zone] =
    Batch(
      // swamp
      Zone(
        centred(-296, -64, 304, 336),
        Environment(Some(Music.Swamp), Set(Meteo.Rain, Meteo.Fog, Meteo.Leaf), Grading.Swamp)
      ),
      // autumn
      Zone(
        centred(24, 368, 304, 160),
        Environment(None, Set(Meteo.Cloud, Meteo.Leaf), Grading.Neutral)
      ),
      Zone(
        centred(24, 17, 304, 160),
        Environment(Some(Music.Dream), Set(Meteo.Ray, Meteo.Fog), Grading.Neutral)
      ),
      Zone(
        centred(24, -152, 304, 160),
        Environment(Some(Music.Dream), Set(Meteo.Ray, Meteo.Cloud), Grading.Neutral)
      ),
      Zone(
        centred(662 + interiorShift, -685, 304, 160),
        Environment(None, Set(Meteo.Ray), Grading.Neutral)
      )
    )
