package game.common.types

import indigo.*
import game.common.constant.Layout

/** Per-frame values every scene's `update` and `ui` receive: the clock, the time since the last
  * frame (for movement), the frame's deterministic dice, and the window's size in screen pixels
  * (for letterboxing, see `Screen`).
  */
final case class Shared(now: Seconds, delta: Seconds, dice: Dice, viewport: Size)

object Shared:

  /** Before the first frame, when Indigo gives `setup` only the dice: the viewport is the
    * configured 1280x720 window; the real one arrives with the first frame.
    */
  def atStartup(dice: Dice): Shared =
    Shared(
      now = Seconds.zero,
      delta = Seconds.zero,
      dice = dice,
      viewport = Layout.screen * Layout.magnification
    )

  def fromContext(context: Context): Shared =
    Shared(
      now = context.frame.time.running,
      // Capped, so a stalled frame (e.g. a background tab) can't teleport anything through walls.
      delta = context.frame.time.delta.min(Seconds(0.1)),
      dice = context.frame.dice,
      viewport = context.frame.viewport.size
    )
