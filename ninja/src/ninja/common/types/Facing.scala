package ninja.common.types

/** Which way a character looks. `column` is the column of a character sprite sheet (Godot's
  * `SpriteCharacter.FrameDirection`): down, up, left, right.
  */
enum Facing(val column: Int) derives CanEqual:
  case Down  extends Facing(0)
  case Up    extends Facing(1)
  case Left  extends Facing(2)
  case Right extends Facing(3)
