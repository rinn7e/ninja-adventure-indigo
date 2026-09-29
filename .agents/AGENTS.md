# Workspace Rules

- Always follow [doc/code-convention.md](../doc/code-convention.md) (the template's rules plus this
  project's), and port behaviour from the Godot project rather than inventing it; keep
  [doc/porting-from-godot.md](../doc/porting-from-godot.md) in sync.
- Before finishing a change: `./mill __.reformat`, then `./mill ninja.test` (the build uses
  `-Werror`, so warnings such as unused imports fail it). If the compiler reports stale signatures
  through a barrel, run `./mill clean ninja` first.
- Don't edit `ninja/src/ninja/common/constant/VillageTiles.scala` by hand: change
  `ninja/tools/import_godot.py` and re-run it (it needs the Godot project in
  `assets/NinjaAdventure Godot V4/`, see the README).
- Add user-facing changes to [CHANGELOG.md](../CHANGELOG.md) under `[Unreleased]`.
