package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val forceDisplaySensitiveMediaLogger =
    Logger.getLogger("app.xpatches.patches.com.twitter.android.ForceDisplaySensitiveMediaPatch")

/**
 * Forces `com/x/models/AccountSettings.getDisplaySensitiveMedia()` to always return
 * `Boolean.TRUE`, regardless of the server-provided `displaySensitiveMedia` account field.
 *
 * The match is by stable, unobfuscated name + return type. When the method is missing or
 * ambiguous in a given APK, the patch logs and skips instead of failing the whole apply.
 */
val forceDisplaySensitiveMediaPatch = bytecodePatch(
    name = "Always display sensitive media",
    description = "Forces AccountSettings.getDisplaySensitiveMedia to always return true.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val matches = Fingerprint(
            name = "getDisplaySensitiveMedia",
            returnType = "Ljava/lang/Boolean;",
        ).matchAllOrNull() ?: emptyList()

        when (matches.size) {
            0 -> forceDisplaySensitiveMediaLogger.warning(
                "getDisplaySensitiveMedia not found; skipping Always display sensitive media",
            )

            1 -> matches[0].method.apply {
                val implementation = implementation
                if (implementation == null) {
                    forceDisplaySensitiveMediaLogger.warning(
                        "getDisplaySensitiveMedia has no body in $definingClass.$name; skipping",
                    )
                } else {
                    removeInstructions(0, instructions.size)
                    addInstructions(
                        0,
                        "sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;\nreturn-object v0",
                    )
                    forceDisplaySensitiveMediaLogger.info(
                        "Patched $definingClass.$name to always return true",
                    )
                }
            }

            else -> forceDisplaySensitiveMediaLogger.warning(
                "getDisplaySensitiveMedia is ambiguous (${matches.size} matches); skipping Always display sensitive media",
            )
        }
    }
}