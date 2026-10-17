package app.dieroboter.patches.world

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_GEOMETRY_DASH_WORLD
import org.w3c.dom.Element
import org.w3c.dom.Node

private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
)

/**
 * Candidate 5: strip the ad identifier permissions from the manifest.
 *
 * Removes AD_ID and the three Ad Services permissions (ad ID, attribution,
 * topics), so the ad SDKs in the mediation stack get no advertising
 * identifier. READ_PHONE_STATE is deliberately left in place (it is the ad
 * SDKs' fallback device identifier, but stripping it is a separate call).
 */
@Suppress("unused")
val stripAdPermissionsPatch = resourcePatch(
    name = "Strip ad tracking permissions",
    description = "Removes the ad identifier permissions (AD_ID and the Ad Services " +
        "permissions: ad ID, attribution and topics) from the manifest.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GEOMETRY_DASH_WORLD)

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
