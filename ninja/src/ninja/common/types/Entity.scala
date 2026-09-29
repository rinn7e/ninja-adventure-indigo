package ninja.common.types

import indigo.*

object Entity:

  /** A destroyable prop (Godot's `Destroyable`, placed as scene tiles). Scene ids follow the
    * tileset's scene collection: 1 crate, 2 grass, 3 pot.
    */
  enum PropKind derives CanEqual:
    case Crate, Grass, Pot

  /** `brokenAt` is when it was destroyed; it then plays its break effect and disappears. */
  final case class Prop(kind: PropKind, position: Vector2, brokenAt: Option[Seconds])
      derives CanEqual

  /** Two-way teleporter (Godot's `Teleporter`): stepping into `area` sends the player to the
    * `target` teleporter, `direction * 25` pixels past it.
    */
  final case class Teleporter(
      position: Vector2,
      area: Rectangle,
      direction: Vector2,
      target: Int
  ) derives CanEqual
