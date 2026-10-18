package app.dieroboter.patches.solitaire

import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_SOLITAIRE

private const val APPSFLYER_WRAPPER = "Lcom/appsflyer/unity/AppsFlyerAndroidWrapper;"
private const val APPSFLYER_IMPL = "Lcom/appsflyer/internal/AFa1uSDK;"
private const val CONTEXT = "Landroid/content/Context;"
private const val STRING = "Ljava/lang/String;"

/**
 * Disable analytics and tracking (bytecode half).
 *
 * AppsFlyer is the one analytics SDK in this app that is driven from code:
 * the Unity (C#) side calls it through AppsFlyerAndroidWrapper, the only Java
 * caller of AppsFlyerLib.init in the app, and all calls land on the SDK
 * implementation AFa1uSDK. No-oping the wrapper's initSDK / startTracking /
 * trackEvent entry points and the implementation's start / logEvent /
 * logSession methods means AppsFlyer never initializes and sends no session,
 * event or attribution data.
 *
 * Firebase Analytics and Crashlytics are not called from Java at all (they
 * are driven by the Unity native plugin and Firebase auto-initialization), so
 * they are handled structurally by the "Remove ad and tracking components"
 * manifest patch, which strips their auto-init components; the stock manifest
 * already sets their collection flags to disabled.
 */
@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics and tracking",
    description = "No-ops the AppsFlyer Unity wrapper entry points (initSDK, startTracking, " +
        "trackEvent) and the AppsFlyer SDK implementation's start, logEvent and logSession " +
        "methods, so AppsFlyer never initializes and sends no session, event or attribution " +
        "data. Firebase Analytics and Crashlytics are disabled by the manifest patch, which " +
        "removes their auto-init components.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SOLITAIRE)

    execute {
        // JNI surface the game's C# code calls.
        noOpMethod(APPSFLYER_WRAPPER, "initSDK", "$STRING$STRING")
        noOpMethod(APPSFLYER_WRAPPER, "startTracking")
        noOpMethod(APPSFLYER_WRAPPER, "startTracking", "Z$STRING")
        noOpMethod(APPSFLYER_WRAPPER, "trackEvent", "${STRING}Ljava/util/HashMap;")
        noOpMethod(APPSFLYER_WRAPPER, "trackEvent", "${STRING}Ljava/util/HashMap;Z$STRING")

        // SDK core, in case anything reaches it past the wrapper.
        noOpMethod(APPSFLYER_IMPL, "start", CONTEXT)
        noOpMethod(APPSFLYER_IMPL, "start", "$CONTEXT$STRING")
        noOpMethod(
            APPSFLYER_IMPL, "start",
            "$CONTEXT$STRING" + "Lcom/appsflyer/attribution/AppsFlyerRequestListener;"
        )
        noOpMethod(APPSFLYER_IMPL, "logEvent", "$CONTEXT$STRING" + "Ljava/util/Map;")
        noOpMethod(
            APPSFLYER_IMPL, "logEvent",
            "$CONTEXT$STRING" + "Ljava/util/Map;" +
                "Lcom/appsflyer/attribution/AppsFlyerRequestListener;"
        )
        noOpMethod(APPSFLYER_IMPL, "logSession", CONTEXT)
    }
}
