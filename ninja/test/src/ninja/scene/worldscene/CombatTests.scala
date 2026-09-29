package ninja.scene.worldscene

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Combat, Layout, Village}
import ninja.scene.worldscene.Type.*
import ninja.scene.worldscene.Update.*

import scala.util.chaining.*

class CombatTests extends munit.FunSuite {

  val start: Seconds = Seconds(10)
  val shared: Shared = Shared(start, Seconds(1.0 / 60), Dice.loaded(1), Size(1280, 720))
  val world: Model   = init(shared).unsafeGet

  /** The player at `at`, facing right, and one monster `gap` pixels to its right. */
  def facingMonster(gap: Double, at: Vector2 = Vector2(344, -140)): Model =
    val monster = monsterAt(start)(at + Vector2(gap, 6))
    world.copy(
      player = Actor.at(at).copy(facing = Facing.Right),
      monsters = Batch(monster)
    )

  test("the lance's hitbox is 6x16 along the facing, 15 pixels out from the sprite's centre") {
    val down  = weaponArea(Actor.at(Vector2(100, 100)))
    val right = weaponArea(Actor.at(Vector2(100, 100)).copy(facing = Facing.Right))
    assertEquals(down, Rectangle(97, 101, 6, 16))
    assertEquals(right, Rectangle(107, 91, 16, 6))
  }

  test("a swing hits a monster in reach once for 3 damage; two swings kill it") {
    val model  = facingMonster(15)
    val swung  = swing(attack = true, start)(model).pipe(strike(start))
    assertEquals(swung.monsters(0).life, Combat.monsterLife - 3)
    val same   = strike(start + Seconds(0.1))(swung)
    assertEquals(same.monsters(0).life, Combat.monsterLife - 3, "each swing hits once")
    val second = swing(attack = true, start + Seconds(0.2))(same).pipe(strike(start + Seconds(0.2)))
    assertEquals(second.monsters(0).life, 0)
    assert(second.monsters(0).diedAt.isDefined)
    assert(second.impacts.nonEmpty)
  }

  test("a swing blocks walking and further swings for 0.2s") {
    val swung = swing(attack = true, start)(world)
    assert(isSwinging(start + Seconds(0.1))(swung))
    assertEquals(swing(attack = true, start + Seconds(0.1))(swung).swingAt, Some(start))
    val walked = movePlayer(Vector2(1, 0), 1.0 / 60, start + Seconds(0.1))(swung)
    assertEquals(walked.player.position, swung.player.position)
  }

  test("a monster touching the player hurts it once, pushing it away") {
    val model = facingMonster(0)
    val hurt  = hurtPlayer(start)(model)
    assertEquals(hurt.life, maxLife - Combat.touchDamage)
    assert(hurt.push.length > 49 && hurt.push.length < 51)
    assertEquals(hurtPlayer(start + Seconds(0.1))(hurt).life, hurt.life, "only on entering")
  }

  test("at 0 life the player dies, then revives at the start with full life") {
    val model = facingMonster(0).copy(life = 1)
    val dead  = hurtPlayer(start)(model)
    assertEquals(dead.diedAt, Some(start))
    assertEquals(revive(start + Seconds(0.4))(dead).diedAt, Some(start))
    val revived = revive(start + Combat.deathTime)(dead)
    assertEquals(revived.life, maxLife)
    assertEquals(revived.player.position, Village.playerStart)
    assertEquals(revived.diedAt, None)
  }

  test("the hearts drain towards the life at 10 per second") {
    val model = world.copy(life = 8)
    assertEqualsDouble(drainLife(0.1, start)(model).lifeShown, maxLife - 1.0, 1e-9)
    assertEqualsDouble(drainLife(1, start)(model).lifeShown, 8.0, 1e-9)
  }

  test("once the camera reaches a new screen, dead monsters and broken props come back") {
    val dead = world.monsters.map(_.copy(life = 0, diedAt = Some(start)))
    val broken = world.props.map(_.copy(brokenAt = Some(start)))
    val moved = world.copy(
      monsters = dead,
      props = broken,
      camera = world.camera.copy(since = start),
      resetPending = true
    )
    assertEquals(resetScreen(start + Seconds(0.5))(moved), moved, "still sliding")
    val reset = resetScreen(start + Layout.cameraSlide)(moved)
    assert(reset.monsters.forall(m => m.diedAt.isEmpty && m.life == Combat.monsterLife))
    assert(reset.props.forall(_.brokenAt.isEmpty))
    assert(!reset.resetPending)
  }

  test("the tutorial hides at the next half-second check once the player moved and attacked") {
    val moved = noteTutorial(Vector2(1, 0), attack = false)(world)
    assertEquals(hideTutorial(start + Seconds(1))(moved).tutorial.hidingAt, None)
    val both = noteTutorial(Vector2.zero, attack = true)(moved)
    assertEquals(hideTutorial(start + Seconds(1.2))(both).tutorial.hidingAt, Some(start + Seconds(1.5)))
  }

  test("monsters stay still until first on screen") {
    val asleep = moveMonsters(1.0 / 60, start + Seconds(2), Dice.loaded(1))(world)
    assert(asleep.monsters.forall(m => !m.awake && m.position == m.start))
  }
}
