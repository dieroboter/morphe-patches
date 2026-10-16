package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

/**
 * Disable AppMetrica activation.
 *
 * 1.3.0 removed the activateReporter call from inside the Yandex mediation
 * glue (yads/eh.invoke) — the one patch in the bundle that deleted an
 * instruction instead of prepending a return, and the prime suspect for the
 * splash crash reported against the combined telemetry patch. 1.3.1 uses the
 * proven prepend technique on the library entry point instead:
 * AppMetrica.activateReporter itself becomes a no-op. Its only caller in the
 * app is that same Yandex glue, so the effect is identical (the reporter is
 * never activated and drops its events) while eh.invoke stays byte-identical.
 */
@Suppress("unused")
val disableAppMetricaPatch = bytecodePatch(
    name = "Disable AppMetrica activation",
    description = "Makes AppMetrica.activateReporter a no-op, so the AppMetrica " +
        "reporter the Yandex mediation glue sets up is never activated and its " +
        "events are dropped.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        AppMetricaActivateReporterFingerprint.method.addInstructions(0, "return-void")
    }
}
