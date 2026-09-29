package ninja

import indigo.*
import indigoextras.subsystems.FPSCounter
import ninja.common.asset.GameAssets
import ninja.common.constant.{Layers, Layout}
import ninja.Subscription.subscriptions
import ninja.Type.*
import ninja.common.Types.{SceneRoute, Shared}
import ninja.scene.{TitleScene, WorldScene}
import ninja.generated.{Assets, NormalFont}
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
        BootResult(ninja.generated.NinjaConfig.config, ())
          .withAssets(GameAssets.assets(flags.getOrElse("baseUrl", "")))
          .withFonts(GameAssets.fontInfo)
          .withSubSystems(
            FPSCounter
              .tint(Layers.fps, NormalFont.fontKey, Assets.assets.generated.NormalFont)
              // Bottom-left, clear of the hearts HUD in the top-left corner.
              .moveTo(Point(3, Layout.screen.height - 13))
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
        subscriptions(model, context.frame.input.keyboard)(e) match
          case Some(msg) => Update.update(Shared.fromContext(context), msg, model)
          case None      => Outcome(model)

    def present(context: Context, model: Model): Outcome[SceneUpdateFragment] =
      Outcome(mainUI(Shared.fromContext(context), model))

  // -----------------------------------------------------------------
  // UI
  // -----------------------------------------------------------------

  /** The root UI (named `mainUI`, since `ninja.ui` is the reusable-UI package): the showing scene's
    * UI, drawn into layers whose order is fixed here for every scene, with the FPS counter on top.
    */
  def mainUI(shared: Shared, model: Model): SceneUpdateFragment =
    layersUI |+| sceneUI(shared, model)

  private val layersUI: SceneUpdateFragment =
    SceneUpdateFragment(
      Layers.world   -> Layer.Content.empty,
      Layers.weather -> Layer.Content.empty,
      Layers.ui      -> Layer.Content.empty,
      Layers.screen  -> Layer.Content.empty,
      Layers.fps     -> Layer.Content.empty
    ).withMagnification(Magnification(Layout.magnification))

  private def sceneUI(shared: Shared, model: Model): SceneUpdateFragment =
    model.route match
      case SceneRoute.Title => TitleScene.ui(shared, model.title)
      case SceneRoute.World => model.world.fold(SceneUpdateFragment.empty)(WorldScene.ui(shared, _))
