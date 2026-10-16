package app.dieroboter.patches.videotogif

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF
import org.w3c.dom.Element
import org.w3c.dom.Node

private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
)

/**
 * Candidate 4: strip the ad identifier permissions from the manifest.
 *
 * Removes AD_ID and the three Ad Services permissions. With "Unlock premium"
 * on, ads never render; this removes the tracking identifiers' permission
 * surface itself. No dependency is declared: unlike the AdMob app ID in the
 * WiFi FTP patch, nothing here makes an SDK throw when the permission is
 * absent — the ad SDKs simply get no identifier.
 */
@Suppress("unused")
val stripAdPermissionsPatch = resourcePatch(
    name = "Strip ad tracking permissions",
    description = "Removes the ad identifier permissions (AD_ID and the Ad Services " +
        "permissions: ad ID, attribution and topics) from the manifest.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

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
        }
    }
}
