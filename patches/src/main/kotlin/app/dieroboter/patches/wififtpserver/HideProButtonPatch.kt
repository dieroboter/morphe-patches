package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element

/**
 * Patch 4 (analysis list): hide the "Get Pro version" button.
 *
 * The button is declared in activity_main.xml with android:onClick="handlePro"
 * (@id/btnBuy). Setting it gone removes it from the main screen card; the ScrollView
 * content reflows over the freed space.
 */
@Suppress("unused")
val hideProButtonPatch = resourcePatch(
    name = "Hide Get Pro version button",
    description = "Hides the 'Get Pro version' button on the main screen, which only " +
        "opens the paid Pro app's store listing.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        document("res/layout/activity_main.xml").use { document ->
            val buttons = document.getElementsByTagName("Button")
            for (i in 0 until buttons.length) {
                val element = buttons.item(i) as Element
                if (element.getAttribute("android:onClick") == "handlePro" ||
                    element.getAttribute("android:id") == "@id/btnBuy"
                ) {
                    element.setAttribute("android:visibility", "gone")
                }
            }
        }
    }
}
