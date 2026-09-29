package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Combat, Layout, Village}
import ninja.generated.Assets
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
      monsters = Village.monsterStarts.map(monsterAt(now)),
      swingAt = None,
      swingHits = Set.empty,
      impacts = Batch.empty,
      push = Vector2.zero,
      hurtAt = None,
      diedAt = None,
      revivedAt = None,
      lifeShown = maxLife.toDouble,
      resetPending = false,
      tutorial = Tutorial(moved = false, attacked = false, hidingAt = None),
      enteredAt = now
    ).pipe(enterZones(now)).pipe(Outcome(_))

  /** A monster at its start, full of life, not yet awake. */
  def monsterAt(now: Seconds)(start: Vector2): Monster =
    Monster(
      start = start,
      position = start,
      velocity = Vector2.zero,
      life = Combat.monsterLife,
      awake = false,
      turnAt = now + Combat.wanderEvery,
      touching = false,
      hitAt = None,
      diedAt = None
    )

  // -----------------------------------------------------------------
  // Update
  // -----------------------------------------------------------------

  def update(shared: Shared, msg: Msg, model: Model): Outcome[Model] =
    msg match
      case Msg.Tick(move, attack) =>
        val now = shared.now
        val dt  = shared.delta.toDouble

        model
          .pipe(revive(now))
          .pipe(noteTutorial(move, attack))
          .pipe(swing(attack, now))
          .pipe(movePlayer(move, dt, now))
          .pipe(strike(now))
          .pipe(breakProps(now))
          .pipe(teleport(now))
          .pipe(moveFollower(dt, now))
          .pipe(movePig(dt, now))
          .pipe(patrol(dt, now))
          .pipe(moveMonsters(dt, now, shared.dice))
          .pipe(hurtPlayer(now))
          .pipe(followCamera(now))
          .pipe(resetScreen(now))
          .pipe(enterZones(now))
          .pipe(hideTutorial(now))
          .pipe(drainLife(dt, now))
          .pipe(soundsOf(now))

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
    * them (Godot's `move_and_slide`). While swinging it stands still, and while dead it doesn't
    * move at all; a knockback push adds to its velocity and decays (Godot 3's `Player`).
    */
  def movePlayer(move: Vector2, dt: Double, now: Seconds)(model: Model): Model =
    if model.diedAt.isDefined then model
    else
      val walked =
        if isSwinging(now)(model) then model.player.copy(velocity = Vector2.zero)
        else walk(move, Village.playerSpeed, dt, now)(model.player)
      val props = model.props.filter(_.brokenAt.isEmpty).map(propBody)
      val (position, velocity) =
        Collision.moveAndSlide(
          walked.velocity + model.push,
          dt,
          playerRadius,
          playerOffset,
          Village.solids,
          props
        )(
          walked.position
        )
      model.copy(
        player = walked.copy(position = position, velocity = velocity - model.push),
        push = model.push * Math.pow(1 - Combat.pushDecay, dt * 60)
      )

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

  // --- Combat (from the Godot 3 version) ----------------------------------

  def isSwinging(now: Seconds)(model: Model): Boolean =
    model.swingAt.exists(now < _ + Combat.swingTime)

  /** Holding attack swings the lance, again every `swingTime` while held; not while dead. */
  def swing(attack: Boolean, now: Seconds)(model: Model): Model =
    if !attack || model.diedAt.isDefined || isSwinging(now)(model) then model
    else
      model.copy(
        swingAt = Some(now),
        swingHits = Set.empty,
        player = model.player.copy(velocity = Vector2.zero)
      )

  /** The lance's hitbox (6x16 along the facing, centred 15 pixels out from the sprite's centre). */
  def weaponArea(player: Actor): Rectangle =
    val direction = directionOf(player.facing)
    val centre    = spriteCentre(player.position) + direction * Combat.weaponReach
    val size =
      if direction.x == 0 then Combat.weaponSize
      else Size(Combat.weaponSize.height, Combat.weaponSize.width)
    Rectangle(
      Math.round(centre.x - size.width / 2.0).toInt,
      Math.round(centre.y - size.height / 2.0).toInt,
      size.width,
      size.height
    )

  /** Where a hit shows its impact effect. */
  def impactPoint(player: Actor): Vector2 =
    spriteCentre(player.position) + directionOf(player.facing) * Combat.impactReach

  /** During a swing, the lance hits each monster once (3 damage; at 0 life it dies), and breaks the
    * props it reaches.
    */
  def strike(now: Seconds)(model: Model): Model =
    if !isSwinging(now)(model) then model
    else
      val area   = weaponArea(model.player)
      val impact = impactPoint(model.player)
      val struck = model.monsters.zipWithIndex
        .filter { case (m, i) =>
          isAlive(m) && !model.swingHits.contains(i) && area.overlaps(monsterHurtArea(m))
        }
        .map(_._2)
      val monsters = model.monsters.zipWithIndex.map { case (m, i) =>
        if !struck.contains(i) then m
        else
          val life = m.life - Combat.weaponDamage
          m.copy(life = life, hitAt = Some(now), diedAt = if life <= 0 then Some(now) else None)
      }
      val props = model.props.map { prop =>
        if prop.brokenAt.isEmpty && Collision.touches(area)(propBody(prop)) then
          prop.copy(brokenAt = Some(now))
        else prop
      }
      val hits = struck.length + props.zip(model.props).filter(_ != _).length
      model.copy(
        monsters = monsters,
        props = props,
        swingHits = model.swingHits ++ struck.toList,
        impacts = model.impacts.filter(now < _._2 + Combat.impactTime) ++
          (if hits > 0 then Batch((impact, now)) else Batch.empty)
      )

  /** Alive, or dying: a dead monster still counts while it shrinks away. */
  def isAlive(monster: Monster): Boolean =
    monster.diedAt.isEmpty

  def isGone(now: Seconds)(monster: Monster): Boolean =
    monster.diedAt.exists(now >= _ + Combat.hitFlash)

  def monsterHurtArea(monster: Monster): Rectangle =
    Combat.monsterHurtArea.moveBy(
      Point(Math.round(monster.position.x).toInt, Math.round(monster.position.y).toInt)
    )

  /** Monsters wake the first time they're on screen, then every `wanderEvery` either stop or set
    * off in a random direction at 40 px/s, sliding along walls. Once gone they stay put.
    */
  def moveMonsters(dt: Double, now: Seconds, dice: Dice)(model: Model): Model =
    val topLeft = viewTopLeft(now)(model.camera)
    val view    = Rectangle(topLeft, Layout.screen)
    model.copy(monsters = model.monsters.map { m =>
      if isGone(now)(m) then m
      else
        val awake = m.awake || view.contains(Point(m.position.x.toInt, m.position.y.toInt))
        val (velocity, turnAt) =
          if !awake then (Vector2.zero, now + Combat.wanderEvery)
          else if now < m.turnAt then (m.velocity, m.turnAt)
          else if m.velocity != Vector2.zero then (Vector2.zero, m.turnAt + Combat.wanderEvery)
          else
            val angle = dice.rollDouble * 2 * Math.PI
            (
              Vector2(Math.cos(angle), Math.sin(angle)) * Combat.monsterSpeed,
              m.turnAt + Combat.wanderEvery
            )
        val (position, _) =
          Collision.moveAndSlide(
            velocity,
            dt,
            Combat.monsterRadius,
            Vector2.zero,
            Village.solids,
            Batch.empty
          )(m.position)
        m.copy(position = position, velocity = velocity, awake = awake, turnAt = turnAt)
    })

  /** A monster's hurt area entering the player's body: 1 damage and a push away from it. At 0 life
    * the player dies.
    */
  def hurtPlayer(now: Seconds)(model: Model): Model =
    val body = playerBody(model.player.position)
    val touched = model.monsters.map { m =>
      val touching = !isGone(now)(m) && Collision.touches(monsterHurtArea(m))(body)
      (m.copy(touching = touching), touching && !m.touching)
    }
    val entering = touched.filter(_._2).map(_._1)
    val monsters = touched.map(_._1)
    if model.diedAt.isDefined || entering.isEmpty then model.copy(monsters = monsters)
    else
      val life = model.life - Combat.touchDamage * entering.length
      val from = entering.last.position
      model.copy(
        monsters = monsters,
        life = life.max(0),
        hurtAt = Some(now),
        diedAt = if life <= 0 then Some(now) else None,
        push = directionTo(spriteCentre(model.player.position))(from) * Combat.pushForce
      )

  /** After `deathTime` on the death frame, the player is back at the start with full life. */
  def revive(now: Seconds)(model: Model): Model =
    model.diedAt match
      case Some(at) if now >= at + Combat.deathTime =>
        model.copy(
          player = Actor.at(Village.playerStart),
          life = maxLife,
          diedAt = None,
          revivedAt = Some(now),
          push = Vector2.zero,
          swingAt = None
        )
      case _ =>
        model

  /** Once the camera has arrived on a new screen, dead monsters revive and broken props come back
    * (Godot 3's `Camera.reset_map`).
    */
  def resetScreen(now: Seconds)(model: Model): Model =
    if !model.resetPending || now < model.camera.since + Layout.cameraSlide then model
    else
      model.copy(
        monsters = model.monsters.map(m =>
          if m.diedAt.isDefined then monsterAt(now)(m.start).copy(awake = m.awake) else m
        ),
        props = model.props.map(_.copy(brokenAt = None)),
        resetPending = false
      )

  /** The hearts drain (or refill) towards the player's life at 10 life per second. */
  def drainLife(dt: Double, now: Seconds)(model: Model): Model =
    val step = Combat.lifeDrainRate * dt
    val gap  = model.life - model.lifeShown
    model.copy(lifeShown =
      if Math.abs(gap) <= step then model.life.toDouble
      else model.lifeShown + Math.signum(gap) * step
    )

  /** Sounds for what happened this frame: a hit (monster or player), or a prop breaking. */
  def soundsOf(now: Seconds)(model: Model): Outcome[Model] =
    val hit =
      model.hurtAt.contains(now) || model.monsters.exists(_.hitAt.contains(now))
    val broke = model.props.exists(_.brokenAt.contains(now))
    Outcome(model).addGlobalEvents(
      Batch(
        Option.when(hit)(PlaySound(Assets.assets.sndHit, Volume.Max)),
        Option.when(broke)(PlaySound(Assets.assets.sndGrass, Volume.Max))
      ).collect { case Some(e) => e }
    )

  // --- Tutorial -----------------------------------------------------------

  def noteTutorial(move: Vector2, attack: Boolean)(model: Model): Model =
    val t = model.tutorial
    model.copy(tutorial =
      t.copy(moved = t.moved || move != Vector2.zero, attacked = t.attacked || attack)
    )

  /** Checked every 0.5s since entering: once the player has moved and attacked, it slides away. */
  def hideTutorial(now: Seconds)(model: Model): Model =
    val t = model.tutorial
    if t.hidingAt.isDefined || !t.moved || !t.attacked then model
    else
      val every  = Combat.tutorialCheck.toDouble
      val checks = Math.ceil((now - model.enteredAt).toDouble / every)
      model.copy(tutorial = t.copy(hidingAt = Some(model.enteredAt + Seconds(checks * every))))

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
          teleportedAt = Some(now),
          resetPending = true
        )

      case _ =>
        model

  // --- Camera -------------------------------------------------------------

  /** When the player crosses into another screen, the camera slides there. */
  def followCamera(now: Seconds)(model: Model): Model =
    val cell = cellOf(model.player.position)
    if cell == model.camera.cell then model
    else
      model.copy(
        camera = CameraGrid(cell, cameraPosition(now)(model.camera), now),
        resetPending = true
      )

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
