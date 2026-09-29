package ninja.scene.worldscene.common

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.Layout
import ninja.scene.worldscene.Type.*

/** Helpers shared by the world scene's update and UI. */
object Util:

  /** The player's collision body (Godot: a circle of radius 7, 1 pixel above its feet). */
  val playerRadius: Double  = 7
  val playerOffset: Vector2 = Vector2(0, -1)

  def playerBody(feet: Vector2): BoundingCircle =
    BoundingCircle(Vertex.fromVector2(feet + playerOffset), playerRadius)

  /** The centre of a character's sprite, 6 pixels above its feet (Godot 3's character origin). */
  def spriteCentre(feet: Vector2): Vector2 =
    feet + Vector2(0, -6)

  /** Where a villager is and what it's doing at `now`, from its routine (Godot 3's `Idle`
    * animation; positions eased linearly, everything else stepped), paused while it talks.
    */
  final case class VillagerPose(
      position: Vector2,
      facing: Facing,
      walking: Boolean,
      flip: Boolean,
      clock: Double
  )

  def villagerPose(now: Seconds)(villager: Villager, state: VillagerState): VillagerPose =
    val clock = (state.pausedAt.getOrElse(now) - state.shift).toDouble
    villager.routine match
      case None =>
        VillagerPose(
          villager.position,
          state.facingPlayer.getOrElse(villager.facing),
          false,
          false,
          clock
        )
      case Some(r) =>
        val t = ((clock % r.length) + r.length) % r.length
        def stepped[A](keys: Batch[(Double, A)], default: A): A =
          keys
            .filter(_._1 <= t)
            .lastOption
            .map(_._2)
            .orElse(keys.lastOption.map(_._2))
            .getOrElse(default)
        val position =
          if r.positions.isEmpty then villager.position
          else
            val keys  = r.positions.toList
            val after = keys.indexWhere(_._1 > t)
            if after <= 0 then keys.last._2 // before the first key or after the last: wrap-around
            else
              val (t0, p0) = keys(after - 1)
              val (t1, p1) = keys(after)
              p0 + (p1 - p0) * ((t - t0) / (t1 - t0))
        VillagerPose(
          position,
          state.facingPlayer.getOrElse(stepped(r.facings, villager.facing)),
          state.pausedAt.isEmpty && stepped(r.walking, false),
          stepped(r.flips, false),
          clock
        )

  /** Godot 3's talk area: a circle of radius 12.8 on the villager. */
  def talkArea(villager: Villager, pose: VillagerPose): BoundingCircle =
    BoundingCircle(Vertex.fromVector2(pose.position + villager.talkOffset), 12.8204)

  /** A villager's body (Godot 3: a 9.5x6 box 2 pixels up), as a circle the player can't walk
    * through.
    */
  def villagerBody(pose: VillagerPose): BoundingCircle =
    BoundingCircle(Vertex.fromVector2(pose.position + Vector2(0, -2)), 5)

  /** Godot 3's `Npc.get_target_direction`: face left or right if the player is more than 8 pixels
    * to the side, else up or down.
    */
  def facingTowards(target: Vector2)(from: Vector2): Facing =
    val dx = from.x - target.x
    val dy = from.y - target.y
    if Math.abs(dx) > 8 then if dx < 0 then Facing.Right else Facing.Left
    else if dy < 0 then Facing.Down
    else Facing.Up

  /** The grid cell showing a world position (Godot's `CameraGrid.world_to_grid`). */
  def cellOf(position: Vector2): Point =
    Point(
      Math.round((position.x - Layout.cameraOffset.x) / Layout.cameraGrid.width).toInt,
      Math.round((position.y - Layout.cameraOffset.y) / Layout.cameraGrid.height).toInt
    )

  def cellPosition(cell: Point): Vector2 =
    Vector2(cell.x * Layout.cameraGrid.width, cell.y * Layout.cameraGrid.height)

  /** Where the camera is, sliding between cells with a sine ease in-out. */
  def cameraPosition(now: Seconds)(camera: CameraGrid): Vector2 =
    val target   = cellPosition(camera.cell)
    val progress = (now - camera.since).toDouble / Layout.cameraSlide.toDouble
    if progress >= 1 then target
    else camera.from + (target - camera.from) * ((1 - Math.cos(Math.PI * progress.max(0))) / 2)

  /** Top-left of the screen, in world pixels. */
  def viewTopLeft(now: Seconds)(camera: CameraGrid): Point =
    val centre = cameraPosition(now)(camera) + Vector2(Layout.cameraOffset.x, Layout.cameraOffset.y)
    Point(
      Math.round(centre.x - Layout.screen.width / 2.0).toInt,
      Math.round(centre.y - Layout.screen.height / 2.0).toInt
    )
