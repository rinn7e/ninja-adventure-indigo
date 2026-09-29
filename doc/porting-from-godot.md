# Porting from Godot

How each part of the Ninja Adventure Godot 4 demo
([pixel-boy/NinjaAdventure](https://github.com/pixel-boy/NinjaAdventure)) maps to this Indigo
port. Godot paths are relative to the Godot project; Scala paths to `game/src/game/`.

## Mapping

| Godot | Indigo port |
| --- | --- |
| `project.godot`: viewport 1280x720, `stretch/mode = "viewport"`, camera zoom 4 | `Layout.screen` (320x180), whole-number letterboxing in `common/util/Screen.scala` and `Main.mainUI` |
| `project.godot` input map (`move_*`: arrows, D-pad, left stick, deadzone 0.5), `Input.get_vector` | `common/util/Input.moveVector` (WASD added) |
| `main.tscn` (`starting_menu = GAME`) | the root `Update` / `SceneRoute`; our title scene is an addition (browsers need a key press before playing music) |
| `system/character/character.gd` (speed, acceleration, `move_and_slide`) | `Actor`, `WorldScene.walk`, `Collision.moveAndSlide` against the tiles' exact polygons |
| `BehaviorFollow` (keep 8 to 24 px from the target) | `WorldScene.followVector`, `moveFollower`, `movePig` (the pig is an `Animal`: its velocity is set each frame) |
| `BehaviorFollowPath` (`Path2D`, waits 3s at each end) | `Patroller`, `WorldScene.patrol`, `Village.patrolPath` |
| `system/camera/camera_grid.gd` (`CameraGrid`: grid 320x176, offset (24, 16), 0.8s sine tween) | `CameraGrid`, `followCamera`, `worldscene/common/Util.{cellOf, cameraPosition, viewTopLeft}` |
| `system/destroyable/*` (grass, pot, crate, broken by the player's body) | `Prop`, `breakProps`, the burst in `ActorsUI` |
| `system/teleporter/*` (fade, then land `direction * 25` past the target) | `Teleporter`, `WorldScene.teleport`, `teleportUI` |
| `system/environment/environment_area.gd`, `resource_environment.gd` (music, meteo list, colour gradient) | `Zone`, `Environment`, `enterZones`, `Village.zones` |
| `world.tscn` particles on the camera (`Rain` + `RainOnFloor`, `Cloud`, `Leaf`, `Raylight`) and `Fog` | `WeatherUI`: particles as pure functions of time; ground effects (z 0: rain, splashes, cloud shadows) under walls and characters, sky effects (leaves z 3, rays z 6, fog z 10) above |
| `system/color_correction/*` (screen shader mapping each channel through a gradient, 0.5s cross-fade) | `ColorGradingUI`: a blend shader on the `grading` layer, `Gradings` (stops copied from the `.tres` files) |
| `system/music.gd` (fade out the old track, then play the new one) | `musicUI` in `scene/worldscene/UI.scala`, with `SceneAudio` |
| `system/ui/player_ui.tscn` (receptacle bar, 4 life per heart) | `HeartBarUI` |
| `TileMap` in `content/map/map_village.tscn`, `content/map/tileset.tres` | `tools/import_godot.py` → `common/constant/VillageTiles.scala` → `common/util/TileMap`, drawn by `TilemapUI` with `CloneTiles` |

Not ported, because the demo doesn't use them: `Snow` (no zone enables it), the V4 weapon system
(its `use_weapon()` only prints) and character hitboxes (nothing is connected to them). Combat
comes from the Godot 3 version instead (below).

## The Godot 3 world, joined north

The Godot 3 demo's world is imported whole and placed north of the Godot 4 village
(`game/tools/godot3.py` reads Godot 3 scenes; `import_godot.py` writes
`common/constant/NorthVillageTiles.scala`; the rest is `common/constant/NorthVillage.scala`).

| Godot 3 | Indigo port |
| --- | --- |
| `World/Maps/Village.tscn`, `Interior.tscn`, `World.tscn`'s dungeon: TileMaps with TileSets whose tiles can be bigger than a cell (trees are 64x48), flipped, placed by `cell_tile_origin` (top-left, centre, bottom-left) and y-sorted by it | `PlacedTile`s drawn by `TilemapUI` with one `CloneTiles` blank per texture and size; floor layers under the characters, y-sorted rows with them, z > 0 over them |
| `Player` (`GreenNinja`, the pack's NinjaGreen, the hooded ninja of its screenshots) | the player's sprite (Godot 4's NinjaBlue, helmeted, looks like one of the samurai) |
| its world at (0, 0) | moved by (-456, -1128), so its screens land exactly on our camera grid and its southern tree line runs along the Godot 4 village's open north edge |
| a strip south of its tree line that leads off the map (unfinished, like the Godot 4 village's open north edge) | the tree line opened at x 704-767 (Godot 3), floor laid through: the path between the two villages |
| cells using deleted tiles (holes) | floor holes patched with a neighbour's tile |
| its house interior | the Godot 4 village's house interior moves two screens east, out of its way |
| `Teleporter` (lands 10 px along the *entered* teleporter's direction) | `Teleporter(reach = 10, keepsOffset = false)` |
| `MusicArea`s and weather emitters placed in the world | environment zones (`NorthVillage.zones`): each music area with the weather of the emitters inside it; a quiet zone for the dungeon |
| `Npc` + `DialogArea` (dialogue disabled: the Dialogic timelines aren't in the project) | `Villager`s with their `AnimationPlayer` routines, `talkArea`, speech bubble, `DialogueUI` (the box, portraits and arrow from its `Hud/DialogBox`); the lines are ours |
| `DestroyableItem` (plants) | `PropKind.Plant` props |
| `Smoke` particles | `SmokeEmitter`s, drawn by `ActorsUI.smokeUI` |
| its `WorldEnvironment` (glow, saturation 1.1) | not ported: our colour grading works per channel |

Every edge of the combined floor that opens onto nothing gets a 16px wall (on the Godot 4 grid).

## Combat, from the Godot 3 version

The Godot 4 demo has no attack and no enemies. The older Godot 3 version of the demo (MIT,
© 2020 Emilio Coppola) does, and its combat is ported on top of the V4 game, with its numbers
(`common/constant/Combat.scala` names each one and its source):

| Godot 3 | Indigo port |
| --- | --- |
| `Player.gd` `action` (Space; A on a gamepad): swing every 0.2s while held, standing still, attack frame (sheet row 4) | `WorldScene.swing`, `movePlayer`, `Input.isAttackHeld`; V3's `AttackD` points at a walk frame (a typo), so all four directions use row 4 |
| `Weapon.gd` / `Weapon.tscn`: the lance (6x16) stabs out over 0.2s (quart ease out-in), rotated to the facing, in front only when facing down; its hitbox (6x16, 15px out) hits each thing once for 3 damage; `Fx.png` impact over 0.1s | `weaponArea`, `strike`, `impactPoint`; drawn by `ActorsUI` |
| `Monster.gd` / `Monster.tscn` (Bamboo): 6 life and a mini life bar, flash and grow when hit, shrink away on death; body 10x6 against walls only | `Monster`, `monsterHurtArea`, `ActorsUI` |
| `RandomMoveBehavior.gd`: every 1s stop, or set off in a random direction at 40 px/s; starts once on screen | `moveMonsters` |
| `Monster._on_AreaHitBox_body_entered`: 1 damage and a push of 50 px/s when its hurt area (12x20) enters the player | `hurtPlayer`; the push decays by 10% per 60 fps frame (`movePlayer`) |
| `LifeBar.gd`: hearts drain and refill at 10 life/s | `drainLife`, `Model.lifeShown` |
| `Player.death`, `Hud.revive`: death frame for 0.5s, fade, full life, back at the start | `revive`, the revive fade in `WorldScene.ui` |
| `Camera.reset_map`: once the camera reaches a new screen, dead monsters revive and broken props come back | `resetScreen`, `Model.resetPending` |
| `SndDeath.wav` on a hit, `SndGrass.wav` when a prop breaks | `soundsOf` |
| `Tuto.gd`: the move / attack banner, gone once the player has done both (checked every 0.5s, slides down over 1s) | `noteTutorial`, `hideTutorial`, `TutorialUI` |

The six monsters stand where Godot 3 puts them: three in its dungeon, three in its rainy forest
(`NorthVillage.monsterStarts`).

## The TileMap encoding

Godot 4 stores each layer's tiles in `layer_N/tile_data` as three ints per tile:

```
[ (y << 16) | (x & 0xFFFF),  source | (atlasX << 16),  atlasY | (alternative << 16) ]
```

`x` and `y` are signed 16-bit: `x` is the sign-extended low 16 bits, and `y` the arithmetic
`value >> 16`. (Decoding `y` as `(value >> 16) & 0xFFFF` places every tile with a negative `x` one
row too low.) Source ids 0 to 4 are the tileset images; source 5 is a scene collection whose tiles
are the destroyable props (1 crate, 2 grass, 3 pot).

The layers are `Wall` (0) and `Wall2` (1), y-sorted with origin -5, `FloorDetail` (2, z -1) and
`Floor` (3, z -2). The village overrides the TileMap's position to (8, -5)
(`VillageTiles.origin`), so a cell's centre is `(8, -5) + cell * 16 + 8`.

## Draw order

Godot draws by z-index, then tree order. The demo's world scene adds the map after the weather
nodes, so at z 0 the map (walls, trees, characters) is drawn over rain, splashes and clouds.
`WorldScene.ui` reproduces this: floor, ground weather, y-sorted walls and characters, tiles with
a positive z, then the `weather` layer for leaves, rays and fog, the HUD, the fades, and finally
the colour grading over everything (Godot's `ScreenFxLayer` is canvas layer 10).
