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

This is the idiomatic Indigo pattern, and Indigo has ready-made parts for it that this port
didn't use: `Signal` (`Lerp`, `EaseIn`, `EaseOut`, `EaseInOut`, `Pulse`, time clamping and
wrapping), `SignalFunction` to compose them, and `Timeline` for multi-step animations. We wrote
the curves by hand, partly because Godot's specific ones (circular, elastic, quartic out-in) aren't
among Indigo's built-ins.

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

## Whose fault is it: functional style, Indigo, or us?

Not every downside above is the price of functional programming, and not every one is Indigo's.
Checked against Indigo 0.30.0-M6's source, they sort into four groups.

### What Indigo is truly missing

These are real gaps; knowing Indigo better would not have helped.

- **Character physics for polygons.** `indigo-physics` is a rigid-body simulation with box and
  circle colliders only, and has no character controller like Godot's `move_and_slide`. The
  tileset's collision shapes are mostly non-rectangular, so our own polygon move-and-slide
  (`Collision.scala`, with a spatial index) was necessary.
- **Y-sorting.** Indigo orders the frame by layers and by the order nodes are listed; nothing sorts
  them by position. We sort tiles and characters by y every frame ourselves. (Performers, below,
  are drawn in an integer `PerformerDepth` order, which you could set from y, but that's still
  sorting by hand.)

### What we didn't know about Indigo

These existed, and we rebuilt them because we didn't know or didn't look:

- **An entity system: `StageManager` and performers** (`indigoextras.performers`, new in 0.30).
  Performers (Lead, Extra, Stunt, Narrator) update and draw themselves, have a draw depth, can
  have a physics collider, and can listen to and emit events: the closest thing to Godot's nodes.
  Monsters, villagers and props could have been performers instead of lists in one 588-line
  update.
- **Time-based values: `Signal`, `SignalFunction` and `Timeline`.** `Signal` has `Lerp`,
  `EaseIn`, `EaseOut`, `EaseInOut`, `Pulse`, and time clamping and wrapping; `Timeline` animates
  values over time windows. We hand-wrote every ease. (Godot's specific curves, circular, elastic
  and quartic out-in, aren't built in, so those would still be hand-written.)
- **Sprite-sheet animation: `Clip` and `Sprite`.** We computed crop rectangles for every frame by
  hand.
- **Short-lived effects: the `Automata` subsystem**, which spawns and retires effects like impacts
  and bursts, driven by signals. We kept them in the model.
- **A merge rule:** combining scene fragments with `|+|` keeps the *first* camera a layer is given.
  We found it by breaking the camera.

### The price of the functional style

These come from keeping `update` pure, whatever the engine:

- **"On enter" needs remembered state.** A pure update only sees "overlapping now"; to act once on
  entering, it must remember the last frame (a monster's `touching`, `confirmHeld`). Indigo's
  events can carry "it happened" onward, but detecting it is still yours.
- **The order of the frame is explicit.** Every step runs in an order written in one place, and
  moving one line changes behaviour. That's honest, and arguably a feature, but it's work.

### Our deliberate choice

- **One model, one update.** The template keeps the whole game in a single TEA `Model` and
  `update`, so every rule is a pure function you can test and every piece of state is visible in
  one place. That shape is also why we didn't reach for Indigo's composition tools (subsystems,
  performers, automata): they keep state inside themselves. A performer-based version would read
  more like the Godot original, at the cost of state living in more than one place.

### Not Indigo's fault at all

- **Frame-rate-dependent code** came from Godot 3's scripts; Indigo hands every update a delta
  time.
- **The 64 KB limit** on the generated data's string literals comes from Scala.js.
- **Performance** was fine: `CloneTiles` in static batches is Indigo's documented answer for big
  tile maps, and it held 60 FPS with 8,000 tiles.

So, of the real costs: two are Indigo's gaps (polygon character physics, y-sorting), two are the
functional style's (edge detection, explicit order), and the rest were our unfamiliarity or our
choice.

## If we did it again

What we'd try, keeping what worked (pure logic, the timestamp pattern, tests without the engine):

- **Performers for the cast.** Monsters, villagers and props as `StageManager` performers that
  update and draw themselves, set their `PerformerDepth` from their y, and report "hit" or
  "entered" as events: most of the 588-line world update would split into small, self-contained
  pieces, much like Godot's scenes. The trade-off is state living inside a subsystem instead of
  the one model the tests can see, so keep the rules themselves (damage, wander, routines) as pure
  functions the performers call.
- **`Signal` and `Timeline` for time-based effects**, adding only the curves Indigo lacks
  (circular, elastic, quartic out-in) as small signal functions.
- **`Clip` for sprite-sheet animation**, instead of computing crop rectangles by hand.
- **`Automata` for fire-and-forget effects** (impacts, bursts, smoke puffs), so the model only
  holds what the game logic needs to know.
- **Keep our own collision.** Indigo has nothing closer to `move_and_slide` for polygons, and the
  spatial index made it cheap enough.

The lesson for the template: "one model, one update" is the right place to start (everything is
visible and testable), but a game this size is where Indigo's own composition tools start to pay
for themselves.

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

**Bad:** you rebuild some of what an engine like Godot gives you for free: character physics and
y-sorting (Indigo's real gaps), and the "on enter" edges (the functional style's price). Node-like
composition and tweens exist in Indigo (performers, signals, timelines); this port chose a
single-model TEA structure over them, which costs code but keeps everything in one testable
place.

**Cool:** the game becomes data that other tools can read. The same collision data drives the
game, a path-finder and a scripted player; a frame can be redrawn from any saved model; and a
port like this one surfaces every quirk of the original, because nothing can hide in a node.

For a small, precise 2D game like this one, the trade is a good one: a little more code up front,
and much less mystery later.
