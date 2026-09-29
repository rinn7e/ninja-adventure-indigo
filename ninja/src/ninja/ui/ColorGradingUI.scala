package ninja.ui

import indigo.*
import ninja.common.Types.*
import ninja.common.constant.{Gradings, Layers}

/** Godot's `ColorCorrection`: a screen shader that maps each channel of everything drawn below it
  * through a gradient, cross-fading from the old gradient to the new one.
  *
  * In Indigo that's a blend shader on its own (empty) layer: blend shaders see the layer (`SRC`)
  * and everything composited below it so far (`DST`), and their output replaces it.
  */
object ColorGradingUI:

  val shaderId: ShaderId = ShaderId("color-grading")

  /** Register with `BootResult.withShaders`. Mirrors Godot's `shader_color_correction.gdshader`,
    * with the gradient textures replaced by their stops (Godot samples a 256px linear texture of
    * them).
    */
  val shader: BlendShader.Source =
    BlendShader
      .Source(shaderId)
      .withFragmentProgram(
        s"""layout (std140) uniform ColorGradingData {
           |  vec4 GRADING;               // x: amount (0 = from, 1 = to), y: from stops, z: to stops
           |  vec4 FROM_STOPS[${Gradings.maxStops}]; // rgb: colour, w: offset
           |  vec4 TO_STOPS[${Gradings.maxStops}];
           |};
           |
           |vec3 sampleGradient(vec4 stops[${Gradings.maxStops}], float count, vec3 color) {
           |  vec3 result = stops[0].rgb;
           |  for (int ch = 0; ch < 3; ch++) {
           |    float x = color[ch];
           |    float y = stops[0][ch];
           |    for (int i = 1; i < ${Gradings.maxStops}; i++) {
           |      if (float(i) >= count) break;
           |      vec4 a = stops[i - 1];
           |      vec4 b = stops[i];
           |      if (x >= b.w) y = b[ch];
           |      else if (x >= a.w) y = mix(a[ch], b[ch], (x - a.w) / max(b.w - a.w, 0.00001));
           |    }
           |    result[ch] = y;
           |  }
           |  return result;
           |}
           |
           |vec4 fragment(vec4 color) {
           |  vec3 from = sampleGradient(FROM_STOPS, GRADING.y, DST.rgb);
           |  vec3 to = sampleGradient(TO_STOPS, GRADING.z, DST.rgb);
           |  return vec4(mix(from, to, GRADING.x), DST.a);
           |}
           |""".stripMargin
      )

  /** The blend material for one frame of the cross-fade. */
  final case class GradingMaterial(from: Grading, to: Grading, amount: Double)
      extends BlendMaterial.SrcAndDst:

    private def stopsData(grading: Grading): Batch[Float] =
      val stops = Gradings.stopsOf(grading)
      val padded =
        stops ++ Batch.fill(Gradings.maxStops - stops.length)(stops.last)
      padded.flatMap { case (offset, color) =>
        Batch(color.r.toFloat, color.g.toFloat, color.b.toFloat, offset.toFloat)
      }

    lazy val toShaderData: ShaderData =
      ShaderData(
        shaderId,
        Batch(
          UniformBlock(
            UniformBlockName("ColorGradingData"),
            Batch(
              Uniform("GRADING") -> ShaderPrimitive.rawBatch(
                Batch(
                  amount.toFloat,
                  Gradings.stopsOf(from).length.toFloat,
                  Gradings.stopsOf(to).length.toFloat,
                  0.0f
                )
              ),
              Uniform("FROM_STOPS") -> ShaderPrimitive.rawBatch(stopsData(from)),
              Uniform("TO_STOPS")   -> ShaderPrimitive.rawBatch(stopsData(to))
            )
          )
        )
      )

  /** The grading layer: `from` fading into `to` over `Gradings.fade`, since `since`. */
  def gradingUI(from: Grading, to: Grading, since: Seconds, now: Seconds): SceneUpdateFragment =
    val amount = ((now - since).toDouble / Gradings.fade.toDouble).max(0).min(1)
    SceneUpdateFragment(
      Layers.grading -> Layer.Content.empty
        .withBlending(Blending.Normal.withBlendMaterial(GradingMaterial(from, to, amount)))
    )
