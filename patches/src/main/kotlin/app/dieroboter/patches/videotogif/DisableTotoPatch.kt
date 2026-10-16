package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

private const val RETURN_UNIT = """
    sget-object v0, Ldc/B;->a:Ldc/B;
    return-object v0
"""

/**
 * Disable the Toto backend (ZipoApps' server-driven config/offers system).
 *
 * initToto registers the app-start listener that drives the config fetch and
 * offer cycle; postConfig posts device/config data back (also invoked by
 * PostConfigWorker); registerFcmToken enrolls the device for promotional
 * pushes (also invoked by TotoRegisterWorker); scheduleRegister enqueues that
 * worker from the billing layer. All four become no-ops. getConfig is
 * deliberately left alone: it returns a real response object, and with the
 * listener dead its only remaining caller is a manual action in the SDK's
 * secret settings screen.
 *
 * Split out of the former combined telemetry patch in 1.3.1.
 *
 * 1.3.2 crash fix: initToto does NOT return Unit — it returns an LBa/a result
 * object (two flags and a timestamp), and xa/j.invokeSuspend check-casts the
 * result to LBa/a. The 1.3.0/1.3.1 Unit return crashed the app at the splash
 * screen with "ClassCastException: dc.B cannot be cast to Ba.a" (confirmed by
 * the on-device logcat crash buffer). initToto now returns the SDK's own
 * neutral result instance, the LBa/a$b singleton (built with 0L, false,
 * false), which is the value the stock code itself produces for a skipped
 * cycle — xa/j reads its fields and takes the no-work path. The other three
 * methods genuinely return Unit (verified in their own bytecode and at their
 * call sites, which never check-cast the result).
 */
@Suppress("unused")
val disableTotoPatch = bytecodePatch(
    name = "Disable Toto backend",
    description = "Disables the Toto server-driven config/offers backend: its app-start " +
        "initialization, config reporting (including the background worker), FCM " +
        "promotional-push token registration and the worker scheduling all become no-ops.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        // NOT a Unit return: initToto returns an LBa/a result and its caller
        // check-casts it. Return the SDK's neutral singleton instance instead.
        TotoInitFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, LBa/a${'$'}b;->d:LBa/a${'$'}b;
                return-object v0
            """
        )
        TotoPostConfigFingerprint.method.addInstructions(0, RETURN_UNIT)
        TotoRegisterFcmTokenFingerprint.method.addInstructions(0, RETURN_UNIT)
        TotoScheduleRegisterFingerprint.method.addInstructions(0, "return-void")
    }
}
