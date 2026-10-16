package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Patch 9 (analysis list): remove the "Rate this app" menu item (@id/action_rate)
 * from the main screen menu. The Settings item is left untouched.
 */
@Suppress("unused")
val removeRateMenuItemPatch = resourcePatch(
    name = "Remove Rate this app menu item",
    description = "Removes the 'Rate this app' item, which opens the app's store listing, " +
        "from the main screen menu.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        document("res/menu/main.xml").use { document ->
            val items = document.getElementsByTagName("item")
            val deadItems = mutableListOf<Node>()
            for (i in 0 until items.length) {
                val element = items.item(i) as Element
                if (element.getAttribute("android:id") == "@id/action_rate") {
                    deadItems += element
                }
            }
            deadItems.forEach { it.parentNode?.removeChild(it) }
        }
    }
}
