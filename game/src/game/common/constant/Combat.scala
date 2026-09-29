package game.common.constant

import indigo.*

/** Combat, from the Godot 3 version of the demo (MIT, © 2020 Emilio Coppola), which V4 lacks. The
  * source of each value is in brackets.
  */
object Combat:

  // --- The player's lance (Player.gd, Weapon.gd, Weapon.tscn) ----------------------------------

  /** A swing blocks the player's input and shows the attack frame this long (`ActionTime`). */
  val swingTime: Seconds = Seconds(0.2)

  /** Damage per hit (`Weapon.damage`). */
  val weaponDamage: Int = 3

  /** The lance's hitbox: 6x16, centred this far along the facing (`HitBox/Shape`). */
  val weaponReach: Double = 15
  val weaponSize: Size    = Size(6, 16)

  /** The impact effect (`ImpactFx`): 4 frames of 32x32 over 0.1s, this far along the facing. */
  val impactReach: Double = 16
  val impactTime: Seconds = Seconds(0.1)

  // --- Monsters (Monster.gd, Monster.tscn, RandomMoveBehavior.gd) -------------------------------

  val monsterLife: Int      = 6
  val monsterSpeed: Double  = 40
  val monsterRadius: Double = 4 // the 10x6 body, as a circle for move-and-slide

  /** A wandering monster stops, or picks a new direction, this often (`Timer`). */
  val wanderEvery: Seconds = Seconds(1)

  /** The area that hurts the player: 12x20, centred 5 pixels above the monster's origin. */
  val monsterHurtArea: Rectangle = Rectangle(-6, -15, 12, 20)

  /** Hit and death effects (`hit_fx`, `death`): 0.2s, circular ease-out. */
  val hitFlash: Seconds = Seconds(0.2)

  // --- The player getting hurt (Player.gd, LifeBar.gd, Hud.gd) ----------------------------------

  /** Damage from touching a monster, and the push away from it (px/s). */
  val touchDamage: Int  = 1
  val pushForce: Double = 50

  /** The push loses this fraction every 60 fps frame (`linear_interpolate(ZERO, 0.1)`). */
  val pushDecay: Double = 0.1

  /** The hearts drain or refill at this many life points per second. */
  val lifeDrainRate: Double = 10

  /** At 0 life the death frame shows this long before the player revives at the start. */
  val deathTime: Seconds = Seconds(0.5)

  /** The revive fade (`Fade`): black for 0.3s, clear by 0.5s. */
  val fadeHold: Seconds = Seconds(0.3)
  val fadeOut: Seconds  = Seconds(0.5)

  // --- Tutorial (Tuto.gd, Tuto.tscn) --------------------------------------------------------------

  /** Checked this often; once the player has moved and attacked, it slides down 100px over 1s. */
  val tutorialCheck: Seconds = Seconds(0.5)
  val tutorialSlide: Seconds = Seconds(1)
