package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Remove the "Contact developer" menu item (@id/action_contact_dev) from the
 * main screen menu. Added by the app in 2.4.1. The Settings item is left
 * untouched. Fails loud if the item is absent, so a future app update that
 * renames or removes it cannot silently turn this patch into a no-op.
 */
@Suppress("unused")
val removeContactDevMenuItemPatch = resourcePatch(
    name = "Remove Contact developer menu item",
    description = "Removes the 'Contact developer' item from the main screen menu.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        document("res/menu/main.xml").use { document ->
            val items = document.getElementsByTagName("item")
            val deadItems = mutableListOf<Node>()
            for (i in 0 until items.length) {
                val element = items.item(i) as Element
                if (element.getAttribute("android:id") == "@id/action_contact_dev") {
                    deadItems += element
                }
            }
            if (deadItems.isEmpty()) {
                throw PatchException(
                    "Could not find the 'Contact developer' menu item (@id/action_contact_dev) " +
                        "in res/menu/main.xml"
                )
            }
            deadItems.forEach { it.parentNode?.removeChild(it) }
        }
    }
}
