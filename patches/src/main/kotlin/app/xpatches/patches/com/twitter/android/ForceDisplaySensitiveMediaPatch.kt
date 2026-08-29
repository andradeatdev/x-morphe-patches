package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Forces `com/x/models/AccountSettings.getDisplaySensitiveMedia()` to always return
 * `Boolean.TRUE`, regardless of the server-provided `displaySensitiveMedia` account field.
 *
 * The original getter is a plain `iget-object v0, p0, ...displaySensitiveMedia;` +
 * `return-object v0`. We replace the whole body with a constant `true`.
 */
val forceDisplaySensitiveMediaPatch = bytecodePatch(
    name = "Always display sensitive media",
    description = "Forces AccountSettings.getDisplaySensitiveMedia to always return true.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val method = mutableClassDefBy("Lcom/x/models/AccountSettings;")
            .methods.first { candidate ->
                candidate.name == "getDisplaySensitiveMedia" &&
                    candidate.returnType == "Ljava/lang/Boolean;"
            }

        method.removeInstructions(0, method.instructions.size)
        method.addInstructions(
            0,
            "sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;\nreturn-object v0",
        )
    }
}