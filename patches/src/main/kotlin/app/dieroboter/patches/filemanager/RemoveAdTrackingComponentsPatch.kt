package app.dieroboter.patches.filemanager

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_FILE_MANAGER
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Ad/tracking permissions removed outright (all five present in 3.8.3). */
private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE"
)

/**
 * ContentProviders whose only job is to initialize an SDK before the app's
 * own code runs: AdMob's init provider and Firebase's auto-init.
 */
private val INIT_PROVIDERS = setOf(
    "com.google.android.gms.ads.MobileAdsInitProvider",
    "com.google.firebase.provider.FirebaseInitProvider"
)

/** Firebase measurement and sessions components (services). */
private val TELEMETRY_SERVICES = setOf(
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.firebase.sessions.SessionLifecycleService"
)

/** Firebase measurement components (receivers). */
private val TELEMETRY_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver"
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

private fun expect(what: String, actual: Int, expected: Int) {
    if (actual != expected) {
        throw PatchException(
            "File Manager manifest anchor failed: removed $actual $what (expected $expected)."
        )
    }
}

/**
 * Remove ad and tracking components (File Manager 3.8.3, manifest).
 *
 * Structural counterpart to the bytecode patches: it removes what would
 * otherwise start the ad and analytics stacks before or independently of
 * the app code.
 *
 * - Permissions: AD_ID, the three Ad Services permissions and the install
 *   referrer service permission.
 * - Providers: the AdMob and Firebase init providers, so neither SDK
 *   auto-initializes at process start (Firebase initializes lazily on
 *   first use instead; the MobileAdsInitProvider runs before any app
 *   code, so not even a premium license stops AdMob from waking up -
 *   removing the provider does).
 * - Telemetry components: the Firebase measurement service, job service
 *   and receiver, and the Firebase Sessions lifecycle service.
 *
 * The Firebase component registrars inside ComponentDiscoveryService are
 * deliberately not touched: they are inert metadata. Firebase's data flow
 * in this app is dead anyway (its event sinks are disabled by the
 * analytics patch, and the FirebaseInitProvider removal stops auto-init).
 *
 * Every removal is counted and asserted, so a manifest that no longer
 * matches the analyzed 3.8.3 shape fails the patch instead of silently
 * half-applying.
 */
@Suppress("unused")
val removeAdTrackingComponentsPatch = resourcePatch(
    name = "Remove ad and tracking components",
    description = "Removes from the manifest: the AdMob and Firebase auto-init providers " +
        "and Firebase's measurement and sessions components, so those SDKs do not start " +
        "at app start; and the ad-tracking permissions (AD_ID, the Ad Services permissions " +
        "and the install referrer permission).",
    default = true
) {
    compatibleWith(COMPATIBILITY_FILE_MANAGER)

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
                "telemetry services",
                removeNamed(document, "service", TELEMETRY_SERVICES),
                TELEMETRY_SERVICES.size
            )
            expect(
                "telemetry receivers",
                removeNamed(document, "receiver", TELEMETRY_RECEIVERS),
                TELEMETRY_RECEIVERS.size
            )
        }
    }
}
