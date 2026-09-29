package game.common

/** Re-exports every module in `types/`, so consumers write `import game.common.Types.*` (~ a
  * Haskell `module Ninja.Common.Types` re-exporting `Ninja.Common.Types.*`).
  */
object Types:
  export types.Ambience.*
  export types.Entity.*
  export types.Tiles.*
  export types.{Actor, Facing, SceneRoute, Shared}
