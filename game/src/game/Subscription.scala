package game

import indigo.*
import game.Type.*
import game.common.Types.SceneRoute
import game.scene.{TitleScene, WorldScene}

object Subscription:

  /** Only the showing scene listens to input; its messages are wrapped as root messages. */
  def subscriptions(model: Model, input: InputState): GlobalEvent => Option[Msg] =
    e =>
      model.route match
        case SceneRoute.Title =>
          TitleScene.subscriptions(model.title, input)(e).map(Msg.TitleSceneMsg(_))

        case SceneRoute.World =>
          model.world.flatMap(WorldScene.subscriptions(_, input)(e)).map(Msg.WorldSceneMsg(_))
