package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
val bypassSensitiveMediaBlurPatch = bytecodePatch(
    name = "Bypass sensitive media blur",
    description = "Skips the blur/age-gate interstitial in Compose media, showing sensitive media directly.",
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
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