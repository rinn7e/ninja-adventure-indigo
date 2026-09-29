package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*

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
      // The environment zone the player last walked into, and its music and weather.
      zone: Option[Int],
      environment: Environment,
      environmentSince: Seconds,
      // The music playing, and the one fading out since `musicSince`.
      music: Option[Music],
      previousMusic: Option[Music],
      musicSince: Seconds,
      life: Int,
      enteredAt: Seconds
  ) derives CanEqual

  enum Msg derives CanEqual:
    /** Every frame, with the direction the player is pushing. */
    case Tick(move: Vector2)
