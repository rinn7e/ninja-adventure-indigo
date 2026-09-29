package ninja.common.types

/** Which scene is showing: the game's `AppRoute` (a TEA web app routes between pages; the game
  * routes between scenes). The scenes themselves are the modules in `scene/`: `TitleScene`,
  * `WorldScene`.
  */
enum SceneRoute derives CanEqual:
  case Title, World
