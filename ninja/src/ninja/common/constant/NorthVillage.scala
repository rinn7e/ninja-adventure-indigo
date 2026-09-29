package ninja.common.constant

import indigo.*
import ninja.common.Types.*
import ninja.common.util.TileMap
import ninja.generated.Assets

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

  /** The villagers (`Village.tscn`'s `Actors`, and the monk in `World.tscn`'s interior), with their
    * idle routines. Godot 3's talk area is a circle of radius 12.8 on the villager.
    */
  val villagers: Batch[Villager] =
    def standing(
        name: String,
        sheet: AssetName,
        face: AssetName,
        x: Double,
        y: Double,
        facing: Facing,
        lines: String*
    ): Villager =
      Villager(
        name,
        sheet,
        SheetKind.Standard,
        face,
        at(x, y),
        facing,
        None,
        Vector2.zero,
        Batch(lines*)
      )
    Batch(
      // Walks down and back up by the dojo (Godot: `Actors/Samurai`, routine 104).
      standing(
        "Makoto",
        Assets.assets.npcSamurai,
        Assets.assets.faceSamurai,
        912,
        464,
        Facing.Down,
        "Hello! Who are you ?",
        "A ninja from the south village? You came up through the old forest path!",
        "I train at the dojo. Bamboo monsters haunt the dungeon east of the lake."
      ).copy(routine =
        Some(
          Routine(
            5,
            Batch(
              0.0 -> at(912, 464),
              1.5 -> at(912, 496),
              2.5 -> at(912, 496),
              4.0 -> at(912, 464)
            ),
            Batch(0.0 -> Facing.Down, 2.5 -> Facing.Up),
            Batch(0.0 -> true, 1.5        -> false, 2.5 -> true, 4.0 -> false),
            Batch.empty
          )
        )
      ),
      // Looks down, right, then left (`Actors/Guard`, routine 126).
      standing(
        "Guard",
        Assets.assets.npcWarrior,
        Assets.assets.faceWarrior,
        464,
        432,
        Facing.Left,
        "Halt! ...Oh, you're one of us.",
        "Mind the forest to the west. It always rains there, and the bamboo don't like visitors."
      ).copy(routine =
        Some(
          Routine(
            5,
            Batch.empty,
            Batch(0.0 -> Facing.Down, 2.0 -> Facing.Right, 4.0 -> Facing.Left),
            Batch.empty,
            Batch.empty
          )
        )
      ),
      // Godot 3 puts her at (680, 600), inside a house's doorway, where the house hides her; she
      // stands just in front of it instead.
      standing(
        "Akane",
        Assets.assets.npcRedNinja,
        Assets.assets.faceRedNinja,
        680,
        634,
        Facing.Down,
        "The south village has a pig and a swamp. We have a dojo and a lake.",
        "I think we win."
      ),
      standing(
        "Grandma",
        Assets.assets.npcOldWoman,
        Assets.assets.faceOldWoman,
        713,
        798,
        Facing.Right,
        "Ah, young one. My old bones feel the rain from the forest.",
        "Break the plants if you must, they grow back by the time you return."
      ).copy(sheetKind = SheetKind.BreathingIdle),
      // Trots in a triangle (`Actors/Dog2`, routine 94).
      standing(
        "Dog",
        Assets.assets.npcDog,
        Assets.assets.faceDog,
        872,
        664,
        Facing.Left,
        "Woof! Woof!"
      )
        .copy(
          sheetKind = SheetKind.Strip,
          routine = Some(
            Routine(
              5,
              Batch(
                0.0 -> at(872, 664),
                1.0 -> at(888, 680),
                1.3 -> at(888, 680),
                2.7 -> at(904, 664),
                3.0 -> at(904, 664),
                4.8 -> at(872, 664)
              ),
              Batch.empty,
              Batch(
                0.0 -> true,
                1.0 -> false,
                1.3 -> true,
                2.7 -> false,
                3.0 -> true,
                4.8 -> false
              ),
              Batch(0.0 -> false, 3.0 -> true)
            )
          )
        ),
      // In the house interior; its talk area sits 3 pixels up (`YSort/Interior/Monk2`).
      standing(
        "Monk",
        Assets.assets.npcMonk,
        Assets.assets.faceMonk,
        342.228 + 823.772,
        -555.006 + 426.006,
        Facing.Down,
        "Sit, rest a while.",
        "This house has seen many adventurers. Few came back from the dungeon unscathed."
      ).copy(talkOffset = Vector2(0, -3))
    )

  /** `Village.tscn`'s `Destroyable` plants, in four 2x2 clumps. */
  val props: Batch[Prop] =
    Batch(
      (752, 808),
      (768, 808),
      (752, 824),
      (768, 824), // by the old woman
      (824, 824),
      (840, 824),
      (824, 808),
      (840, 808),
      (840, 632),
      (856, 632),
      (840, 648),
      (856, 648) // by the dog
    ).map { case (x, y) => Prop(PropKind.Plant, at(x, y), brokenAt = None) }

  /** `Village.tscn`'s smoke: a chimney's (its `modulate` replaces the scene's grey with a warm
    * white; over everything) and the rainy forest's campfire (grey, two puffs).
    */
  val smokes: Batch[SmokeEmitter] =
    Batch(
      SmokeEmitter(at(825, 541), 4, RGBA(0.9, 0.9, 0.8, 1), onTop = true),
      SmokeEmitter(at(440, 770), 2, RGBA(0.592, 0.592, 0.592, 1), onTop = false)
    )
