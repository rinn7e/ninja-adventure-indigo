package ninja.common.types

import indigo.*

/** Per-frame values every scene's `update` and `ui` receive: the clock, the time since the last
  * frame (for movement), and the frame's deterministic dice.
  */
final case class Shared(now: Seconds, delta: Seconds, dice: Dice)

object Shared:

  /** Before the first frame, when Indigo gives `setup` only the dice. */
  def atStartup(dice: Dice): Shared =
    Shared(now = Seconds.zero, delta = Seconds.zero, dice = dice)

  def fromContext(context: Context): Shared =
    Shared(
      now = context.frame.time.running,
      // Capped, so a stalled frame (e.g. a background tab) can't teleport anything through walls.
      delta = context.frame.time.delta.min(Seconds(0.1)),
      dice = context.frame.dice
    )
