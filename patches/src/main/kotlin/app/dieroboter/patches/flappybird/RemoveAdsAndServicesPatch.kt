package app.dieroboter.patches.flappybird

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_FLAPPY_BIRD

/**
 * The ad / external-services removal of the 64-bit port:
 *
 * - GameActivity.b() still builds the AdView (other layout code references it),
 *   but the port hides it (setVisibility(INVISIBLE), matching the visibility
 *   Runnable below) and deletes the ad request, so no ad is ever loaded.
 * - The Runnable that used to make the AdView VISIBLE on the UI thread now
 *   sets INVISIBLE instead (const 0x0 -> 0x4).
 * - GameActivity.onStart() no longer hooks the activity into Google Play
 *   Games (basegameutils), which the port disconnected.
 * - flappy/c.a(IIII) — a tap-zone handler that fired a scene action when a
 *   specific screen region was tapped — is gutted to a bare return, and the
 *   state dispatcher loses its c(11, ...) trigger call, as in the port.
 */
@Suppress("unused")
val removeAdsAndServicesPatch = bytecodePatch(
    name = "Remove ads and Google services hooks",
    description = "Hides the AdMob banner and removes the ad request so no ad is ever loaded, " +
        "disconnects the Google Play Games activity hook, and removes the port-removed " +
        "tap-zone handler and scene trigger tied to the old services UI.",
    default = true
) {
    compatibleWith(COMPATIBILITY_FLAPPY_BIRD)

    execute {
        GameActivityLayoutFingerprint.method.apply {
            // Delete the ad request block (new-instance com/google/ads/d .. AdView.a(d)).
            val requestStart = requireFirstIndex {
                it.opcode == Opcode.NEW_INSTANCE && it.refString() == "Lcom/google/ads/d;"
            }
            if (implementation!!.instructions[requestStart + 3].refString() !=
                "Lcom/google/ads/AdView;->a(Lcom/google/ads/d;)V"
            ) {
                throw PatchException("Unexpected ad-request shape in GameActivity.b()")
            }
            removeInstructions(requestStart, 4)

            // Hide the banner right after it is created and refreshed.
            val refresh = requireFirstIndex {
                it.refString() == "Lcom/google/ads/AdView;->refreshDrawableState()V"
            }
            addInstructions(
                refresh + 1,
                """
                const/4 v4, 0x4
                invoke-virtual {v3, v4}, Lcom/google/ads/AdView;->setVisibility(I)V
                """,
            )
        }

        // The visibility Runnable now hides the banner instead of showing it.
        AdVisibilityRunnableFingerprint.method.apply {
            val call = requireFirstIndex {
                it.refString() == "Lcom/google/ads/AdView;->setVisibility(I)V"
            }
            val constIndex = call - 1
            val constInsn = implementation!!.instructions[constIndex]
            if (constInsn !is NarrowLiteralInstruction || constInsn.narrowLiteral != 0) {
                throw PatchException("Unexpected visibility constant in com.dotgears.b.run()")
            }
            val register = (constInsn as OneRegisterInstruction).registerA
            replaceInstruction(constIndex, "const/4 v$register, 0x4")
        }

        // Disconnect the Google Play Games hookup in onStart.
        GameActivityOnStartFingerprint.method.apply {
            val call = requireFirstIndex {
                it.refString() == "Lcom/google/example/games/basegameutils/a;->a(Landroid/app/Activity;)V"
            }
            if (implementation!!.instructions[call - 1].refString() !=
                "Lcom/dotgears/GameActivity;->g:Lcom/google/example/games/basegameutils/a;"
            ) {
                throw PatchException("Unexpected Play Games hookup shape in GameActivity.onStart()")
            }
            removeInstructions(call - 1, 2)
        }

        // Gut the tap-zone handler to a bare return.
        SceneTapZoneFingerprint.method.apply {
            removeInstructions(0, implementation!!.instructions.size)
            addInstructions(0, "return-void")
        }

        // Remove the dispatcher's c(11, ...) trigger call.
        SceneDispatchFingerprint.method.apply {
            val calls = allIndices { insn ->
                insn.refString() == "Lcom/dotgears/flappy/c;->c(II)V"
            }.filter { index ->
                val prev = implementation!!.instructions[index - 1]
                prev is NarrowLiteralInstruction && prev.narrowLiteral == 0xb
            }
            if (calls.size != 1) {
                throw PatchException("Expected one c(11, ...) call in the scene dispatcher, found ${calls.size}")
            }
            removeInstructions(calls[0] - 1, 2)
        }
    }
}
