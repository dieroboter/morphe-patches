package app.dieroboter.patches.klondike

import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_KLONDIKE

private const val MOBILE_ADS = "Lcom/google/android/gms/ads/MobileAds;"
private const val BASE_AD_VIEW = "Lcom/google/android/gms/ads/BaseAdView;"
private const val GMS_INTERSTITIAL = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;"
private const val GMS_REWARDED = "Lcom/google/android/gms/ads/rewarded/RewardedAd;"
private const val GMS_APP_OPEN = "Lcom/google/android/gms/ads/appopen/AppOpenAd;"
private const val FAN_INIT = "Lcom/facebook/ads/AudienceNetworkAds;"
private const val FAN_AD_VIEW = "Lcom/facebook/ads/AdView;"
private const val FAN_INTERSTITIAL = "Lcom/facebook/ads/InterstitialAd;"
private const val APS_REQUEST = "Lcom/amazon/device/ads/DTBAdRequest;"

private const val CONTEXT = "Landroid/content/Context;"
private const val STRING = "Ljava/lang/String;"
private const val AD_REQUEST = "Lcom/google/android/gms/ads/AdRequest;"
private const val AD_MANAGER_REQUEST = "Lcom/google/android/gms/ads/admanager/AdManagerAdRequest;"

/**
 * Disable all ads and ad components (Klondike Solitaire 5.9).
 *
 * The ad stack is Google AdMob mediation (publisher ca-app-pub-1158309768049631)
 * with Meta Audience Network bidding into it, plus Amazon Publisher Services
 * (APS/DTB) feeding the same mediation. All three networks are reached
 * exclusively through their SDKs' public Java entry points, which are named
 * (not obfuscated) in this build, so the patch no-ops those entry points:
 *
 * - AdMob: MobileAds.initialize (both overloads) and the mediation-adapter
 *   initialization kill; BaseAdView.loadAd (the banner load every AdView
 *   subclass inherits); the static load() entry points of InterstitialAd,
 *   RewardedAd and AppOpenAd. With the loads dead, the load callbacks never
 *   fire, which is the SDKs' ordinary no-fill state.
 * - Meta FAN: AudienceNetworkAds.initialize and the loadAd entry points of
 *   its banner and interstitial classes (FAN participates as a bidder into
 *   AdMob, so this is belt-and-braces on top of the mediation kill).
 * - Amazon APS: every DTBAdRequest load entry point (loadAd x4,
 *   loadSmartBanner).
 *
 * 20 methods in total, all void, all prepended with return-void by
 * [noOpMethod]; every target is asserted to match exactly one method.
 *
 * Deliberately not touched: the game's own KlondikeActivity and the
 * DoodleMobile helper layer. The SDK-level kills starve every caller at
 * once, and leaving the app lifecycle code intact avoids the crash class
 * that comes from gutting glue code whose other duties are not visible.
 */
@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable all ads and ad components",
    description = "Disables the game's entire ad stack: AdMob mediation initialization and the " +
        "banner, interstitial, rewarded and app-open load entry points; Meta Audience " +
        "Network initialization and loads; and Amazon APS (DTB) ad requests. No ads of any " +
        "format are fetched or shown.",
    default = true
) {
    compatibleWith(COMPATIBILITY_KLONDIKE)

    execute {
        // --- AdMob / Google Mobile Ads ---
        noOpMethod(MOBILE_ADS, "initialize", CONTEXT)
        noOpMethod(
            MOBILE_ADS, "initialize",
            CONTEXT + "Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;"
        )
        noOpMethod(MOBILE_ADS, "disableMediationAdapterInitialization", CONTEXT)
        noOpMethod(BASE_AD_VIEW, "loadAd", AD_REQUEST)
        noOpMethod(
            GMS_INTERSTITIAL, "load",
            CONTEXT + STRING + AD_REQUEST +
                "Lcom/google/android/gms/ads/interstitial/InterstitialAdLoadCallback;"
        )
        noOpMethod(
            GMS_REWARDED, "load",
            CONTEXT + STRING + AD_REQUEST +
                "Lcom/google/android/gms/ads/rewarded/RewardedAdLoadCallback;"
        )
        noOpMethod(
            GMS_REWARDED, "load",
            CONTEXT + STRING + AD_MANAGER_REQUEST +
                "Lcom/google/android/gms/ads/rewarded/RewardedAdLoadCallback;"
        )
        noOpMethod(
            GMS_APP_OPEN, "load",
            CONTEXT + STRING + AD_REQUEST + "Lcom/google/android/gms/ads/appopen/AppOpenAd\$AppOpenAdLoadCallback;"
        )
        noOpMethod(
            GMS_APP_OPEN, "load",
            CONTEXT + STRING + AD_REQUEST + "I" +
                "Lcom/google/android/gms/ads/appopen/AppOpenAd\$AppOpenAdLoadCallback;"
        )
        noOpMethod(
            GMS_APP_OPEN, "load",
            CONTEXT + STRING + AD_MANAGER_REQUEST + "I" +
                "Lcom/google/android/gms/ads/appopen/AppOpenAd\$AppOpenAdLoadCallback;"
        )

        // --- Meta Audience Network ---
        noOpMethod(FAN_INIT, "initialize", CONTEXT)
        noOpMethod(FAN_AD_VIEW, "loadAd")
        noOpMethod(FAN_AD_VIEW, "loadAd", "Lcom/facebook/ads/AdView\$AdViewLoadConfig;")
        noOpMethod(FAN_INTERSTITIAL, "loadAd")
        noOpMethod(FAN_INTERSTITIAL, "loadAd", "Lcom/facebook/ads/InterstitialAd\$InterstitialLoadAdConfig;")

        // --- Amazon Publisher Services (DTB) ---
        noOpMethod(APS_REQUEST, "loadAd", "Lcom/amazon/device/ads/DTBAdCallback;")
        noOpMethod(APS_REQUEST, "loadAd", "Lcom/amazon/device/ads/DTBAdCallback;II")
        noOpMethod(APS_REQUEST, "loadAd", "Lcom/amazon/device/ads/DTBAdCallback;Ljava/lang/String;")
        noOpMethod(
            APS_REQUEST, "loadAd",
            "Lcom/amazon/device/ads/DTBAdCallback;IILcom/amazon/device/ads/AdType;"
        )
        noOpMethod(APS_REQUEST, "loadSmartBanner", "Lcom/amazon/device/ads/DTBAdCallback;")
    }
}
