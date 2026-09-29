# Porting from Godot

How each part of the Ninja Adventure Godot 4 demo
([pixel-boy/NinjaAdventure](https://github.com/pixel-boy/NinjaAdventure)) maps to this Indigo
port. Godot paths are relative to the Godot project; Scala paths to `ninja/src/ninja/`.

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

Not ported, because the demo doesn't use them: `Snow` (no zone enables it), the weapon (its
`use_weapon()` only prints) and character hitboxes (nothing is connected to them).

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
