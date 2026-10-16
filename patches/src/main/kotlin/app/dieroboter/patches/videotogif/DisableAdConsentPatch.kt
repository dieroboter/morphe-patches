package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

/**
 * Candidate 1: disable the ad-consent (UMP/GDPR) flow.
 *
 * The PremiumHelper SDK's consent manager (LGa/h) funnels every consent action
 * into e(Activity, Continuation), which builds ConsentRequestParameters and
 * calls UMP's requestConsentInfoUpdate. Returning Unit immediately means no
 * consent info is ever requested and no consent form can be loaded or shown.
 * (Premium users skip consent anyway once "Unlock premium" is on; this removes
 * the machinery itself rather than relying on that.)
 */
@Suppress("unused")
val disableAdConsentPatch = bytecodePatch(
    name = "Disable ad consent flow",
    description = "Disables the Google UMP ad-consent flow: the SDK's consent request " +
        "returns immediately, so no consent information is requested and no consent " +
        "form is ever loaded or shown.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        ConsentRequestFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ldc/B;->a:Ldc/B;
                return-object v0
            """
        )
    }
}
