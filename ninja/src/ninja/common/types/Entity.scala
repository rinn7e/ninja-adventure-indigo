package ninja.common.types

import indigo.*

object Entity:

  /** A destroyable prop (Godot's `Destroyable`, placed as scene tiles). Scene ids follow the
    * tileset's scene collection: 1 crate, 2 grass, 3 pot.
    */
  enum PropKind derives CanEqual:
    case Crate, Grass, Pot, Plant // Plant: the Godot 3 version's `DestroyableItem`

  /** `brokenAt` is when it was destroyed; it then plays its break effect and disappears. */
  final case class Prop(kind: PropKind, position: Vector2, brokenAt: Option[Seconds])
      derives CanEqual

  /** Two-way teleporter (Godot's `Teleporter`): stepping into `area` sends the player to the
    * `target` teleporter, `reach` pixels past it along `direction`. Godot 4's keep the player's
    * offset from the teleporter it entered (and reach 25); Godot 3's land on the spot (reach 10).
    */
  final case class Teleporter(
      position: Vector2,
      area: Rectangle,
      direction: Vector2,
      target: Int,
      reach: Double,
      keepsOffset: Boolean
  ) derives CanEqual

  /** How a villager's sprite sheet is laid out (Godot 3's `Npc` sprite frames): the usual 4 columns
    * (down, up, left, right) with rows 0-3 walking; a two-row breathing idle (OldWoman); or a
    * two-frame strip that flips to face right (the dog).
    */
  enum SheetKind derives CanEqual:
    case Standard, BreathingIdle, Strip

  /** A villager's looping `Idle` animation (Godot 3's `AnimationPlayer` tracks): positions eased
    * linearly between keys, and facing, walking and flip set at theirs. Times in seconds.
    */
  final case class Routine(
      length: Double,
      positions: Batch[(Double, Vector2)],
      facings: Batch[(Double, Facing)],
      walking: Batch[(Double, Boolean)],
      flips: Batch[(Double, Boolean)]
  ) derives CanEqual

  /** A villager of the Godot 3 version (`Npc` with a `DialogArea`): where it stands, its routine,
    * and what it says (its lines are ours: the Godot 3 project's dialogue files aren't in it).
    */
  final case class Villager(
      name: String,
      sheet: AssetName,
      sheetKind: SheetKind,
      faceset: AssetName,
      position: Vector2,
      facing: Facing,
      routine: Option[Routine],
      talkOffset: Vector2,
      lines: Batch[String]
  ) derives CanEqual

  /** A smoke plume (the Godot 3 version's `Smoke` particles): puffs rising from `position`, tinted,
    * either y-sorted with the scene or drawn over everything (`onTop`, a chimney's).
    */
  final case class SmokeEmitter(position: Vector2, puffs: Int, tint: RGBA, onTop: Boolean)
      derives CanEqual
