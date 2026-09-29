package ninja

import indigo.*
import ninja.Type.*
import ninja.common.Types.SceneRoute
import ninja.scene.{TitleScene, WorldScene}

object Subscription:

  /** Only the showing scene listens to input; its messages are wrapped as root messages. */
  def subscriptions(model: Model, keyboard: Keyboard): GlobalEvent => Option[Msg] =
    e =>
      model.route match
        case SceneRoute.Title =>
          TitleScene.subscriptions(model.title, keyboard)(e).map(Msg.TitleSceneMsg(_))

        case SceneRoute.World =>
          model.world.flatMap(WorldScene.subscriptions(_, keyboard)(e)).map(Msg.WorldSceneMsg(_))
