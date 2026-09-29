package game

import indigo.*
import indigoextras.subsystems.FPSCounter
import game.common.asset.GameAssets
import game.common.constant.{Layers, Layout}
import game.common.util.Screen
import game.Subscription.subscriptions
import game.Type.*
import game.common.Types.{SceneRoute, Shared}
import game.scene.{TitleScene, WorldScene}
import game.ui.ColorGradingUI
import game.generated.{Assets, BuildConfig, NormalFont}
import tyrian.*

import scala.scalajs.js.annotation.*

/** The whole root program as one module (~ Haskell's `module Main`): the entry point the HTML page
  * calls (`IndigoGame.launch(...)`), the Indigo `Program`, and the root UI.
  *
  * The entry point: Indigo games run inside a Tyrian runtime, which can host a wrapper app around
  * the game; we don't need one, so its model is `Unit`.
  */
@JSExportTopLevel("IndigoGame")
object Main extends BasicGameRuntime[Unit]:

  def game: Game[?, ?, ?] =
    Program()

  def settings: Settings =
    Settings.default

  def init(flags: Map[String, String]): Result[Unit] =
    Result(())

  def update(model: Unit): GlobalMsg => Result[Unit] =
    case _ => Result(model)

  def eventMapping: PartialIso[GlobalMsg, GlobalEvent] =
    PartialIso.none

  // -----------------------------------------------------------------
  // Program
  // -----------------------------------------------------------------

  /** The Indigo program: wires the root `init`, `update`, `mainUI` and `subscriptions`. `Update` is
    * qualified here, since `init` / `update` alone are this runtime's own methods.
    */
  final class Program() extends Game[Unit, Shared, Model]:

    val gameId: GameId = GameId("ninja-adventure")

    // Scenes are routed by the root program itself (see `SceneRoute`), not by Indigo's scene manager, so
    // it gets a single empty scene, as in Indigo's own sandboxes.
    def initialScene(bootData: Unit): Option[SceneName] =
      None

    def scenes(bootData: Unit): NonEmptyBatch[Scene[Model]] =
      NonEmptyBatch(Scene.empty)

    val eventFilters: EventFilters =
      EventFilters.Permissive

    def boot(flags: Map[String, String]): Outcome[BootResult[Unit, Model]] =
      Outcome(
        BootResult(BuildConfig.config, ())
          .withAssets(GameAssets.assets(flags.getOrElse("baseUrl", "")))
          .withFonts(GameAssets.fontInfo)
          .withShaders(ColorGradingUI.shader)
          .withSubSystems(
            FPSCounter
              .tint(Layers.fps, NormalFont.fontKey, Assets.assets.generated.NormalFont)
              // Bottom-left, clear of the hearts HUD in the top-left corner.
              .moveTo(Point(3, Layout.screen.height - 16))
          )
      )

    def setup(
        bootData: Unit,
        assetCollection: AssetCollection,
        dice: Dice
    ): Outcome[Startup[Shared]] =
      Outcome(Startup.Success(Shared.atStartup(dice)))

    def initialModel(startupData: Shared): Outcome[Model] =
      Update.init(startupData)

    def updateModel(context: Context, model: Model): GlobalEvent => Outcome[Model] =
      e =>
        subscriptions(model, context.frame.input)(e) match
          case Some(msg) => Update.update(Shared.fromContext(context), msg, model)
          case None      => Outcome(model)

    def present(context: Context, model: Model): Outcome[SceneUpdateFragment] =
      Outcome(mainUI(Shared.fromContext(context), model))

  // -----------------------------------------------------------------
  // UI
  // -----------------------------------------------------------------

  /** The root UI (named `mainUI`, since `game.ui` is the reusable-UI package): the showing scene's
    * UI, drawn into layers whose order is fixed here for every scene, with the FPS counter and the
    * letterbox bars on top.
    */
  def mainUI(shared: Shared, model: Model): SceneUpdateFragment =
    layersUI(shared.viewport) |+| sceneUI(shared, model) |+| barsUI(shared.viewport)

  /** Every layer is scaled by the whole-number `Screen.scaleFor`, and the screen-space layers are
    * moved so the 320x180 view is centred in the window. The world layer's camera is left to each
    * scene (`Screen.centredCamera`), since merged layers keep the first camera they're given.
    */
  private def layersUI(viewport: Size): SceneUpdateFragment =
    val centred = Screen.centredCamera(viewport)(Point.zero)
    SceneUpdateFragment(
      Layers.world   -> Layer.Content.empty,
      Layers.weather -> Layer.Content.empty.withCamera(centred),
      Layers.ui      -> Layer.Content.empty.withCamera(centred),
      Layers.screen  -> Layer.Content.empty.withCamera(centred),
      Layers.grading -> Layer.Content.empty,
      Layers.fps     -> Layer.Content.empty.withCamera(centred),
      Layers.bars    -> Layer.Content.empty
    ).withMagnification(Magnification(Screen.scaleFor(viewport)))

  /** Black bars over whatever is drawn outside the view (world beyond the screen, weather tiles),
    * in window coordinates (game pixels at the current scale).
    */
  private def barsUI(viewport: Size): SceneUpdateFragment =
    val window = Screen.windowFor(viewport) + Size(1)
    val view   = Rectangle(Screen.offsetFor(viewport), Layout.screen)
    val black  = Fill.Color(RGBA.Black)
    SceneUpdateFragment(
      Layers.bars -> Layer.Content(
        Batch(
          Rectangle(0, 0, window.width, view.y),
          Rectangle(0, view.bottom, window.width, window.height - view.bottom),
          Rectangle(0, view.y, view.x, view.height),
          Rectangle(view.right, view.y, window.width - view.right, view.height)
        ).filter(r => r.width > 0 && r.height > 0).map(r => Quad(r, black))
      )
    )

  private def sceneUI(shared: Shared, model: Model): SceneUpdateFragment =
    model.route match
      case SceneRoute.Title => TitleScene.ui(shared, model.title)
      case SceneRoute.World => model.world.fold(SceneUpdateFragment.empty)(WorldScene.ui(shared, _))
