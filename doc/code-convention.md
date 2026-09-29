# Code Conventions for `ninja-adventure-indigo`

This project follows the conventions of
[indigo-game-starter-template](https://github.com/rinn7e/indigo-game-starter-template): its
**[code-convention.md](https://github.com/rinn7e/indigo-game-starter-template/blob/master/doc/code-convention.md)**
(rules 0-6) and its README's guides for
[Scala / Indigo](https://github.com/rinn7e/indigo-game-starter-template#coming-from-scala--indigo-whats-unusual-here)
and [Elm / Haskell](https://github.com/rinn7e/indigo-game-starter-template#coming-from-elm--haskell-read-it-like-this)
developers. This file only lists what this project adds.

## What this project adds

### Faithful to the Godot original

- Port behaviour from the Godot project rather than inventing it, and say where it comes from in
  the doc comment (`Godot's CameraGrid`, `world.tscn's ColorCorrection`). Numbers copied from
  Godot keep Godot's values (speeds, durations, gradient stops, z-indexes) and live in
  `common/constant/` (`Village`, `Combat`, `Gradings`), each with the Godot file it comes from.
- Data that can be read from the Godot project is imported, not typed in:
  `game/tools/import_godot.py` generates `common/constant/VillageTiles.scala` (don't edit it by
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

### One frame, one pipeline

The world's `update` is a chain of pure `Model => Model` steps (`movePlayer`, `strike`,
`moveMonsters`, `resetScreen`, ...) piped together; the last step, `soundsOf`, turns what happened
this frame (a hit, a prop breaking) into `PlaySound` events in the `Outcome`. Steps stay easy to
test on their own.

### Time-based effects are pure functions

Particles, fades, the camera slide and the music cross-fade are computed from the clock and a
start time stored in the model (`environmentSince`, `teleportedAt`, `camera.since`), never
simulated step by step. Random-looking values come from a hash of the particle's index and life
(`WeatherUI.noise`), so a frame can be drawn from the model alone.
