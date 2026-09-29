package game.scene.worldscene

import indigo.*
import game.common.Types.*

object Type:

  /** SamuraiBlue walking its path (Godot's `BehaviorFollowPath`): the point it heads for, which way
    * along the path it goes, and until when it waits at an end.
    */
  final case class Patroller(
      actor: Actor,
      target: Int,
      direction: Int,
      waitingUntil: Option[Seconds]
  ) derives CanEqual

  /** The pig (Godot's `Animal`). As in the Godot script, its velocity is in pixels per frame. */
  final case class Animal(
      position: Vector2,
      velocity: Vector2,
      flip: Boolean,
      moving: Boolean,
      animSince: Seconds
  ) derives CanEqual

  /** A wandering monster (Godot 3's `Monster` with `RandomMoveBehavior`). It starts wandering once
    * it's first on screen (`awake`), and `turnAt` is when it next stops or picks a direction.
    * `touching` is whether its hurt area overlaps the player (it hurts on entering). Once dead it
    * shrinks away, then stays gone until the screen resets.
    */
  final case class Monster(
      start: Vector2,
      position: Vector2,
      velocity: Vector2,
      life: Int,
      awake: Boolean,
      turnAt: Seconds,
      touching: Boolean,
      hitAt: Option[Seconds],
      diedAt: Option[Seconds]
  ) derives CanEqual

  /** A villager's routine clock: its routine runs on `now - shift`, and stops at `pausedAt` while
    * it talks (facing the player, `facingPlayer`).
    */
  final case class VillagerState(
      shift: Seconds,
      pausedAt: Option[Seconds],
      facingPlayer: Option[Facing]
  ) derives CanEqual

  /** A conversation: which villager, which of its lines, and when that line started typing. */
  final case class Talk(villager: Int, line: Int, since: Seconds) derives CanEqual

  /** The tutorial banner (Godot 3's `Tuto`): hidden once the player has moved and attacked. */
  final case class Tutorial(moved: Boolean, attacked: Boolean, hidingAt: Option[Seconds])
      derives CanEqual

  /** The screen-by-screen camera (Godot's `CameraGrid`): the grid cell it shows, sliding there from
    * `from` since `since`.
    */
  final case class CameraGrid(cell: Point, from: Vector2, since: Seconds) derives CanEqual

  final case class Model(
      player: Actor,
      follower: Actor, // SamuraiGreen
      pig: Animal,
      patroller: Patroller, // SamuraiBlue
      props: Batch[Prop],
      camera: CameraGrid,
      // When the player last went through a teleporter: drives the screen transition.
      teleportedAt: Option[Seconds],
      // The environment zone the player last walked into, and its music, weather and grading (which
      // fades in from `previousGrading` since `environmentSince`).
      zone: Option[Int],
      environment: Environment,
      environmentSince: Seconds,
      previousGrading: Grading,
      // The music playing, and the one fading out since `musicSince`.
      music: Option[Music],
      previousMusic: Option[Music],
      musicSince: Seconds,
      life: Int,
      // --- Combat (from the Godot 3 version) ---
      monsters: Batch[Monster],
      // When the current lance swing started, and the monsters it has already hit.
      swingAt: Option[Seconds],
      swingHits: Set[Int],
      // Where hits landed, for the impact effect.
      impacts: Batch[(Vector2, Seconds)],
      // Knockback velocity, decaying.
      push: Vector2,
      hurtAt: Option[Seconds],
      diedAt: Option[Seconds],
      revivedAt: Option[Seconds],
      // The life the hearts show, draining towards `life`.
      lifeShown: Double,
      // Set when the camera moves to another screen: once it arrives, monsters and props reset.
      resetPending: Boolean,
      tutorial: Tutorial,
      // Villagers (from the Godot 3 version): their routine clocks, the conversation, the one in
      // talking range (and since when, for the speech bubble's pop), and whether confirm is held.
      villagers: Batch[VillagerState],
      talk: Option[Talk],
      nearVillager: Option[(Int, Seconds)],
      confirmHeld: Boolean,
      debug: Boolean,
      enteredAt: Seconds
  ) derives CanEqual

  enum Msg derives CanEqual:
    /** Every frame, with the direction the player is pushing, whether attack is held, and whether
      * confirm (talk / next line) is held.
      */
    case Tick(move: Vector2, attack: Boolean, confirm: Boolean)

    /** F3: show or hide the debug overlay (the player's world position). */
    case ToggleDebug
