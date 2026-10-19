package app.dieroboter.patches.klondike

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_KLONDIKE
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Ad/tracking permissions removed outright (all five present in 5.9). */
private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE"
)

/**
 * ContentProviders whose only job is to initialize an SDK before the app's
 * own code runs: the ad networks' init providers, Facebook's init provider
 * and Firebase's auto-init.
 */
private val INIT_PROVIDERS = setOf(
    "com.google.android.gms.ads.MobileAdsInitProvider",
    "com.facebook.ads.AudienceNetworkContentProvider2",
    "com.doodlemobile.helper.AudienceNetworkContentProvider2",
    "com.facebook.internal.FacebookInitProvider2",
    "com.google.firebase.provider.FirebaseInitProvider"
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
            "Klondike manifest anchor failed: removed $actual $what (expected $expected)."
        )
    }
}

/**
 * Remove ad and tracking components (Klondike Solitaire 5.9, manifest).
 *
 * Structural counterpart to the two bytecode patches: it removes what would
 * otherwise start the ad and analytics stacks before or independently of
 * the game code.
 *
 * - Permissions: AD_ID, the three Ad Services permissions and the install
 *   referrer service permission.
 * - Providers: the AdMob, Audience Network (both the Meta and the
 *   DoodleMobile-helper copies), Facebook and Firebase init providers, so
 *   none of those SDKs auto-initialize at process start.
 *
 * The Firebase component registrars inside ComponentDiscoveryService are
 * deliberately not touched: they are inert metadata, and removing component
 * registrations is the change that crashed the MobilityWare Solitaire
 * build on device. Firebase's data flow in this app is dead anyway (its
 * only consumer, DoodleBI, is disabled by the analytics patch, and the
 * FirebaseInitProvider removal stops auto-init).
 *
 * Every removal is counted and asserted, so a manifest that no longer
 * matches the analyzed 5.9 shape fails the patch instead of silently
 * half-applying.
 */
@Suppress("unused")
val removeAdTrackingComponentsPatch = resourcePatch(
    name = "Remove ad and tracking components",
    description = "Removes from the manifest: the ad networks' and Facebook's auto-init " +
        "providers (AdMob, Audience Network x2, Facebook) and Firebase's init provider, so " +
        "none of those SDKs initialize at app start; and the ad-tracking permissions (AD_ID, " +
        "the Ad Services permissions and the install referrer permission).",
    default = true
) {
    compatibleWith(COMPATIBILITY_KLONDIKE)

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
        }
    }
}
