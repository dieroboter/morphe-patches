package app.dieroboter.patches.world

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_GEOMETRY_DASH_WORLD

/**
 * Candidate 4: disable AppMetrica telemetry.
 *
 * AppMetrica ships with the Yandex mediation stack; its reporter is activated
 * from exactly one place in the app, the Yandex glue (yads/eh.invoke). Making
 * AppMetrica.activateReporter itself a no-op means the reporter is never
 * activated and its events are dropped, while the glue code stays
 * byte-identical. Same technique as the GIF Maker bundle's AppMetrica patch.
 */
@Suppress("unused")
val disableAppMetricaPatch = bytecodePatch(
    name = "Disable AppMetrica activation",
    description = "Makes AppMetrica.activateReporter a no-op, so the AppMetrica " +
        "reporter the Yandex mediation glue sets up is never activated and its " +
        "events are dropped.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GEOMETRY_DASH_WORLD)

    execute {
        AppMetricaActivateReporterFingerprint.method.addInstructions(0, "return-void")
    }
}
