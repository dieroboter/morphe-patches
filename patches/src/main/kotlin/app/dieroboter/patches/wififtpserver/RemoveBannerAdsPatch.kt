package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER

/**
 * Patch 1 (analysis list): remove the banner ad.
 *
 * The app has exactly one ad: an AdMob banner built in MainActivity.v() and loaded
 * once by a delayed runnable. MobileAds.initialize() happens in MainActivity.s().
 * Returning early from both means the SDK is never initialized, the AdView is never
 * created, and @id/adContainer (wrap_content) collapses to nothing.
 */
@Suppress("unused")
val removeBannerAdsPatch = bytecodePatch(
    name = "Remove banner ads",
    description = "Removes the AdMob banner ad from the main screen. The ads SDK is never " +
        "initialized and the banner view is never created or loaded.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        AdsInitFingerprint.method.addInstructions(0, "return-void")
        BannerBuildFingerprint.method.addInstructions(0, "return-void")
    }
}
