package game.scene.worldscene

import indigo.*
import game.common.Types.*
import game.common.constant.NorthVillage
import game.scene.worldscene.Type.*
import game.scene.worldscene.Update.*
import game.scene.worldscene.common.Util.*
import game.scene.worldscene.subui.DialogueUI.wrap

import scala.util.chaining.*

class VillagerTests extends munit.FunSuite {

  val start: Seconds = Seconds(10)
  val shared: Shared = Shared(start, Seconds(1.0 / 60), Dice.loaded(1), Size(1280, 720))
  val world: Model   = init(shared).unsafeGet

  val makoto: Int     = NorthVillage.villagers.toList.indexWhere(_.name == "Makoto")
  val villager        = NorthVillage.villagers(makoto)
  def next(model: Model, at: Seconds): Model = talk(pressed = true, at)(model)

  /** The player just below Makoto, in talking range. */
  val nearMakoto: Model =
    val pose = villagerPose(start)(villager, world.villagers(makoto))
    world
      .copy(player = Actor.at(pose.position + Vector2(0, 10)))
      .pipe(approachVillagers(start))

  test("a routine eases between its position keys and steps its facing") {
    val halfway = villagerPose(start + Seconds(0.75))(villager, VillagerState(start, None, None))
    assertEqualsDouble(halfway.position.y, villager.position.y + 16, 1e-9)
    assertEquals(halfway.facing, Facing.Down)
    assert(halfway.walking)
    val back = villagerPose(start + Seconds(3))(villager, VillagerState(start, None, None))
    assertEquals(back.facing, Facing.Up)
  }

  test("in talking range, confirm starts a conversation: the villager stops and faces the player") {
    assertEquals(nearMakoto.nearVillager.map(_._1), Some(makoto))
    val talking = next(nearMakoto, start)
    assertEquals(talking.talk, Some(Talk(makoto, 0, start)))
    assertEquals(talking.villagers(makoto).pausedAt, Some(start))
    assertEquals(talking.villagers(makoto).facingPlayer, Some(Facing.Down))
  }

  test("confirm finishes typing a line, then moves on; after the last line the routine resumes") {
    val talking = next(nearMakoto, start)
    val typed   = next(talking, start + Seconds(0.1))
    assertEquals(typed.talk.map(_.line), Some(0))
    assert(isTyped(typed.talk.get, start + Seconds(0.1)))
    val lines = villager.lines.length
    val done = (1 to lines).foldLeft(typed)((m, i) => next(m, start + Seconds(10.0 * i)))
    assertEquals(done.talk, None)
    assertEquals(done.villagers(makoto).pausedAt, None)
    assertEquals(done.villagers(makoto).shift, start + Seconds(10.0 * lines) - start + start)
  }

  test("lines wrap at word boundaries") {
    assertEquals(wrap("one two three"), Batch("one two three"))
    assert(wrap(villager.lines(1)).forall(_.length <= 40))
  }
}
