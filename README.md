# ninja-adventure-indigo

A port of the [Ninja Adventure](https://pixel-boy.itch.io/ninja-adventure-asset-pack) Godot 4 demo
to [Indigo](https://indigoengine.io) `0.30.0-M6`, a purely functional game engine for Scala 3 /
Scala.js. It is structured like a TEA ([The Elm Architecture](https://guide.elm-lang.org/architecture/))
web app, following [indigo-game-starter-template](https://github.com/rinn7e/indigo-game-starter-template).

The aim is to match the original as closely as possible: the same map, characters, collision,
camera, weather, colour grading and music, at the same 320x180 resolution.

## Screenshots

| Title | Village | Swamp | House |
| --- | --- | --- | --- |
| ![Title](doc/screenshot/title.png) | ![Village](doc/screenshot/village.png) | ![Swamp](doc/screenshot/swamp.png) | ![House](doc/screenshot/house.png) |

## What's in it

- **The village map**, imported tile for tile from the Godot project (5 tilesets, 4 layers, exact
  collision polygons, y-sorting and z-indexes).
- **Characters** with Godot's movement (100 px/s, acceleration 1000, deceleration 800) and
  move-and-slide collision against the tiles' polygons: the player (NinjaBlue), a follower
  (SamuraiGreen), a wandering pig, and a patrolling guard (SamuraiBlue) that waits at each end of
  its path.
- **Breakable grass, pots and crates**, with particle bursts.
- **The screen-by-screen camera** (`CameraGrid`), sliding to the next screen over 0.8s.
- **Teleporters** (house doors), with Godot's fade transition.
- **Environment zones**: walking into one changes the weather (rain with splashes, fog, clouds,
  leaves, light rays), the music (with a fade), and the colour grading (the swamp's gradient
  shader, cross-fading over 0.5s).
- **The hearts HUD.**
- **Keyboard and gamepad** input, as in Godot's input map (D-pad, left stick with a 0.5 deadzone).
- **Letterboxing**: the 320x180 view is scaled by the largest whole number that fits the window
  (4x at 1280x720, 6x at 1920x1080) and centred with black bars, in the browser and in the
  desktop (Electron) build alike.

The title screen is an addition: the Godot demo starts straight in the world, but a browser only
plays music after a key press, which the title provides.

## Controls

| Input | Action |
| --- | --- |
| Arrows / WASD, D-pad, left stick | Walk |
| Space / Enter / Z, Cross (A) / Options (Start) | Start the game from the title |

Walk into grass, pots and crates to break them, and into a house door to go inside.

## Requirements

- A JDK (tested with OpenJDK 21), for Mill and the Scala compiler.
- Node.js (tested with 24), which runs the Scala.js unit tests.
- Python 3, to serve the build locally and to re-import from the Godot project.

Mill itself needs no install: the bundled `./mill` launcher downloads the pinned version.

## Commands

This is a Mill 1.x build (the version is pinned in `.mill-version`). The game is the `ninja`
module: sources in `ninja/src/ninja/`, tests in `ninja/test/src/ninja/`, assets in `ninja/assets/`.

```bash
./mill ninja.test          # run the unit tests (pure game logic)
./mill ninja.indigoBuild   # build a static site into out/ninja/indigoBuild.dest
./mill ninja.indigoRun     # run as a desktop app (Electron)
./mill __.reformat         # scalafmt
./mill clean ninja         # if the compiler reports an old signature through a barrel
```

To play in a browser, serve the build output:

```bash
python3 -m http.server 8787 --directory out/ninja/indigoBuild.dest
```

## Re-importing from the Godot project

Everything the game needs is already in the repository: the images and music in `ninja/assets/`,
and the map in `ninja/src/ninja/common/constant/VillageTiles.scala`, both written by
`ninja/tools/import_godot.py`. To re-run the importer (pure Python, no dependencies), download the
Godot project from <https://github.com/pixel-boy/NinjaAdventure> into
`assets/NinjaAdventure Godot V4/`, then:

```bash
python3 ninja/tools/import_godot.py
```

The Godot project isn't included here because it has no licence (see [Credits](#credits)).

## Coming from Scala / Indigo, or from Elm / Haskell?

The code follows the template's conventions, which read like a TEA web app rather than typical
Indigo code: the root routes between scenes itself (no Indigo `Scene`s), parents intercept their
children's messages (no `OutMsg`), one file is one module, barrels sit next to their folders, and
helpers are plain curried functions, data last, chained with `pipe`. The template's README has
guides for both backgrounds:

- [Coming from Scala / Indigo? What's unusual here](https://github.com/rinn7e/indigo-game-starter-template#coming-from-scala--indigo-whats-unusual-here)
- [Coming from Elm / Haskell? Read it like this](https://github.com/rinn7e/indigo-game-starter-template#coming-from-elm--haskell-read-it-like-this)

The rules, with what this project adds, are in **[doc/code-convention.md](doc/code-convention.md)**,
and **[doc/porting-from-godot.md](doc/porting-from-godot.md)** maps each part of the Godot demo to
its Indigo counterpart.

**Where to start:** `ninja/src/ninja/Main.scala`, then `scene/WorldScene.scala` and the files in
`scene/worldscene/`: `Update.scala` is the game logic.

## Architecture

```
ninja/src/ninja/
  Main.scala           entry point, the Indigo Game, mainUI (layers, letterbox bars)
  Type.scala           root Model (route + scene models), Msg
  Update.scala         root init, update: delegate, intercept, route
  Subscription.scala   the showing scene's subscriptions, wrapped
  common/
    Types.scala        barrel: re-exports types/ (import ninja.common.Types.*)
    types/             Actor, Ambience (zones, weather, music, grading), Entity (props,
                       teleporters), Facing, SceneRoute, Shared, Tiles
    util/              pure helpers: Collision (move-and-slide), Motion, Input, Screen
                       (letterboxing), TileMap
    constant/          Layout, Layers, Village (characters, zones, teleporters), Gradings,
                       VillageTiles (generated from the Godot map)
    asset/             GameAssets (asset list, font, music)
  ui/                  reusable UI: CharacterSpriteUI, HeartBarUI, LabelUI, FadeInUI,
                       ColorGradingUI (the grading blend shader)
  theme/               Palette
  scene/
    TitleScene.scala  WorldScene.scala      barrels: each re-exports its folder
    titlescene/  worldscene/
      Type.scala  Update.scala  Subscription.scala  UI.scala
      subui/             TilemapUI, ActorsUI, WeatherUI
      common/            Util: bodies, camera grid
ninja/test/src/ninja/  mirrors the source tree
ninja/tools/
  import_godot.py      copies assets and generates VillageTiles.scala from the Godot project
```

There is no mutable state: time, the frame's `Dice` and the window size arrive in `Shared`, and
every effect (weather particles, fades, the camera slide) is a pure function of the model and the
clock.

### Performance

The map's ~3,500 tiles are drawn with instanced `CloneTiles`: static batches for the floor and
for each row of walls (so characters can be y-sorted between them), and a small animated batch for
water. The game runs at 60 FPS; the FPS counter is in the bottom-left corner.

## Credits

- Art, music and font: the [Ninja Adventure asset pack](https://pixel-boy.itch.io/ninja-adventure-asset-pack)
  by [Pixel-boy](https://pixel-boy.itch.io/) and [AAA](https://www.instagram.com/challenger.aaa/),
  released under [CC0](assets/Ninja%20Adventure%20-%20Asset%20Pack/LICENSE.txt) (included in
  `assets/`). Support them on [Patreon](https://www.patreon.com/pixelarchipel).
- Game design, map and behaviour: the Ninja Adventure Godot 4 demo by Pixel-boy,
  <https://github.com/pixel-boy/NinjaAdventure>. This project is an independent re-implementation
  in Scala; the demo's scripts and scenes are not redistributed.
- Nine images in `ninja/assets/` come from that demo rather than the pack: `crate`, `pot`, `grass`,
  `pig`, `shadow`, `fx_cloud`, `fx_fog`, `fx_rain` and `tileset_animated`. They are the same
  author's versions of the pack's art, made for the demo; the demo repository states no licence.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

The code is [MIT](LICENSE). The assets keep their own terms: the asset pack in `assets/` and the
images and music copied from it into `ninja/assets/` are CC0; see [Credits](#credits) for the nine
images taken from the Godot demo.
