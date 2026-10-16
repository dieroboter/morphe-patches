package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

/**
 * Disable the Singular analytics SDK.
 *
 * The SDK has exactly one Singular.init call site in the app: the
 * PremiumHelper SingularManager coroutine na/e.invokeSuspend. Returning Unit
 * immediately means Singular is never initialized, so it collects and sends
 * nothing. Split out of the former combined telemetry patch in 1.3.1.
 */
@Suppress("unused")
val disableSingularPatch = bytecodePatch(
    name = "Disable Singular telemetry",
    description = "Prevents the Singular analytics SDK from initializing: its only " +
        "init call site (the PremiumHelper Singular manager coroutine) returns " +
        "immediately.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        SingularInitFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ldc/B;->a:Ldc/B;
                return-object v0
            """
        )
    }
}
