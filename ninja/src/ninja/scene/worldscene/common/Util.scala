package ninja.scene.worldscene.common

import indigo.*
import ninja.common.constant.Layout
import ninja.scene.worldscene.Type.*

/** Helpers shared by the world scene's update and UI. */
object Util:

  /** The player's collision body (Godot: a circle of radius 7, 1 pixel above its feet). */
  val playerRadius: Double  = 7
  val playerOffset: Vector2 = Vector2(0, -1)

  def playerBody(feet: Vector2): BoundingCircle =
    BoundingCircle(Vertex.fromVector2(feet + playerOffset), playerRadius)

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
