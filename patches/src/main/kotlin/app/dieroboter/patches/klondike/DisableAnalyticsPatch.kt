package app.dieroboter.patches.klondike

import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_KLONDIKE

private const val APPSFLYER = "Lcom/appsflyer/internal/AFa1cSDK;"
private const val FB_EVENTS = "Lcom/facebook/appevents/AppEventsLogger;"
private const val DOODLE_BI = "Lcom/doodlemobile/doodle_bi/DoodleBI;"

private const val CONTEXT = "Landroid/content/Context;"
private const val STRING = "Ljava/lang/String;"
private const val MAP = "Ljava/util/Map;"

/**
 * Disable analytics and tracking (Klondike Solitaire 5.9).
 *
 * Three reporting channels, all Java, all reached through named public APIs:
 *
 * - AppsFlyer 6.12.4: the public AppsFlyerLib class is abstract; the patch
 *   no-ops the concrete implementation behind it (internal AFa1cSDK):
 *   start() (all 3 overloads) and logEvent() (both overloads). init() is
 *   deliberately left alone: it only stores configuration and returns the
 *   singleton (callers chain off the return value), while every
 *   data-bearing call goes through start/logEvent.
 * - Facebook AppEvents: activateApp() (all 4 overloads), logEvent() (all 6),
 *   logPurchase() (all 3) and setUserData() (both) are no-oped.
 * - DoodleBI, DoodleMobile's own BI layer and the game's only Firebase
 *   consumer: the generic logEvent/logOnlyOnceEvent sinks, all ten
 *   Log/_Log ad-revenue and LTV event methods, both purchase recorders and
 *   commitPendingEvents() are no-oped. Its onCreate() initializers are
 *   deliberately kept: they create the singleton the launcher activity
 *   dereferences at startup (no-oping them crashed v1.4.0-dev.3 at launch
 *   with an NPE, per the on-device trace). Initialization sends nothing by
 *   itself; with the sinks dead, Firebase Analytics has no sender left in
 *   the app (verified: no other code calls FirebaseAnalytics.logEvent).
 *
 * 35 methods in total, all void, all prepended with return-void by
 * [noOpMethod]; every target is asserted to match exactly one method.
 */
@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics and tracking",
    description = "Disables AppsFlyer (session start and event logging), Facebook AppEvents " +
        "(activation, events, purchases, user data) and DoodleBI, the game's own reporting " +
        "layer and its only Firebase Analytics consumer. No usage or ad-revenue events " +
        "leave the device through these channels.",
    default = true
) {
    compatibleWith(COMPATIBILITY_KLONDIKE)

    execute {
        // --- AppsFlyer ---
        noOpMethod(APPSFLYER, "start", CONTEXT)
        noOpMethod(APPSFLYER, "start", CONTEXT + STRING)
        noOpMethod(
            APPSFLYER, "start",
            CONTEXT + STRING + "Lcom/appsflyer/attribution/AppsFlyerRequestListener;"
        )
        noOpMethod(APPSFLYER, "logEvent", CONTEXT + STRING + MAP)
        noOpMethod(
            APPSFLYER, "logEvent",
            CONTEXT + STRING + MAP + "Lcom/appsflyer/attribution/AppsFlyerRequestListener;"
        )

        // --- Facebook AppEvents ---
        noOpMethod(FB_EVENTS, "activateApp", "Landroid/app/Application;")
        noOpMethod(FB_EVENTS, "activateApp", "Landroid/app/Application;" + STRING)
        noOpMethod(FB_EVENTS, "activateApp", CONTEXT)
        noOpMethod(FB_EVENTS, "activateApp", CONTEXT + STRING)
        noOpMethod(
            FB_EVENTS, "logEvent",
            "Lcom/facebook/appevents/AppEvent;Lcom/facebook/appevents/AccessTokenAppIdPair;"
        )
        noOpMethod(
            FB_EVENTS, "logEvent",
            STRING + "Ljava/lang/Double;Landroid/os/Bundle;ZLjava/util/UUID;"
        )
        noOpMethod(FB_EVENTS, "logEvent", STRING)
        noOpMethod(FB_EVENTS, "logEvent", STRING + "D")
        noOpMethod(FB_EVENTS, "logEvent", STRING + "DLandroid/os/Bundle;")
        noOpMethod(FB_EVENTS, "logEvent", STRING + "Landroid/os/Bundle;")
        noOpMethod(
            FB_EVENTS, "logPurchase",
            "Ljava/math/BigDecimal;Ljava/util/Currency;Landroid/os/Bundle;Z"
        )
        noOpMethod(FB_EVENTS, "logPurchase", "Ljava/math/BigDecimal;Ljava/util/Currency;")
        noOpMethod(
            FB_EVENTS, "logPurchase",
            "Ljava/math/BigDecimal;Ljava/util/Currency;Landroid/os/Bundle;"
        )
        noOpMethod(FB_EVENTS, "setUserData", "Landroid/os/Bundle;")
        noOpMethod(
            FB_EVENTS, "setUserData",
            STRING + STRING + STRING + STRING + STRING + STRING + STRING + STRING + STRING + STRING
        )

        // --- DoodleBI (DoodleMobile BI / Firebase consumer) ---
        // Its onCreate() initializers are deliberately NOT touched: they
        // create the DoodleBI singleton, which KlondikeActivity dereferences
        // in its own onCreate (getInstance().addAdditional...Behavior).
        // No-oping them crashed the game at launch with an NPE
        // (v1.4.0-dev.3, on-device trace). Initialization is harmless with
        // every event sink below dead.
        noOpMethod(DOODLE_BI, "logEvent", STRING + MAP)
        noOpMethod(DOODLE_BI, "logOnlyOnceEvent", STRING + STRING + MAP)
        noOpMethod(DOODLE_BI, "LogAdditionalAfAd", "III")
        noOpMethod(DOODLE_BI, "LogAf7DaysAd", "I")
        noOpMethod(DOODLE_BI, "LogAfAdViewRevenueEvent", "F")
        noOpMethod(DOODLE_BI, "LogAroFirebaseAdRevenueEvent", "F")
        noOpMethod(DOODLE_BI, "LogFbvoAppsflyerAdRevenueEvent", "F")
        noOpMethod(DOODLE_BI, "LogTaichiTroasFirebaseAdRevenueEvent", "F")
        noOpMethod(DOODLE_BI, "LogTaichiTroasFirebaseAdRevenueEvent2", "FF")
        noOpMethod(DOODLE_BI, "_LogAdditionalAfAd2", "III")
        noOpMethod(DOODLE_BI, "_LogAdditionalCustomEvent", "I" + STRING + "I")
        noOpMethod(DOODLE_BI, "_LogLtv", "II")
        noOpMethod(DOODLE_BI, "recordInAppPurchase", "FI" + STRING + STRING)
        noOpMethod(DOODLE_BI, "recordInAppPurchaseNoCheck", "FI")
        noOpMethod(DOODLE_BI, "commitPendingEvents")
    }
}
