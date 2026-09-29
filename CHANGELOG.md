# Changelog

All notable changes to **ninja-adventure-indigo** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/) and adheres to [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added

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
- **Godot importer**: `ninja/tools/import_godot.py` copies the assets and generates the map data
  (pure Python, no dependencies).
- **Unit tests** for collision, the TileMap decoding, motion, input, letterboxing, and the root
  and world updates.
- **Documentation**: `README.md`, `doc/code-convention.md` (the template's rules, plus this
  project's), `doc/porting-from-godot.md` (Godot to Indigo mapping).
