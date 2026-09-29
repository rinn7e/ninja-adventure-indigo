# Code Conventions for `ninja-adventure-indigo`

This project follows the conventions of
[indigo-game-starter-template](https://github.com/rinn7e/indigo-game-starter-template): read its
**[code-convention.md](https://github.com/rinn7e/indigo-game-starter-template/blob/master/doc/code-convention.md)**
for the rules and their reasons. In short:

0. **One file, one module**: `X.scala` defines `object X`; barrels sit next to their folder
   (`scene/WorldScene.scala` re-exports `scene/worldscene/`, `common/Types.scala` re-exports
   `common/types/`).
1. **Anything that is or returns a drawing type ends in `UI`** (`floorUI`, `gradingUI`,
   `val groundUI`).
2. **Stateless UI** is an `XxxUI` module: `ui/` when shared, `scene/<name>/subui/` when not.
3. **Stateful UI** is a TEA module folder (none needed in this game yet).
4. **TEA child msg interception**, no `OutMsg`: the root delegates to a scene's `update` and reads
   its updated model.
5. **Plain curried functions, data last, chained with `pipe`**:
   `model.pipe(movePlayer(move, dt, now)).pipe(breakProps(now))`.
6. **A scene's `init(shared, ...)` returns `Outcome[Model]`.**

## What this project adds

### Faithful to the Godot original

- Port behaviour from the Godot project rather than inventing it, and say where it comes from in
  the doc comment (`Godot's CameraGrid`, `world.tscn's ColorCorrection`). Numbers copied from
  Godot keep Godot's values (speeds, durations, gradient stops, z-indexes), named in
  `common/constant/`.
- Data that can be read from the Godot project is imported, not typed in:
  `ninja/tools/import_godot.py` generates `common/constant/VillageTiles.scala` (don't edit it by
  hand) and copies the assets.
- When Indigo's defaults differ from Godot's, match Godot. For example, collision is our own exact
  polygon move-and-slide (`common/util/Collision.scala`), not `indigo-physics`, which only has
  boxes and circles.

### Screen space and letterboxing

- Everything is laid out in the fixed 320x180 view (`Layout.screen`), in game pixels. Never read
  the window size for layout; `Shared.viewport` is only for `common/util/Screen.scala`.
- `mainUI` scales every layer by `Screen.scaleFor(viewport)` and centres the screen-space layers.
  Merging fragments with `|+|` keeps the **first** camera a layer is given, so `mainUI` leaves the
  world layer's camera to the scenes, which set it with
  `Screen.centredCamera(shared.viewport)(topLeft)`.
- Layers, back to front (`common/constant/Layers.scala`): `world`, `weather`, `ui`, `screen`
  (fades), `grading` (the colour grading shader, which re-colours everything below it), `fps`,
  `bars` (the letterbox).

### Time-based effects are pure functions

Particles, fades, the camera slide and the music cross-fade are computed from the clock and a
start time stored in the model (`environmentSince`, `teleportedAt`, `camera.since`), never
simulated step by step. Random-looking values come from a hash of the particle's index and life
(`WeatherUI.noise`), so a frame can be drawn from the model alone.
