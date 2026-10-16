package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element
import org.w3c.dom.Node

private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
)

private const val ADS_APPLICATION_ID_META = "com.google.android.gms.ads.APPLICATION_ID"
private const val MOBILE_ADS_INIT_PROVIDER = "com.google.android.gms.ads.MobileAdsInitProvider"

/**
 * Patch 3 (analysis list): strip the ad tracking surface from the manifest.
 *
 * Removes the ad identifier permissions, the AdMob application ID meta-data and the
 * MobileAds initialization provider. Depends on "Remove banner ads": without the
 * application ID the ads SDK throws if anything ever initializes it, so this must
 * never be applied while ads are still active.
 */
@Suppress("unused")
val stripAdTrackingPatch = resourcePatch(
    name = "Strip ad tracking permissions and app ID",
    description = "Removes the AdMob application ID, the MobileAds initialization provider " +
        "and the ad identifier permissions (AD_ID and Ad Services) from the manifest. " +
        "Requires 'Remove banner ads'.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)
    dependsOn(removeBannerAdsPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val permissions = document.getElementsByTagName("uses-permission")
            val deadPermissions = mutableListOf<Node>()
            for (i in 0 until permissions.length) {
                val element = permissions.item(i) as Element
                if (element.getAttribute("android:name") in AD_PERMISSIONS) {
                    deadPermissions += element
                }
            }
            deadPermissions.forEach { it.parentNode?.removeChild(it) }

            val application = document
                .getElementsByTagName("application")
                .item(0) as Element
            val deadChildren = mutableListOf<Node>()
            val children = application.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i)
                if (node !is Element) continue
                val name = node.getAttribute("android:name")
                val remove = when (node.tagName) {
                    "meta-data" -> name == ADS_APPLICATION_ID_META
                    "provider" -> name == MOBILE_ADS_INIT_PROVIDER
                    else -> false
                }
                if (remove) deadChildren += node
            }
            deadChildren.forEach { application.removeChild(it) }
        }
    }
}
