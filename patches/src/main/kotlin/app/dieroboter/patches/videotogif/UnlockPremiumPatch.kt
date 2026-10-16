package app.dieroboter.patches.videotogif

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF

/**
 * Unlock premium (GIF Maker / Video to GIF, PremiumHelper SDK 5.3.2).
 *
 * The app gates everything through one SDK predicate:
 * Lcom/zipoapps/premiumhelper/a;->i()Z, which reads the persisted
 * "has_active_purchase" flag (written only by the billing layer after a real
 * Play purchase). Forcing it to return true makes the whole SDK treat the
 * user as premium: banner and interstitial ads are suppressed by the SDK's
 * own "User is Premium" path, the splash/paywall/relaunch offers route past,
 * and premium support/settings unlock.
 *
 * Note the app code itself never checks premium — all gating is SDK-side,
 * and the only other readers of the underlying purchase record are
 * analytics/FCM bookkeeping, so no feature gate bypasses this method.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium: forces the PremiumHelper SDK's isPremium check to " +
        "always return true, which also suppresses banner/interstitial ads and the " +
        "paywall and relaunch offers through the SDK's own premium paths.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        IsPremiumFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
