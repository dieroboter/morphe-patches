package app.dieroboter.patches.solitaire

import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_SOLITAIRE

private const val UNITY_AD_CONTROL = "Lcom/mobilityware/advertising/UnityAdControl;"
private const val AD_CONTROL = "Lcom/mobilityware/advertising/AdControl2;"
private const val AD_NET_CONTROLLER = "Lcom/mobilityware/advertising/MWAdNetController;"
private const val AD_PARAMS = "Lcom/mobilityware/advertising/AdParams2;"
private const val ADAPTERS = "Lcom/mobilityware/advertising/"

private const val STRING = "Ljava/lang/String;"

/** UnityAdControl void methods that show, load or report ads; all become no-ops. */
private val BRIDGE_NO_OPS = listOf(
    "showBanner" to "I",
    "showBanner" to "II",
    "showBanner" to "II$STRING",
    "showBanner" to "I$STRING",
    "clearBanner" to "",
    "requestNativeAd" to "",
    "requestNativeAd" to STRING,
    "activateRewardedAd" to "",
    "activateRewardedAd" to STRING,
    "clickOnNativeAd" to "",
    "clickOnNativePrivacy" to "",
    // Ad-event telemetry the bridge forwards into the ad stack.
    "logAdData" to "Ljava/util/Map;",
    "logAdEvent" to "${STRING}Ljava/util/HashMap;"
)

/** UnityAdControl boolean methods (show attempts and availability queries); all report false. */
private val BRIDGE_FALSE = listOf(
    "displayAd" to "",
    "displayAd" to STRING,
    "displayAdFromSource" to STRING,
    "displayAdTime" to "I",
    "displayAdTime" to "I$STRING",
    "displayInternalRewardedAd" to "",
    "displayOfferwall" to "",
    "displayRewardedAd" to "",
    "displayRewardedAd" to STRING,
    "displayRewardedAdWithTierName" to STRING,
    "displayRewardedAdWithTierName" to "$STRING$STRING",
    "requestAdFromSource" to STRING,
    "isTimeForAd" to "",
    "isRewardedAdAvailable" to "",
    "isRewardedAdAvailable" to STRING,
    "isInterstitialReady" to "",
    "isOfferwallAvailable" to "",
    "isAdDisplaying" to "",
    "isNativeAdReady" to ""
)

/** AdControl2 void methods; the facade behind the bridge gets the same treatment. */
private val FACADE_NO_OPS = listOf(
    "activateRewardedAd" to "",
    "activateRewardedAd" to STRING,
    "clearBanner" to ""
)

/** AdControl2 boolean methods; all report false. */
private val FACADE_FALSE = listOf(
    "displayAd" to "",
    "displayAd" to STRING,
    "displayAdTime" to "I",
    "displayAdTime" to "I$STRING",
    "displayInternalRewardedAd" to "",
    "displayOfferwall" to "",
    "displayRewardedAd" to "",
    "displayRewardedAd" to STRING,
    "displayRewardedAdWithTierName" to STRING,
    "displayRewardedAdWithTierName" to "$STRING$STRING",
    "getNative" to "Landroid/app/Activity;Lcom/mobilityware/advertising/ACNativeAdRequest;",
    "getNative" to "Landroid/app/Activity;Lcom/mobilityware/advertising/ACNativeAdRequest;$STRING",
    "isTimeForAd" to "",
    "isRewardedAdAvailable" to "",
    "isRewardedAdAvailable" to STRING,
    "isInterstitialReady" to "",
    "isOfferwallAvailable" to "",
    "isAdDisplaying" to "",
    "getInternalAdShowing" to ""
)

/**
 * Every MobilityWare network adapter (plus the abstract base, which covers
 * any adapter without its own override). startNet is where each adapter
 * initializes its ad network SDK (AppLovin MAX, AdMob, Unity Ads, Vungle,
 * Chartboost, Meta, BidMachine, Amazon, Digital Turbine, GAM) and starts
 * loading; forcing it to false means no network ever starts.
 */
