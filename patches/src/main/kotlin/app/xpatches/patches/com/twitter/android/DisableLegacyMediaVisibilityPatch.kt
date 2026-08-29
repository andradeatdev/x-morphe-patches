package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

/**
 * Forces the legacy "needs blur" predicate to always return false.
 *
 * `com/twitter/model/mediavisibility/d.a(g)Z` decides whether a tweet from the legacy
 * tweetview stack must be blurred. When the media visibility decision says so, the method
 * ends with `const/4 p0, 0x1` and returns true. We replace the whole body with a constant
 * `false`, which is exactly the first branch it already returns in the clean case
 * (`const/4 v0, 0x0; return v0`).
 */
val disableLegacyMediaVisibilityPatch = bytecodePatch(
    name = "Disable legacy sensitive media blur",
    description = "Forces the legacy 'needs blur' predicate to always return false.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val method = mutableClassDefBy("Lcom/twitter/model/mediavisibility/d;")
            .methods.first { candidate ->
                candidate.name == "a" &&
                    candidate.returnType == "Z" &&
                    candidate.parameterTypes == listOf("Lcom/twitter/model/mediavisibility/g;")
            }

        check(method.instructions.any { it.opcode == Opcode.IGET_OBJECT }) {
            "unexpected body in ${method.definingClass}.${method.name}"
        }

        method.removeInstructions(0, method.instructions.size)
        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
    }
}