# Functional game dev: notes from porting Godot to Indigo

This project ported two Godot demos, written the usual way (a tree of mutable nodes wired with
signals, tweens and timers), to Indigo, where a game is one immutable `Model`, a pure `update`
and a pure `ui`. Every Godot script was read line by line and rewritten. These are the notes from
doing that: what got better, what got harder, and what was surprising.

## The two mindsets

| Godot | Indigo (as used here) |
| --- | --- |
| A scene tree of nodes, each owning its state (`position`, `velocity`, `life`) | One `Model` value; nothing else holds state |
| `_process(delta)` on every node, in tree order | One `update(shared, msg, model)`, a pipeline of `Model => Model` steps in an order you write down |
| Signals (`body_entered`, `timeout`, `animation_finished`) | Nothing happens unless `update` checks for it |
| `Tween`, `Timer`, `AnimationPlayer` running in the background | A start time in the model, and a function of the clock |
| Nodes draw themselves | `ui(model)` returns a description of the frame, from scratch, every frame |
| `move_and_slide`, areas, physics layers | Our own functions over geometry (`Collision.scala`) |

## What got better

### Tweens and timers become timestamps

Almost every Godot `Tween` and `Timer` turned into one field holding *when it started*, plus a
function that computes the current value from the clock:

- The camera slide (`CameraGrid`'s 0.8s sine tween) is `camera.since`, and `cameraPosition(now)`
  eases between cells.
- The teleport fade, the revive fade, the hit flash, the tutorial sliding away and the music
  cross-fade: `teleportedAt`, `revivedAt`, `hurtAt`, `tutorial.hidingAt`, `musicSince`.
- A villager's looping `AnimationPlayer` routine is data (keyframes) plus a clock; *pausing* it
  while it talks is just remembering when it paused and shifting the clock after
  (`VillagerState.shift`).

There's nothing to start, stop, kill or leak. Godot's camera script has to `tween.kill()` before
starting a new slide; here a new slide just replaces the timestamp. Two effects can't drift out of
sync, and any frame can be drawn from the model alone.

### Particles as pure functions of time

Godot's `GPUParticles2D` simulates particles on the GPU. Here each particle is a closed-form
function of its index and the clock (`WeatherUI`): where it spawned (a hash of index and life
number), how far it has moved, which frame it shows. Rain splashes, Godot's *sub-emitter*, are
"where drop *i* was when it emitted", computed rather than remembered. Weather has no state at
all, so it can't pile up, it restarts cleanly, and a screenshot of any moment is reproducible.

The limit: this works for particles that don't interact with anything. Smoke that bumped into
walls would need real state.

### Effects are derived, not triggered

In Godot, a monster's `hit()` calls `$SndHit.play()`. Here, update steps only change the model,
and one final step, `soundsOf`, looks at what happened this frame (a `hitAt` or `brokenAt` equal
to now) and emits the sound events. Sound is a consequence of state, in one place, and no step
has to remember to play anything.

### Testing without the engine

Every rule is a plain function, so the 48 tests build a model by hand and call the step directly:
"a swing hits a monster once for 3 damage", "the patroller waits 3 seconds at the end of its
path", "the hearts drain at 10 per second". Randomness comes in through `Dice.loaded(n)`, time
through `Shared.now`. Nothing is mocked, and no engine runs.

### Tools come almost for free

Because the world is data, the collision polygons the game uses can be read by other programs
too. `game/tools/route.py` path-finds across the real map from the same generated data, and
`live.js` drives the real game in a browser; that's how doors, dialogue and the dungeon were
checked by playing, not by guessing. Resetting a screen (monsters revive, props come back) is
`map` over two lists. Saving a game would be serialising one value.

## What got harder

### No `move_and_slide`

Godot gives you character physics that slide along any collision polygon. Indigo's physics
library (`indigo-physics`) only has boxes and circles, and 83 of the tileset's 227 polygons aren't
rectangles, so collision is our own ~80-line move-and-slide. With 2,500 polygons across both maps
it also needed a spatial index (`WorldMap.solidsNear`). A functional engine gives you less out of
the box; you write more of it yourself.

### Signals have to be rebuilt as state

`body_entered` fires once, when something *enters*. A pure update only sees "is it overlapping
now?", so every "on enter" needs a remembered previous value: a monster's `touching` flag (it
hurts only on entering), `confirmHeld` (a key press, not a held key), `nearVillager` with the time
the speech bubble should pop. Each is small, but each is something Godot did for you.

