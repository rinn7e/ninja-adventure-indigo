package game

import game.common.Types.SceneRoute
import game.scene.{TitleScene, WorldScene}

object Type:

  final case class Model(
      route: SceneRoute,
      title: TitleScene.Model,
      // Created when the game starts.
      world: Option[WorldScene.Model]
  ) derives CanEqual

  /** Root messages: each wraps a scene's own `Msg`, like the page messages of a TEA root. */
  enum Msg derives CanEqual:
    case TitleSceneMsg(subMsg: TitleScene.Msg)
    case WorldSceneMsg(subMsg: WorldScene.Msg)
