# Workspace Rules

- Always follow [doc/code-convention.md](../doc/code-convention.md) (the template's rules plus this
  project's), and port behaviour from the Godot project rather than inventing it; keep
  [doc/porting-from-godot.md](../doc/porting-from-godot.md) in sync.
- Before finishing a change: `./mill __.reformat`, then `./mill game.test` (the build uses
  `-Werror`, so warnings such as unused imports fail it). If the compiler reports stale signatures
  through a barrel, run `./mill clean game` first.
- Don't edit `game/src/game/common/constant/VillageTiles.scala` by hand: change
  `game/tools/import_godot.py` and re-run it (it needs the Godot project in
  `assets/NinjaAdventure Godot V4/` and `assets/NinjaAdventure Godot V3/`, see the README).
- For anything the template already covers, link to it rather than repeating it.
- Add user-facing changes to [CHANGELOG.md](../CHANGELOG.md) under `[Unreleased]`.
