package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element

private const val ICON_BLUE = "#1C9CEC"

private const val FOLDER_PATH =
    "M27.48,73.72 L80.52,73.72 A4.68,4.68 0 0 0 85.20,69.04 L85.20,46.42 A4.68,4.68 0 0 0 80.52,41.74 L52.44,41.74 L52.44,39.40 A3.12,3.12 0 0 0 49.32,36.28 L26.70,36.28 A3.90,3.90 0 0 0 22.80,40.18 L22.80,69.04 A4.68,4.68 0 0 0 27.48,73.72 Z"

private const val SECTOR_1 =
    "M37.45,54.21 A20.20,20.20 0 0 1 70.55,54.21 L67.48,56.36 A16.46,16.46 0 0 0 40.52,56.36 Z"
private const val SECTOR_2 =
    "M41.93,57.34 A14.74,14.74 0 0 1 66.07,57.34 L63.01,59.49 A11.00,11.00 0 0 0 44.99,59.49 Z"
private const val SECTOR_3 =
    "M46.39,60.47 A9.28,9.28 0 0 1 61.61,60.47 L58.54,62.63 A5.54,5.54 0 0 0 49.46,62.63 Z"
private const val DOT_PATH =
    "M50.57,65.80 A3.43,3.43 0 1 1 57.43,65.80 A3.43,3.43 0 1 1 50.57,65.80 Z"

private val FOREGROUND_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="$FOLDER_PATH" />
    <path
        android:fillColor="$ICON_BLUE"
        android:pathData="$SECTOR_1" />
    <path
        android:fillColor="$ICON_BLUE"
        android:pathData="$SECTOR_2" />
    <path
        android:fillColor="$ICON_BLUE"
        android:pathData="$SECTOR_3" />
    <path
        android:fillColor="$ICON_BLUE"
        android:pathData="$DOT_PATH" />
</vector>
"""

private val MONOCHROME_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FF000000"
        android:fillType="evenOdd"
        android:pathData="$FOLDER_PATH $SECTOR_1 $SECTOR_2 $SECTOR_3 $DOT_PATH" />
</vector>
"""

private const val ADAPTIVE_ICON_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

/**
 * Material You icon.
 *
 * The stock icon is a legacy 144px PNG (@drawable/logo, yellow folder) with no
 * adaptive icon, so launchers shrink it into a white circle. This patch adds a
 * real adaptive icon (background color #1C9CEC, the app's accent; white folder
 * foreground with the WiFi arcs knocked out in blue; monochrome layer for
 * Material You themed icons) and points the manifest's icon/roundIcon at it.
 * The stock logo.png entries are left in place but no longer referenced by the
 * launcher icon.
 */
@Suppress("unused")
val materialYouIconPatch = resourcePatch(
    name = "Material You icon",
    description = "Replaces the legacy launcher icon with a Material You adaptive icon: " +
        "white folder and WiFi glyph on the app's blue, with a monochrome layer for " +
        "themed icons.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        document("res/values/colors.xml").use { document ->
            val color = document.createElement("color")
            color.setAttribute("name", "ic_launcher_background")
            color.textContent = ICON_BLUE
            document.documentElement.appendChild(color)
        }

        fun writeResource(path: String, content: String) {
            val file = get(path, copy = false)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }

        writeResource("res/drawable/ic_launcher_foreground.xml", FOREGROUND_XML)
        writeResource("res/drawable/ic_launcher_monochrome.xml", MONOCHROME_XML)
        writeResource("res/drawable-anydpi-v26/ic_launcher.xml", ADAPTIVE_ICON_XML)

        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element
            application.setAttribute("android:icon", "@drawable/ic_launcher")
            application.setAttribute("android:roundIcon", "@drawable/ic_launcher")
        }
    }
}
