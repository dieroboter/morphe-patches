package app.dieroboter.patches.solitaire

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_SOLITAIRE
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Ad/tracking permissions removed outright (all six present in 9.8.0.6679). */
private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
    "com.amazon.privacypass.ATTEST"
)

/**
 * ContentProviders whose only job is to initialize an SDK before the app's
 * own code runs: the ad networks' init providers and Firebase's auto-init.
 */
private val INIT_PROVIDERS = setOf(
    "com.applovin.sdk.AppLovinInitProvider",
    "com.google.android.gms.ads.MobileAdsInitProvider",
    "io.bidmachine.BidMachineInitProvider",
    "com.vungle.ads.VungleProvider",
    "com.facebook.ads.AudienceNetworkContentProvider",
    "com.google.firebase.provider.FirebaseInitProvider"
)

/** Services removed: Firebase Sessions and the Google Measurement backend. */
private val DEAD_SERVICES = setOf(
    "com.google.firebase.sessions.SessionLifecycleService",
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService"
)

/** Receivers removed: the Google Measurement backend receiver. */
private val DEAD_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver"
)

/**
 * Firebase component registrars removed from ComponentDiscoveryService's
 * meta-data, so the Analytics, Crashlytics, Remote Config and Sessions
 * components are never registered even if FirebaseApp itself is created by
 * the Unity native plugin. The meta-data android:name values have the form
 * "com.google.firebase.components:<registrar class>".
 */
private val DEAD_FIREBASE_REGISTRARS = setOf(
    "com.google.firebase.analytics.connector.internal.AnalyticsConnectorRegistrar",
    "com.google.firebase.crashlytics.CrashlyticsRegistrar",
    "com.google.firebase.crashlytics.FirebaseCrashlyticsKtxRegistrar",
    "com.google.firebase.crashlytics.ndk.CrashlyticsNdkRegistrar",
    "com.google.firebase.remoteconfig.RemoteConfigRegistrar",
    "com.google.firebase.remoteconfig.FirebaseRemoteConfigKtxRegistrar",
    "com.google.firebase.sessions.FirebaseSessionsRegistrar"
)

/** androidx.startup initializers removed (Unity Ads auto-initialization). */
private val DEAD_STARTUP_INITIALIZERS = setOf(
    "com.unity3d.services.core.configuration.AdsSdkInitializer"
)

private fun Element.androidName() = getAttribute("android:name")

/** Removes every element with the given tag whose android:name is in [names]; returns the count. */
private fun removeNamed(document: Document, tag: String, names: Set<String>): Int {
    val nodes = document.getElementsByTagName(tag)
    val dead = mutableListOf<Node>()
    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as Element
        if (element.androidName() in names) dead += element
    }
    dead.forEach { it.parentNode?.removeChild(it) }
    return dead.size
}

/** Removes meta-data children of [parent] whose android:name ends with one of [suffixes]. */
private fun removeMetaData(parent: Element, suffixes: Set<String>): Int {
    val dead = mutableListOf<Node>()
    val children = parent.childNodes
    for (i in 0 until children.length) {
        val node = children.item(i)
        if (node is Element && node.tagName == "meta-data" &&
            suffixes.any { node.androidName().endsWith(it) }
        ) {
            dead += node
        }
    }
    dead.forEach { it.parentNode?.removeChild(it) }
    return dead.size
}

private fun expect(what: String, actual: Int, expected: Int) {
    if (actual != expected) {
        throw PatchException(
            "Solitaire manifest anchor failed: removed $actual $what (expected $expected)."
        )
    }
}

/**
 * Remove ad and tracking components (manifest half of both patch groups).
 *
 * Structural counterpart to the two bytecode patches: it removes what would
 * otherwise start the ad and analytics stacks before or independently of the
 * game code.
 *
 * - Ad networks: the AppLovin, AdMob, BidMachine, Vungle and Meta init
 *   providers and the Unity Ads androidx.startup initializer, so no ad SDK
 *   auto-initializes at process start.
 * - Firebase: the FirebaseInitProvider (no Java auto-init), the Analytics /
 *   Crashlytics / Remote Config / Sessions registrars inside
 *   ComponentDiscoveryService, the Sessions service, and the Google
 *   Measurement receiver and services that back Firebase Analytics.
 * - Permissions: AD_ID, the three Ad Services permissions, the install
 *   referrer service permission and Amazon's ATTEST permission.
 *
 * Every removal is counted and asserted, so a manifest that no longer
 * matches the analyzed 9.8.0.6679 shape fails the patch instead of silently
 * half-applying.
 */
@Suppress("unused")
val removeAdTrackingComponentsPatch = resourcePatch(
    name = "Remove ad and tracking components",
    description = "Removes from the manifest: the ad networks' auto-init providers (AppLovin, " +
        "AdMob, BidMachine, Vungle, Meta) and the Unity Ads startup initializer, so no ad SDK " +
        "initializes at app start; Firebase's init provider, its Analytics, Crashlytics, Remote " +
        "Config and Sessions component registrations, and the Google Measurement components, so " +
        "Firebase analytics and crash reporting never start; and the ad-tracking permissions " +
        "(AD_ID, the Ad Services permissions, the install referrer permission and Amazon ATTEST).",
    default = true
) {
    compatibleWith(COMPATIBILITY_SOLITAIRE)

    execute {
        document("AndroidManifest.xml").use { document ->
            expect(
                "tracking permissions",
                removeNamed(document, "uses-permission", TRACKING_PERMISSIONS),
                TRACKING_PERMISSIONS.size
            )
            expect(
                "init providers",
                removeNamed(document, "provider", INIT_PROVIDERS),
                INIT_PROVIDERS.size
            )
            expect(
                "dead services",
                removeNamed(document, "service", DEAD_SERVICES),
                DEAD_SERVICES.size
            )
            expect(
                "dead receivers",
                removeNamed(document, "receiver", DEAD_RECEIVERS),
                DEAD_RECEIVERS.size
            )

            // Firebase registrar meta-data inside ComponentDiscoveryService.
            val services = document.getElementsByTagName("service")
            var registrarsRemoved = 0
            for (i in 0 until services.length) {
                val element = services.item(i) as Element
                if (element.androidName() ==
                    "com.google.firebase.components.ComponentDiscoveryService"
                ) {
                    registrarsRemoved += removeMetaData(element, DEAD_FIREBASE_REGISTRARS)
                }
            }
            expect("Firebase registrars", registrarsRemoved, DEAD_FIREBASE_REGISTRARS.size)

            // Unity Ads initializer inside the androidx.startup provider.
            val providers = document.getElementsByTagName("provider")
            var initializersRemoved = 0
            for (i in 0 until providers.length) {
                val element = providers.item(i) as Element
                if (element.androidName() == "androidx.startup.InitializationProvider") {
                    initializersRemoved += removeMetaData(element, DEAD_STARTUP_INITIALIZERS)
                }
            }
            expect("startup initializers", initializersRemoved, DEAD_STARTUP_INITIALIZERS.size)
        }
    }
}
