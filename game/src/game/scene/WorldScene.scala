package game.scene

/** The world scene as one module. Re-exports `worldscene/`, so the parent writes
  * `import game.scene.WorldScene` and `WorldScene.update`, `WorldScene.Msg`, `WorldScene.ui` (~
  * Haskell's `import qualified Ninja.Scene.WorldScene as WorldScene`).
  */
object WorldScene:
  export worldscene.Type.*
  export worldscene.Update.*
  export worldscene.Subscription.*
  export worldscene.UI.*
