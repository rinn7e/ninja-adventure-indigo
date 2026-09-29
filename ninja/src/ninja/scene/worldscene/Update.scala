package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Layout, Village}
import ninja.common.util.Collision
import ninja.common.util.Motion.*
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.common.Util.*

import scala.util.chaining.*

object Update:

  /** Godot's `Character` acceleration and deceleration, in pixels per second squared. */
  val acceleration: Double = 1000
  val deceleration: Double = 800

  /** The pig's acceleration (Godot's `Animal`), in pixels per frame, per second. */
  val pigAcceleration: Double = 5

  /** How close followers stay to their target (Godot's `BehaviorFollow`). */
  val followMin: Double = 8
  val followMax: Double = 24

  /** The player's damage area (radius 16) reaching a prop's hitbox (radius ~12.2). */
  val breakReach: Double = 16 + 12.1655

  /** A teleporter ignores the player for this long after it arrives. */
  val teleportCooldown: Seconds = Seconds(0.1)

  val maxLife: Int = 12

  // -----------------------------------------------------------------
  // Init
  // -----------------------------------------------------------------

  /** Entering the world for a new game: everyone at their start, the camera on the player's screen,
    * and the environment of the zone the player starts in (its music starts at once).
    */
  def init(shared: Shared): Outcome[Model] =
    val now = shared.now
    Model(
      player = Actor.at(Village.playerStart),
      follower = Actor.at(Village.followerStart),
      pig = Animal(Village.pigStart, Vector2.zero, flip = false, moving = false, now),
      patroller = Patroller(Actor.at(Village.patrollerStart), 0, 1, None),
      props = Village.props,
      camera = CameraGrid(cellOf(Village.playerStart), Vector2.zero, now - Layout.cameraSlide),
      teleportedAt = None,
      zone = None,
      environment = Environment.calm,
      environmentSince = now,
      previousGrading = Grading.Initial,
      music = None,
      previousMusic = None,
      musicSince = now,
      life = maxLife,
      enteredAt = now
    ).pipe(enterZones(now)).pipe(Outcome(_))

  // -----------------------------------------------------------------
  // Update
  // -----------------------------------------------------------------

  def update(shared: Shared, msg: Msg, model: Model): Outcome[Model] =
    msg match
      case Msg.Tick(move) =>
        val now = shared.now
        val dt  = shared.delta.toDouble

        model
          .pipe(movePlayer(move, dt, now))
          .pipe(breakProps(now))
          .pipe(teleport(now))
          .pipe(moveFollower(dt, now))
          .pipe(movePig(dt, now))
          .pipe(patrol(dt, now))
          .pipe(followCamera(now))
          .pipe(enterZones(now))
          .pipe(Outcome(_))

  // --- Characters ---------------------------------------------------------

  /** Godot's `Character._physics_process`: speed up towards `move * speed`, or slow down to a stop;
    * face the way it's pushed; restart the animation when it starts or stops walking.
    */
  def walk(move: Vector2, speed: Double, dt: Double, now: Seconds)(actor: Actor): Actor =
    val moving = move != Vector2.zero
    val velocity =
      if moving then moveToward(move * speed, acceleration * dt)(actor.velocity)
      else moveToward(Vector2.zero, deceleration * dt)(actor.velocity)
    actor.copy(
      velocity = velocity,
      facing = if moving then facingOf(move) else actor.facing,
      moving = moving,
      animSince = if moving != actor.moving then now else actor.animSince
    )

  /** Moves without collisions (the NPCs have no collision shape in the Godot project). */
  def glide(dt: Double)(actor: Actor): Actor =
    actor.copy(position = actor.position + actor.velocity * dt)

  /** Godot's `BehaviorFollow.go_to_pos`: closer when too far, away when too close. */
  def followVector(target: Vector2)(from: Vector2): Vector2 =
    val distance = from.distanceTo(target)
    if distance > followMax then directionTo(target)(from)
    else if distance < followMin then directionTo(from)(target)
    else Vector2.zero

  /** The player collides with the walls' polygons and with props that aren't broken, sliding along
    * them (Godot's `move_and_slide`).
    */
  def movePlayer(move: Vector2, dt: Double, now: Seconds)(model: Model): Model =
    val walked = walk(move, Village.playerSpeed, dt, now)(model.player)
    val props  = model.props.filter(_.brokenAt.isEmpty).map(propBody)
    val (position, velocity) =
      Collision.moveAndSlide(
        walked.velocity,
        dt,
        playerRadius,
        playerOffset,
        Village.solids,
        props
      )(
        walked.position
      )
    model.copy(player = walked.copy(position = position, velocity = velocity))

  /** A prop's physical body (Godot: a circle of radius 8). */
  def propBody(prop: Prop): BoundingCircle =
    BoundingCircle(Vertex.fromVector2(prop.position), 8)

  def moveFollower(dt: Double, now: Seconds)(model: Model): Model =
    val move = followVector(model.player.position)(model.follower.position)
    model.copy(follower =
      walk(move, Village.followerSpeed, dt, now)(model.follower).pipe(glide(dt))
    )

  /** Godot's `Animal._process`: velocity (per frame) eases towards `move * speed * dt`. */
  def movePig(dt: Double, now: Seconds)(model: Model): Model =
    val pig  = model.pig
    val move = followVector(model.follower.position)(pig.position + Village.pigFollowOffset)
    val velocity =
      moveToward(move * (Village.pigSpeed * dt), pigAcceleration * dt)(pig.velocity)
    val moving = velocity != Vector2.zero
    model.copy(pig =
      pig.copy(
        position = pig.position + velocity,
        velocity = velocity,
        flip = if move.x != 0 then move.x > 0 else pig.flip,
        moving = moving,
        animSince = if moving != pig.moving then now else pig.animSince
      )
    )

  /** Godot's `BehaviorFollowPath` (not looping, waiting at the ends): walk to the target point; at
    * an end, wait, then turn around.
    */
  def patrol(dt: Double, now: Seconds)(model: Model): Model =
    val p    = model.patroller
    val path = Village.patrolPath
    val last = path.length - 1

    def advance(p: Patroller): Patroller =
      val direction = if p.target == 0 then 1 else if p.target == last then -1 else p.direction
      p.copy(target = p.target + direction, direction = direction, waitingUntil = None)

    val next =
      p.waitingUntil match
        case Some(until) if now < until =>
          p.copy(actor = walk(Vector2.zero, Village.patrollerSpeed, dt, now)(p.actor))

        case Some(_) =>
          advance(p)

        case None =>
          val target = path(p.target)
          if p.actor.position.distanceTo(target) >= Village.patrolPrecision then
            p.copy(actor =
              walk(directionTo(target)(p.actor.position), Village.patrollerSpeed, dt, now)(p.actor)
            )
          else if p.target == 0 || p.target == last then
            p.copy(
              actor = walk(Vector2.zero, Village.patrollerSpeed, dt, now)(p.actor),
              waitingUntil = Some(now + Village.patrolWait)
            )
          else advance(p)

    model.copy(patroller = next.copy(actor = glide(dt)(next.actor)))

  // --- Props --------------------------------------------------------------

  /** The player's damage area breaks any prop it reaches (props have one life). */
  def breakProps(now: Seconds)(model: Model): Model =
    model.copy(props = model.props.map { prop =>
      if prop.brokenAt.isEmpty && prop.position.distanceTo(model.player.position) < breakReach then
        prop.copy(brokenAt = Some(now))
      else prop
    })

  // --- Teleporters --------------------------------------------------------

  /** Stepping into a teleporter sends the player (and its followers, who jump to their targets) to
    * the linked one, and snaps the camera there. The UI blacks out and fades back in.
    */
  def teleport(now: Seconds)(model: Model): Model =
    val coolingDown = model.teleportedAt.exists(now < _ + teleportCooldown)
    val body        = playerBody(model.player.position)

    Village.teleporters.find(t => Collision.touches(t.area)(body)) match
      case Some(from) if !coolingDown =>
        val to       = Village.teleporters(from.target)
        val position = to.position + (model.player.position - from.position) + to.direction * 25
        val cell     = cellOf(position)
        model.copy(
          player = model.player.copy(position = position),
          follower = model.follower.copy(position = position),
          pig = model.pig.copy(position = position),
          camera = CameraGrid(cell, cellPosition(cell), now - Layout.cameraSlide),
          teleportedAt = Some(now)
        )

      case _ =>
        model

  // --- Camera -------------------------------------------------------------

  /** When the player crosses into another screen, the camera slides there. */
  def followCamera(now: Seconds)(model: Model): Model =
    val cell = cellOf(model.player.position)
    if cell == model.camera.cell then model
    else model.copy(camera = CameraGrid(cell, cameraPosition(now)(model.camera), now))

  // --- Environment --------------------------------------------------------

  /** Walking into another zone changes the weather, the grading (fading from the old one, Godot's
    * `color_correction.gd`), and the music if it's different: the old music fades out, then the new
    * one plays (Godot's `music.gd`).
    */
  def enterZones(now: Seconds)(model: Model): Model =
    val body = playerBody(model.player.position)
    val zone = Village.zones.toList.indexWhere(z => Collision.touches(z.area)(body))

    if zone < 0 || model.zone.contains(zone) then model
    else
      val environment = Village.zones(zone).environment
      val changed = model.copy(
        zone = Some(zone),
        environment = environment,
        environmentSince = now,
        previousGrading = model.environment.grading
      )
      if environment.music == model.music then changed
      else changed.copy(music = environment.music, previousMusic = model.music, musicSince = now)
