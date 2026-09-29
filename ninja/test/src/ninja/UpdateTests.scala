package ninja

import indigo.*
import ninja.Subscription.subscriptions
import ninja.Type.*
import ninja.Update.*
import ninja.common.Types.*
import ninja.scene.{TitleScene, WorldScene}

class UpdateTests extends munit.FunSuite {

  val shared: Shared = Shared(Seconds(1), Seconds(1.0 / 60), Dice.loaded(1), Size(1280, 720))
  val start: Model   = init(shared).unsafeGet

  test("the game starts on the title") {
    assertEquals(start.route, SceneRoute.Title)
    assertEquals(start.world, None)
  }

  test("starting from the title creates the world and enters it") {
    val entered = update(shared, Msg.TitleSceneMsg(TitleScene.Msg.Start), start).unsafeGet
    assertEquals(entered.route, SceneRoute.World)
    assert(entered.world.isDefined)
  }

  test("only the showing scene hears input") {
    val confirm = KeyboardEvent.KeyUp(Key.SPACE)
    assertEquals(subscriptions(start, InputState.default)(confirm), Some(Msg.TitleSceneMsg(TitleScene.Msg.Start)))
    val inWorld = update(shared, Msg.TitleSceneMsg(TitleScene.Msg.Start), start).unsafeGet
    assertEquals(subscriptions(inWorld, InputState.default)(FrameTick), Some(Msg.WorldSceneMsg(WorldScene.Msg.Tick(Vector2.zero, false, false))))
  }
}
