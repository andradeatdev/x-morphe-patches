package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val bypassSensitiveMediaBlurLogger =
    Logger.getLogger("app.xpatches.patches.com.twitter.android.BypassSensitiveMediaBlurPatch")

val bypassSensitiveMediaBlurPatch = bytecodePatch(
    name = "Bypass sensitive media blur",
    description = "Skips the blur/age-gate interstitial in Compose media, showing sensitive media directly.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
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
        }
    }
}
