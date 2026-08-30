package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val bypassSensitiveMediaBlurLogger =
    Logger.getLogger("app.xpatches.patches.com.twitter.android.BypassSensitiveMediaBlurPatch")

/**
 * Bypasses the blur/age-gate interstitial X shows for sensitive media.
 *
 * Instead of anchoring on the obfuscated Compose renderer `com/x/sensitivemedia/impl/u`.c (which
 * is renamed per release), this patches the one stable choke point every render path reads:
 * `com/x/models/interstitial/MediaVisibilityResults.getBlurImageInterstitial()`. Its body is
 * replaced with `const/4 v0, 0x0; return-object v0`, so every caller (Compose timeline, post
 * detail, legacy views) receives a null interstitial and skips the blur/age gate.
 *
 * The match is by stable, unobfuscated name + return type. When the method is missing or
 * ambiguous in a given APK, the patch logs and skips instead of failing the whole apply.
 */
val bypassSensitiveMediaBlurPatch = bytecodePatch(
    name = "Bypass sensitive media blur",
    description = "Skips the blur/age-gate interstitial in Compose media, showing sensitive media directly.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        // 1. Procuramos por QUALQUER método no APK que contenha a string fixa do toString()
        // Isso vai nos dar a classe exata do MediaVisibilityResults, não importa o nome dela (h, z, x, etc.)
        val matches = Fingerprint(
            customMatch = { method ->
                method.implementation?.instructions?.any { instruction ->
                    instruction.toString().contains("MediaVisibilityResults(blurImageInterstitial=")
                } == true
            }
        ).matchAllOrNull() ?: emptyList()

        if (matches.isEmpty()) {
            bypassSensitiveMediaBlurLogger.warning(
                "Could not find MediaVisibilityResults via toString() fingerprint; skipping patch",
            )
            return@execute
        }

        // 2. Pegamos a classe que foi encontrada (ex: com/x/models/interstitial/h)
        val targetClass = matches.first().method.definingClass

        // 3. Agora localizamos o construtor (<init>) dessa classe específica
        val constructorMatch = Fingerprint(
            name = "<init>",
            definingClass = targetClass
        ).matchAllOrNull()?.firstOrNull()

        if (constructorMatch == null) {
            bypassSensitiveMediaBlurLogger.warning(
                "Constructor for $targetClass not found; skipping patch",
            )
            return@execute
        }

        constructorMatch.method.apply {
            val implementation = implementation
            if (implementation == null) {
                bypassSensitiveMediaBlurLogger.warning(
                    "Constructor for $targetClass has no body; skipping",
                )
            } else {
                // 4. Analisamos dinamicamente o bytecode do construtor para descobrir 
                // o nome do campo (hoje 'a') e o tipo do campo (hoje 'Lcom/x/models/interstitial/e;')
                // O construtor do Kotlin Data Class termina salvando o parâmetro no campo usando 'iput-object'
                val iputInstruction = instructions.firstOrNull { it.toString().contains("iput-object") }
                
                if (iputInstruction == null) {
                    bypassSensitiveMediaBlurLogger.warning(
                        "Could not find field assignment in constructor; skipping patch",
                    )
                    return@execute
                }

                // Extraímos a assinatura exata do campo diretamente da instrução original do Twitter (ex: Lcom/x/models/interstitial/h;->a:Lcom/x/models/interstitial/e;)
                val fieldSignature = iputInstruction.toString().substringAfter("iput-object ").substringBefore(",")

                // 5. Substituímos o corpo do construtor aplicando o nulo dinamicamente baseado na assinatura extraída
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
                    "Successfully dynamically patched $targetClass to always nullify the blur interstitial.",
                )
            }
        }
    }
}
