package app.dieroboter.patches.world

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_GEOMETRY_DASH_WORLD

/**
 * Candidate 3: disable the ad consent flow.
 *
 * LiteRobTopActivity.setupGAM enables AppLovin's terms-and-privacy flow
 * (the first-launch privacy dialog) with setEnabled(true) — the boolean is
 * loaded by `const/4 v2, 1` at instruction 9, directly before the setEnabled
 * call at instruction 10 (layout verified against the 2.2.147 dex; the
 * fingerprint additionally requires the setEnabled call to be present).
 * Flipping that one constant to 0 disables the flow while leaving the rest
 * of setupGAM — including AppLovinSdk.initialize, which rewarded video
 * depends on — untouched. The Google UMP classes in the stack are driven by
 * the mediation adapters and go quiet with the MAX flow disabled.
 */
@Suppress("unused")
val disableAdConsentPatch = bytecodePatch(
    name = "Disable ad consent flow",
    description = "Flips the AppLovin terms-and-privacy flow in LiteRobTopActivity" +
        ".setupGAM from enabled to disabled, so the first-launch ad consent dialog " +
        "never appears.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GEOMETRY_DASH_WORLD)

    execute {
        SetupGamFingerprint.method.replaceInstruction(9, "const/4 v2, 0x0")
    }
}
