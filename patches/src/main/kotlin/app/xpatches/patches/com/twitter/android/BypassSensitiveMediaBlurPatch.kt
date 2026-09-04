package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

<<<<<<< HEAD
=======
/**
 * Bypasses the blur/age-gate interstitial that X shows for sensitive media in the
 * Compose (x-lite) timeline, post detail and profile views.
 *
 * The method `com/x/sensitivemedia/impl/u.c` renders the per-media component. Before the
 * final "no blur" branch it calls `getBlurImageInterstitial()`; the returned value decides
 * whether to show an interstitial. We zero `v0` right before the `if-eqz v0` check so it
 * always takes the "no interstitial" path, which is equivalent to the manually validated
 * `return null` edit (it skips the age-gate interstitial but keeps the Compose group
 * balanced, unlike a raw `goto`).
 */
>>>>>>> parent of ac1a48b (fix: resolve anchors by fingerprint so patches work across X versions)
val bypassSensitiveMediaBlurPatch = bytecodePatch(
    name = "Bypass sensitive media blur",
    description = "Skips the blur/age-gate interstitial in Compose media, showing sensitive media directly.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
<<<<<<< HEAD
        // 1. Procuramos pela string única do serializador da imagem de desfoque
        val matches = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { instruction ->
                    // Varre o pool de strings procurando pelo identificador único do modelo
                    instruction.toString().contains("com.x.models.interstitial.BlurImageInterstitial")
                } == true
            }
        ).matchAllOrNull() ?: emptyList()

        if (matches.isEmpty()) {
            bypassSensitiveMediaBlurLogger.warning(
                "Serializer string not found; skipping patch",
            )
            return@execute
        }

        // 2. A partir do serializador, sabemos que a classe correspondente é do pacote interstitial
        // Vamos buscar dinamicamente pelo construtor da classe de resultados que recebe o objeto de desfoque.
        // Como vimos no arquivo h.java, o construtor recebe o tipo 'e' (BlurImageInterstitial).
        // Para tornar genérico: buscamos um construtor (<init>) no pacote 'com/x/models/interstitial'
        // que receba exatamente um objeto daquela mesma pasta como parâmetro e retorne void (V).

        val allConstructors = Fingerprint(
            name = "<init>",
            returnType = "V"
        ).matchAllOrNull() ?: emptyList()

        // Filtra pelo padrão exato do construtor da classe MediaVisibilityResults (h.java)
        val targetConstructor = allConstructors.firstOrNull { match ->
            val method = match.method
            val isTargetPackage = method.definingClass.startsWith("com/x/models/interstitial/")
            val params = method.parameterTypes

            // O construtor deve ter exatamente 1 parâmetro, e esse parâmetro deve ser uma classe do mesmo pacote (o objeto e)
            isTargetPackage && params.size == 1 && params[0].startsWith("Lcom/x/models/interstitial/")
        }

        if (targetConstructor == null) {
            bypassSensitiveMediaBlurLogger.warning(
                "Target MediaVisibilityResults constructor not found; skipping patch",
            )
            return@execute
        }

        targetConstructor.method.apply {
            val implementation = implementation
            if (implementation == null) {
                bypassSensitiveMediaBlurLogger.warning(
                    "Constructor has no body; skipping",
                )
            } else {
                // 3. Pegamos a primeira instrução iput-object que joga o parâmetro no campo da classe
                val iputInstruction = instructions.firstOrNull { it.toString().contains("iput-object") }

                if (iputInstruction == null) {
                    bypassSensitiveMediaBlurLogger.warning(
                        "iput-object instruction not found in constructor; skipping",
                    )
                    return@execute
                }

                // Extrai a assinatura do campo de forma limpa (ex: Lcom/x/models/interstitial/h;->a:Lcom/x/models/interstitial/e;)
                val fieldSignature = iputInstruction.toString().substringAfter("iput-object ").substringBefore(",")

                // 4. Substitui as instruções do construtor para forçar o campo a salvar null
                removeInstructions(0, instructions.size)
                addInstructions(
                    0,
                    """
                    .registers 3
                    const/4 v0, 0x0
                    iput-object v0, p0, $fieldSignature
                    return-void
                    """.trimIndent()
                )

                bypassSensitiveMediaBlurLogger.info(
                    "Successfully robustly patched MediaVisibilityResults via serializer mapping.",
                )
            }
=======
        val method = mutableClassDefBy("Lcom/x/sensitivemedia/impl/u;")
            .methods.first { candidate ->
                candidate.name == "c" &&
                    candidate.returnType == "Ljava/lang/Object;" &&
                    candidate.parameterTypes == listOf("Landroidx/compose/runtime/Composer;", "I")
            }

        val ins = method.instructions

        val invokeIndex = ins.indexOfFirst {
            it is ReferenceInstruction &&
                (it.reference as? MethodReference)?.name == "getBlurImageInterstitial"
>>>>>>> parent of ac1a48b (fix: resolve anchors by fingerprint so patches work across X versions)
        }
        check(invokeIndex >= 0) { "getBlurImageInterstitial call not found in ${method.definingClass}.${method.name}" }
        check(invokeIndex >= 1) { "unexpected method start ${method.definingClass}.${method.name}" }
        check(ins[invokeIndex - 1].opcode == Opcode.IF_EQZ) {
            "expected if-eqz guarding getBlurImageInterstitial in ${method.definingClass}.${method.name}"
        }

        // Force the media-visibility path to always produce a null BlurImageInterstitial:
        //   const/4 v0, 0x0
        //   if-eqz v0, :cond_x   (always taken, skipping the blur-fetching call)
        // The null interstitial is what later feeds the q boundary frame, so no blur/age-gate shows.
        method.addInstruction(invokeIndex - 1, "const/4 v0, 0x0")
    }
}
