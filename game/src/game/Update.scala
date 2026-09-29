package game

import indigo.*
import game.Type.*
import game.common.Types.*
import game.scene.{TitleScene, WorldScene}

object Update:

  // -----------------------------------------------------------------
  // Init
  // -----------------------------------------------------------------

  /** Like a TEA root `init`: starts on the title, with the title's entry effects. */
  def init(shared: Shared): Outcome[Model] =
    TitleScene.init(shared).map(title => Model(SceneRoute.Title, title, None))

  // -----------------------------------------------------------------
  // Update
  // -----------------------------------------------------------------

  /** Delegates each message to its scene, then intercepts the child messages the root cares about
    * (TEA child msg interception; `flatMap` is `updateAndCmd`).
    */
  def update(shared: Shared, msg: Msg, model: Model): Outcome[Model] =
    msg match
      case Msg.TitleSceneMsg(subMsg) =>
        TitleScene
          .update(shared, subMsg, model.title)
          .map(title => model.copy(title = title))
          .flatMap { m =>
            subMsg match
              case TitleScene.Msg.Start => startGame(shared, m)
          }

      case Msg.WorldSceneMsg(subMsg) =>
        model.world match
          case None =>
            Outcome(model)

          case Some(world) =>
            WorldScene
              .update(shared, subMsg, world)
              .map(world => model.copy(world = Some(world)))

  /** A new game: a fresh world. */
  private def startGame(shared: Shared, model: Model): Outcome[Model] =
    WorldScene
      .init(shared)
      .map(world => model.copy(route = SceneRoute.World, world = Some(world)))
