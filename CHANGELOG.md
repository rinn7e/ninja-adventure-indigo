# Changelog

All notable changes to **ninja-adventure-indigo** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/) and adheres to [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added

- **Combat from the Godot 3 version of the demo** (MIT): the lance (hold Space), Bamboo monsters
  that wander, hurt and knock the player back, with life bars; death and revival; draining
  hearts; monsters and props resetting when the camera reaches a new screen; hit and break
  sounds; the controls tutorial.
- `doc/development.md`: build gotchas and how to check a change.
- **The Godot 3 demo's world, north of the Godot 4 village**: its map (village, lake, snow,
  rainy forest, house interior, dungeon) imported from its Godot 3 tile maps and joined to the
  Godot 4 village through a gap in the forest; its doors, music (four tracks), weather as
  environment zones (snow and sparks), its six monsters, breakable plants and smoke.
- **Villagers and dialogue**: the Godot 3 villagers with their idle routines, a speech bubble in
  talking range, and a dialogue box with portraits; Space talks next to a villager.
- Walls along every map edge that opened onto nothing.
- **Play it online**: every push to `master` runs the tests, builds the optimised game and
  publishes it to GitHub Pages (`.github/workflows/pages.yml`).
- F3 shows the player's position; `game/tools/route.py` and `game/tools/live.js` play the game
  from a script.

### Changed

- The code follows the template's layout: the Mill module, folder and package are `game`
  (`game/src/game/`), and the generated config is `BuildConfig`.
- The README now builds on the template's README instead of repeating it.
- The player is the Godot 3 version's green-hooded ninja (the pack's NinjaGreen) instead of the
  Godot 4 demo's helmeted NinjaBlue, which looks like a samurai.

### Fixed

- The font's space is now wide enough between words (the old width was appended, not replaced).

---

## [0.1.0] - 2026-09-29

### Added

- **The Ninja Adventure demo in Indigo**: the village map imported tile for tile from the Godot 4
  project (tilesets, layers, collision polygons, y-sorting, z-indexes), at Godot's 320x180
  resolution.
- **Characters and behaviours**: Godot's movement and exact polygon move-and-slide collision, a
  following samurai, a wandering pig, and a patrolling guard.
- **World features**: breakable grass, pots and crates; the screen-by-screen sliding camera;
  house doors with a fade transition; the hearts HUD.
- **Environment zones**: weather (rain with floor splashes, fog, clouds, leaves, light rays) in
  Godot's draw order, music with a fade, and colour grading (a blend shader mapping the screen
  through the zone's gradient, cross-fading over 0.5s).
- **Input**: keyboard and gamepad (D-pad, left stick with Godot's 0.5 deadzone).
- **Letterboxing**: the view is scaled by the largest whole number that fits the window and
  centred with black bars, in the browser and the desktop build.
- **Title screen**, so the browser allows music once a key is pressed.
- **Godot importer**: `game/tools/import_godot.py` copies the assets and generates the map data
  (pure Python, no dependencies).
- **Unit tests** for collision, the TileMap decoding, motion, input, letterboxing, and the root
  and world updates.
- **Documentation**: `README.md`, `doc/code-convention.md` (the template's rules, plus this
  project's), `doc/porting-from-godot.md` (Godot to Indigo mapping).
