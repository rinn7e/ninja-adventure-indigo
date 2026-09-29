package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Layout, Village}
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.Update.*
import ninja.scene.worldscene.common.Util.*

class UpdateTests extends munit.FunSuite {

  val start: Seconds = Seconds(10)
  val shared: Shared = Shared(start, Seconds(1.0 / 60), Dice.loaded(1), Size(1280, 720))
  val world: Model   = init(shared).unsafeGet

  def tick(move: Vector2, at: Seconds)(model: Model): Model =
    update(shared.copy(now = at), Msg.Tick(move, false, false), model).unsafeGet

  def ticks(n: Int, move: Vector2)(model: Model): Model =
    (1 to n).foldLeft(model)((m, i) => tick(move, start + Seconds(i / 60.0))(m))

  test("the game starts in the player's zone, with its music and weather") {
    assertEquals(world.zone, Some(2))
    assertEquals(world.music, Some(Music.Dream))
    assertEquals(world.environment.meteo, Set(Meteo.Ray, Meteo.Fog))
    assertEquals(world.life, maxLife)
  }

  test("walking speeds up at 1000 px/s² towards 100 px/s, and slows down at 800 px/s²") {
    val a = walk(Vector2(1, 0), 100, 0.05, start)(Actor.at(Vector2.zero))
    assertEqualsDouble(a.velocity.x, 50, 1e-9)
    assertEquals(a.facing, Facing.Right)
    assert(a.moving)
    val b = walk(Vector2.zero, 100, 0.05, start)(a)
    assertEqualsDouble(b.velocity.x, 10, 1e-9)
    assertEquals(b.facing, Facing.Right, "keeps facing the last direction")
  }

  test("the player walks and the camera stays on its screen") {
    val moved = ticks(30, Vector2(1, 0))(world)
    assert(moved.player.position.x > world.player.position.x + 20)
    assertEquals(moved.camera.cell, world.camera.cell)
  }

  test("followers close in when too far, back off when too close, and rest in between") {
    assertEquals(followVector(Vector2(100, 0))(Vector2.zero), Vector2(1, 0))
    assertEquals(followVector(Vector2(4, 0))(Vector2.zero), Vector2(-1, 0))
    assertEquals(followVector(Vector2(16, 0))(Vector2.zero), Vector2.zero)
  }

  test("the patroller walks to the end of its path, waits 3 seconds, then turns around") {
    val atEnd = world.copy(patroller = world.patroller.copy(actor = Actor.at(Village.patrolPath(0))))
    val waiting = tick(Vector2.zero, start)(atEnd)
    assertEquals(waiting.patroller.waitingUntil, Some(start + Village.patrolWait))
    val turned = tick(Vector2.zero, start + Seconds(3.1))(waiting)
    assertEquals(turned.patroller.target, 1)
    assertEquals(turned.patroller.waitingUntil, None)
  }

  test("props within reach of the player break; others don't") {
    val prop   = Village.props.head
    val near   = world.copy(player = world.player.copy(position = prop.position + Vector2(20, 0)))
    val broken = breakProps(start)(near)
    assertEquals(broken.props.head.brokenAt, Some(start))
    assert(broken.props.filter(_.brokenAt.isDefined).forall(_.position.distanceTo(near.player.position) < breakReach))
    assertEquals(breakProps(start)(world).props.filter(_.brokenAt.isDefined).length, 0)
  }

  test("a teleporter sends the player and its followers to the other one, and snaps the camera") {
    val from    = Village.teleporters(0)
    val to      = Village.teleporters(1)
    val onPad   = world.copy(player = world.player.copy(position = from.position + Vector2(0, 7)))
    val arrived = teleport(start)(onPad)
    val landing = to.position + Vector2(0, 7) + to.direction * 25
    assertEquals(arrived.player.position, landing)
    assertEquals(arrived.follower.position, landing)
    assertEquals(arrived.teleportedAt, Some(start))
    assertEquals(arrived.camera.cell, cellOf(landing))
    assertEquals(cameraPosition(start)(arrived.camera), cellPosition(cellOf(landing)))
  }

  test("crossing into another screen makes the camera slide there over 0.8 s") {
    val below   = world.copy(player = world.player.copy(position = Vector2(64, 200)))
    val sliding = followCamera(start)(below)
    assertEquals(sliding.camera.cell, Point(0, 1))
    assertEquals(cameraPosition(start)(sliding.camera), cellPosition(world.camera.cell))
    assertEquals(cameraPosition(start + Layout.cameraSlide)(sliding.camera), cellPosition(Point(0, 1)))
  }

  test("walking into the swamp changes the weather, and fades out the old music first") {
    val swamp   = world.copy(player = world.player.copy(position = Vector2(-296, -64)))
    val entered = enterZones(start)(swamp)
    assertEquals(entered.zone, Some(0))
    assertEquals(entered.music, Some(Music.Swamp))
    assertEquals(entered.previousMusic, Some(Music.Dream))
    assert(entered.environment.meteo.contains(Meteo.Rain))
  }

  test("a zone with the same music doesn't restart it") {
    val north   = world.copy(player = world.player.copy(position = Vector2(24, -152)))
    val entered = enterZones(start)(north)
    assertEquals(entered.zone, Some(3))
    assertEquals(entered.musicSince, world.musicSince)
  }
}
