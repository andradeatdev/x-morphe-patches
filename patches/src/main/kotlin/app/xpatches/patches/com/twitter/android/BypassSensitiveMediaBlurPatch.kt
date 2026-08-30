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
        val matches = Fingerprint(
            name = "getBlurImageInterstitial",
            returnType = "Lcom/x/models/interstitial/BlurImageInterstitial;",
        ).matchAllOrNull() ?: emptyList()

        when (matches.size) {
            0 -> bypassSensitiveMediaBlurLogger.warning(
                "getBlurImageInterstitial not found; skipping Bypass sensitive media blur",
            )

            1 -> matches[0].method.apply {
                val implementation = implementation
                if (implementation == null) {
                    bypassSensitiveMediaBlurLogger.warning(
                        "getBlurImageInterstitial has no body in $definingClass.$name; skipping",
                    )
                } else {
                    removeInstructions(0, instructions.size)
                    addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                    bypassSensitiveMediaBlurLogger.info(
                        "Patched $definingClass.$name to always return a null BlurImageInterstitial",
                    )
                }
            }

            else -> bypassSensitiveMediaBlurLogger.warning(
                "getBlurImageInterstitial is ambiguous (${matches.size} matches); skipping Bypass sensitive media blur",
            )
        }
    }
}