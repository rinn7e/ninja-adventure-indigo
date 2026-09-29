# Advanced Indigo features, to be tested in a larger game

Indigo (0.30.0-M6) has features this game doesn't use. Some we didn't know about while porting
(see [functional-game-dev.md](functional-game-dev.md)); others only prove their worth at a scale
this game doesn't reach: 6 monsters, 6 villagers and about 60 props are few enough that one model
and one update stay easy to follow.

This page lists them, why they're not used here, and what a larger game would need in order to
test them properly. The asset pack has plenty for one: 50+ characters, 30+ monsters, 9 bosses,
60+ items, 100+ sounds and 37 music tracks, of which this game uses a small slice.

## The features

### Performers and `StageManager` (`indigoextras.performers`, new in 0.30)

An entity system built on a `SubSystem`. Performers update and draw themselves, have a draw depth
(`PerformerDepth`), can have a physics collider, and can listen to and emit events:

- **Extra**: a background entity that updates and draws itself, with no events;
- **Stunt**: an Extra whose motion comes from the physics simulation;
- **Lead**: a full entity, with a collider and events;
- **Narrator**: influences events without being seen.

It's the closest thing Indigo has to Godot's nodes, and it's still functional: a performer's
`update` returns a new performer.

**Why not here:**

- The gameplay is mostly interactions *between* entities (the lance and monsters, monsters and the
  player, talking, screen resets), which would become events bouncing between performers.
- Its physics (`indigo-physics`) has box and circle colliders only, and our walls are polygons.
- It's new in a milestone release, with few examples yet.
- It keeps state outside the game's model, which the template's "one model, one update" rule
  avoids.

**A larger game would test it with** many enemy types with their own behaviour (chasers, shooters,
bosses with phases), spawners, and lots of ambient characters. A first, low-risk experiment:
this game's villagers as Extras, since they mostly act alone.

### `Signal`, `SignalFunction` and `Timeline` (`indigo.core.temporal`, `indigo.core.animation`)

Values as functions of time. `Signal` has `Lerp`, `EaseIn`, `EaseOut`, `EaseInOut`, `Pulse`, and
time clamping and wrapping; `SignalFunction` composes them; `Timeline` animates values over time
windows.

**Why not here:** we didn't know about them, and hand-wrote every ease with a start time in the
model. The idea is the same; Indigo's version is ready-made and composable. Godot's circular,
elastic and quartic out-in curves aren't built in.

**A larger game would test it with** boss attack patterns, cutscenes, screen shakes and scripted
sequences. In this game it can replace the camera slide, fades, the tutorial slide and the speech
bubble's pop with little risk.

### `Clip` and `Sprite` (`indigo.scenegraph`)

Sprite-sheet animation played by the engine.

**Why not here:** we didn't know about them, and crop every animation frame by hand.

**A larger game would test it with** dozens of monster sheets and attack, hit and death
animations. In this game it fits looping animations first (water, the monsters' walk, the speech
bubble, the impact effect); state-driven frames (walk, attack, death) need a closer look at its
API.

### The `Automata` subsystem (`indigoextras.subsystems`)

Spawns and retires short-lived effects driven by signals, outside the game's model.

**Why not here:** our few effects (impacts, prop bursts, smoke) live in the model and are drawn as
functions of time.

**A larger game would test it with** projectiles, hit sparks, spell effects and anything else
that exists for a moment in large numbers. Game logic should never need to read what it spawns.

### `SubSystem`s (`indigo.shared.subsystems`)

Self-contained mini-programs with their own model, `update` and `present`, talking to the game
through events. Performers and Automata are built on them; this game only uses one, the FPS
counter.

**A larger game would test it with** an inventory, a quest log, a save system, or a pause menu
(like the one in the asset pack's preview): features that are mostly independent of the world.

## Suggested order

1. **`Signal` / `Timeline`** in this game: quick, low risk, and more idiomatic Indigo.
2. **`Clip`** for looping animations in this game.
3. **`Automata`** for purely visual effects.
4. **Performers**, first for the villagers as Extras, then for enemies once the game has enough
   kinds of them to make encapsulation pay off.
5. **`SubSystem`s** for the first feature that stands on its own (a pause menu or an inventory).

Each step can be checked the way this port was: the tests, then a live playthrough
([development-note.md](development-note.md)), and a note in
[functional-game-dev.md](functional-game-dev.md) on what it was like.

## Still missing from Indigo

Two things a larger game would still build itself, whatever it uses from the list above:

- **Character physics for polygon walls**: `indigo-physics` has no `move_and_slide`; keep
  `common/util/Collision.scala`.
- **Y-sorting**: Indigo orders by layer and node order; sorting by position stays ours.
