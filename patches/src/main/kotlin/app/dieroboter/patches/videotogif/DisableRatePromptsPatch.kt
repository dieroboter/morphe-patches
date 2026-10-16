package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

/**
 * Candidate 2: disable the rate-us prompts — and hide the settings row.
 *
 * Every rate prompt decision passes through Ba/c.l(Ta/k;)Z, which checks the
 * PH_ALLOWED_RATE_DIALOG_SOURCES config for the prompt's source. Its only
 * callers are the two rate-prompt drivers. Forcing it to false means no rate
 * dialog is ever considered allowed, regardless of source or counters.
 *
 * The "Rate us" row in the settings screen is a separate surface the gate
 * never controlled: it is declared in res/xml/ph_settings.xml and the
 * settings fragment only looks it up by key. So RateUsPreference's
 * constructor additionally calls Preference.setVisible(false) on itself
 * (obfuscated to x(Z)V in this build — identified by its callers: the SDK's
 * own preferences use it to show/hide themselves), which removes the row.
 */
@Suppress("unused")
val disableRatePromptsPatch = bytecodePatch(
    name = "Disable rate prompts",
    description = "Disables the in-app rate-us prompts (the SDK's rate-dialog source " +
        "gate always returns false) and hides the 'Rate us' row in the settings screen.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        RateDialogGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )

        val rateUsConstructor = RateUsPreferenceConstructorFingerprint.method
        val tailIndex = rateUsConstructor.implementation!!.instructions.size - 1
        rateUsConstructor.addInstructions(
            tailIndex,
            """
                const/4 v0, 0x0
                invoke-virtual {v1, v0}, Landroidx/preference/Preference;->x(Z)V
            """
        )
    }
}
