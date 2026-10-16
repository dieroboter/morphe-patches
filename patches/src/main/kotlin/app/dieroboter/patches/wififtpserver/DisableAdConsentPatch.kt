package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER

/**
 * Patch 2 (analysis list): disable the UMP ad consent flow on its own.
 *
 * In MainActivity.onCreate the app starts a consent info update and only initializes
 * ads once the consent gate reports that ads may be requested. This patch forces the
 * gate open and stops the update task, so no consent form is ever requested or shown
 * and ads (if left enabled) load immediately.
 *
 * Has no visible effect when combined with "Remove banner ads".
 */
@Suppress("unused")
val disableAdConsentPatch = bytecodePatch(
    name = "Disable ad consent flow",
    description = "Disables the Google UMP consent flow that gates ad loading. The consent " +
        "info update never runs and ads are treated as always allowed to load. Has no " +
        "visible effect if 'Remove banner ads' is also enabled.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        // The gate method's first local register is v0; return true immediately.
        ConsentGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
        ConsentUpdateTaskFingerprint.method.addInstructions(0, "return-void")
    }
}
