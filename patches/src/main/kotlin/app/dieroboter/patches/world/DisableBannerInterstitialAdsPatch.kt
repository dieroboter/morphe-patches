package app.dieroboter.patches.world

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_GEOMETRY_DASH_WORLD

/**
 * Candidate 1: disable banner and interstitial ads.
 *
 * All ads in the game are driven from native code through LiteRobTopActivity's
 * JNI bridge. No-oping _enableBanner and _showInterstitial removes both ad
 * surfaces, and forcing _hasCachedInterstitial to false keeps the native
 * side from waiting on an interstitial that will never show. The banner and
 * interstitial objects are still created at SDK init (LiteRobTopActivity$1
 * .onSdkInitialized) but are never displayed. Rewarded video is deliberately
 * NOT touched — it has its own bridge methods (_showRewardedVideo /
 * _hasCachedRewardedVideo), which stay functional.
 */
@Suppress("unused")
val disableBannerInterstitialAdsPatch = bytecodePatch(
    name = "Disable banner and interstitial ads",
    description = "No-ops the native bridge methods that show banner and interstitial " +
        "ads (LiteRobTopActivity._enableBanner / _showInterstitial) and makes " +
        "_hasCachedInterstitial report false. Rewarded video ads are left working.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GEOMETRY_DASH_WORLD)

    execute {
        EnableBannerFingerprint.method.addInstructions(0, "return-void")
        ShowInterstitialFingerprint.method.addInstructions(0, "return-void")
        HasCachedInterstitialFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )
    }
}