### Order is explicit, and it matters

Godot runs `_process` in tree order and physics in its own step; you rarely think about it. Here
the frame is a pipeline written in one place (`revive`, `noteTutorial`, `talk`, `swing`,
`movePlayer`, `strike`, ... `soundsOf`), and moving one line changes behaviour: talking must come
before swinging, or Space would attack the villager you meant to talk to. It's more honest, but
it's all on you.

### Composition is by hand

In Godot a `Hitbox` or `DamageArea` is a scene you drop onto a node, and behaviour appears. Here,
"the lance hits monsters and plants" is a function that knows about both lists. `Update.scala`
for the world grew past 500 lines; the convention of small curried steps keeps it readable, but
there's no inspector showing what's attached to what.

### Frame-rate-dependent code has to be fixed, not copied

Godot code often assumes a fixed frame: Godot 3's knockback does
`push_velocity.linear_interpolate(ZERO, 0.1)` every physics frame. A pure update gets a delta
time, so it became `push * pow(0.9, dt * 60)`. The Godot 4 pig keeps its velocity *in pixels per
frame*, faithfully, and is the one place that would behave differently at another frame rate.

### Drawing order is yours to build

Godot sorts by z-index, then tree order, with y-sort per node. Here there are layers, and y-sorting
is a sort we do each frame over tiles and characters. A few engine details bit: merging scene
fragments keeps the *first* camera a layer gets, and a flipped tile instance is mirrored around
its corner, so it has to start one tile further on.

### Performance needs care, not tricks

A new model every frame, a fresh scene description every frame, re-sorting everything by y: it's
fine at 60 FPS, but only because the 8,000 map tiles are instanced (`CloneTiles`) in static
batches the renderer caches. Naive "one node per tile" would not have been. Immutability didn't
cost much here; the drawing strategy mattered far more.

## Surprises from reading the Godot code closely

Rewriting everything as explicit data made the originals' quirks impossible to miss:

- Godot 3's `AttackD` animation points at a walk frame (a typo); every other direction uses the
  attack frame.
- Godot 3 monsters only ever play their "down" walk, whichever way they move.
- A Godot 3 teleporter lands you along the direction of the teleporter you *entered*, not the one
  you arrive at.
- Godot 3's villagers have dialogue areas, but the dialogue call is commented out; the dialogue
  files aren't in the project.
- Godot 4's weapon system is complete down to damage and teams, but `use_weapon()` only prints.
- Godot 4 defines a snow effect that no area ever turns on.
- Two Godot 3 floor cells use deleted tiles, so the original has holes there too.
- A Godot 3 villager stands inside a house's doorway, where the house hides her.
- Godot 3's camera finds its screen with `floor`, Godot 4's with `round` (hence Godot 4's camera
  offset).
- Godot's TileMap stores a cell's x in the low 16 bits and y in the high 16, both signed; getting
  the sign of y wrong shifts every tile left of x = 0 by a row, which is exactly what our first
  import did.

## Verdict

**Good:** determinism, testability, and reasoning. Anything time-based (tweens, fades,
particles, routines) is simpler and more robust as a function of the clock. Bugs are easier to
find because the whole state is one value you can print, and the order of events is written down.

**Bad:** you rebuild what an engine like Godot gives you for free: physics, signals, node
composition, an editor to see it all. More code, and the order and edge cases are your job.

**Cool:** the game becomes data that other tools can read. The same collision data drives the
game, a path-finder and a scripted player; a frame can be redrawn from any saved model; and a
port like this one surfaces every quirk of the original, because nothing can hide in a node.

For a small, precise 2D game like this one, the trade is a good one: a little more code up front,
and much less mystery later.