private val ADAPTER_CLASSES = listOf(
    "MWAdNetAdapter", "MWAdMobAdapter", "MWAmazonAdapter", "MWAmazonBidderAdapter",
    "MWAppLovinAdapter", "MWBidMachineAdapter", "MWChartboostAdapter",
    "MWDigitalTurbineAdapter", "MWFANAdapter", "MWFastAdAdapter", "MWGAMAdapter",
    "MWMAXAdapter", "MWMWX2Adapter", "MWMWX2NativeAdapter", "MWUnityAdapter",
    "MWVungleAdapter"
)

/**
 * Disable all ads and ad components (bytecode half).
 *
 * Ads are driven from the Unity (C#) side through UnityAdControl, a JNI
 * bridge over AdControl2 / MWAdNetController (MobilityWare ad framework
 * 3.4.9), which fans out to per-network adapters over AppLovin MAX and
 * direct SDK integrations. This patch kills the stack at every layer:
 *
 * - Bridge and facade: every show/request method is a no-op and every
 *   availability query returns false, so the game never asks for or shows a
 *   banner, interstitial, rewarded, native or internal cross-promo ad.
 * - Self-start machinery: AdParams2 is a Thread whose run() fetches the
 *   remote ad configuration that builds the adapter tiers; run() and
 *   onResume() are no-ops, and MWAdNetController.onStart() (the lifecycle
 *   entry that starts the tiers) is a no-op. MWAdNetController's
 *   verifyMAXMediation() (which would open the AppLovin mediation debugger)
 *   is a no-op too.
 * - Adapters: startNet() returns false on every adapter, and the direct
 *   AdMob/GAM/Digital Turbine initialize methods are no-ops, so no ad SDK
 *   initializes even if some path reaches an adapter.
 *
 * Verified against Solitaire 9.8.0.6679: all anchors are content-matched by
 * class, name, parameter types and return type.
 */
@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable all ads and ad components",
    description = "No-ops the MobilityWare ad bridge (UnityAdControl) and facade (AdControl2) " +
        "show/request methods and makes their availability queries return false, so no banner, " +
        "interstitial, rewarded, native or internal cross-promo ad is loaded or shown. Also " +
        "disables the framework's self-start path (AdParams2 config fetch, MWAdNetController.onStart) " +
        "and every network adapter's startNet/initialize entry point, so AppLovin MAX, AdMob and " +
        "the other mediated ad networks are never initialized.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SOLITAIRE)

    execute {
        for ((name, params) in BRIDGE_NO_OPS) noOpMethod(UNITY_AD_CONTROL, name, params)
        for ((name, params) in BRIDGE_FALSE) falseMethod(UNITY_AD_CONTROL, name, params)

        for ((name, params) in FACADE_NO_OPS) noOpMethod(AD_CONTROL, name, params)
        for ((name, params) in FACADE_FALSE) falseMethod(AD_CONTROL, name, params)
        nullMethod(AD_CONTROL, "getBanner", "Landroid/app/Activity;", "Landroid/view/View;")
        nullMethod(AD_CONTROL, "getBanner", "Landroid/app/Activity;$STRING", "Landroid/view/View;")

        noOpMethod(AD_NET_CONTROLLER, "onStart")
        noOpMethod(AD_NET_CONTROLLER, "verifyMAXMediation")
        noOpMethod(AD_PARAMS, "run")
        noOpMethod(AD_PARAMS, "onResume")

        for (adapter in ADAPTER_CLASSES) {
            falseMethod("$ADAPTERS$adapter;", "startNet")
        }
        noOpMethod("${ADAPTERS}MWAdMobAdapter;", "initializeAdmob")
        noOpMethod("${ADAPTERS}MWMWX2Adapter;", "initializeAdmob")
        noOpMethod("${ADAPTERS}MWGAMAdapter;", "initializeGAM")
        noOpMethod("${ADAPTERS}MWDigitalTurbineAdapter;", "initializeDT")
    }
}
