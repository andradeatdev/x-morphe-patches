package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import java.util.logging.Logger

private val disableLegacyMediaVisibilityLogger =
    Logger.getLogger("app.xpatches.patches.com.twitter.android.DisableLegacyMediaVisibilityPatch")

/**
 * Forces the legacy "needs blur" predicate to always return false.
 *
 * On the legacy tweetview stack, sensitive media blurred state is decided by a static predicate
 * taking a `com/twitter/model/mediavisibility/` value and returning a boolean, reading instance
 * fields (IGET_OBJECT) of that value. On newer X builds that method already returns false and no
 * match exists; on older builds the matching method is located behaviorally (return type `Z`,
 * exactly one parameter in the mediavisibility package, body accesses fields) instead of by the
 * obfuscated class/method letters, which change between releases.
 *
 * When the method is missing or ambiguous, the patch logs and skips instead of crashing.
 */
val disableLegacyMediaVisibilityPatch = bytecodePatch(
    name = "Disable legacy sensitive media blur",
    description = "Forces the legacy 'needs blur' predicate to always return false.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val matches = Fingerprint(
            returnType = "Z",
            custom = { method, _ ->
                method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].startsWith("Lcom/twitter/model/mediavisibility/") &&
                    (method.implementation?.instructions?.any { it.opcode == Opcode.IGET_OBJECT } ?: false)
            },
        ).matchAllOrNull() ?: emptyList()

        when (matches.size) {
            0 -> disableLegacyMediaVisibilityLogger.warning(
                "Legacy 'needs blur' predicate not found; skipping Disable legacy sensitive media blur",
            )

            1 -> matches[0].method.apply {
                val implementation = implementation
                if (implementation == null) {
                    disableLegacyMediaVisibilityLogger.warning(
                        "Predicate has no body in $definingClass.$name; skipping",
                    )
                } else {
                    removeInstructions(0, instructions.size)
                    addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    disableLegacyMediaVisibilityLogger.info(
                        "Patched $definingClass.$name to always return false",
                    )
                }
            }

            else -> {
                val definitions = matches.joinToString { it.method.definingClass + "." + it.method.name }
                disableLegacyMediaVisibilityLogger.warning(
                    "Legacy 'needs blur' predicate is ambiguous (${matches.size} matches: $definitions); skipping",
                )
            }
        }
    }
}