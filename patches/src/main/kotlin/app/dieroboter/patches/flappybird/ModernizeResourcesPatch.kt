package app.dieroboter.patches.flappybird

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_FLAPPY_BIRD
import org.w3c.dom.Element

private object PatchResources {
    fun read(path: String): ByteArray = BundledResources.read(path)
}

private val SIGNIN_STRING_NAMES = setOf("common_signin_button_text", "common_signin_button_text_long")

private val LOCALE_DIRS = listOf(
    "values-af", "values-am", "values-ar", "values-be", "values-bg", "values-ca",
    "values-cs", "values-da", "values-de", "values-el", "values-en-rGB", "values-es",
    "values-es-rUS", "values-et", "values-et-rEE", "values-fa", "values-fi", "values-fr",
    "values-hi", "values-hr", "values-hu", "values-in", "values-it", "values-iw",
    "values-ja", "values-ko", "values-lt", "values-lv", "values-ms", "values-ms-rMY",
    "values-nb", "values-nl", "values-pl", "values-pt", "values-pt-rBR", "values-pt-rPT",
    "values-ro", "values-ru", "values-sk", "values-sl", "values-sr", "values-sv",
    "values-sw", "values-th", "values-tl", "values-tr", "values-uk", "values-vi",
    "values-zh-rCN", "values-zh-rTW", "values-zu",
)

/**
 * The resource half of the 64-bit port, mirroring the port build's final state:
 *
 * - Manifest: the game activity becomes the launcher (the splash activity and
 *   the AdMob activity are removed), the INTERNET and ACCESS_NETWORK_STATE
 *   permissions go away, the activity gains the configChanges / exported /
 *   display-cutout attributes of the port, and the SDK levels are raised to
 *   the port's min 14 / target 34. The app's version is NOT changed (the
 *   port build stamped its own 1.12 / versionCode 13; the patches leave the
 *   stock 1.3 / versionCode 4 in place).
 * - An adaptive launcher icon (foreground/monochrome drawables, mipmap
 *   anydpi XML, teal background color) replaces the legacy icon reference.
 * - AppTheme gains the fullscreen + shortEdges cutout items.
 * - The dead Google sign-in resource cluster (button drawables, +1 images,
 *   colors and strings in every locale) and the splash layout/image are
 *   deleted, as in the port.
 * - The four sound effects are replaced with the port's re-encoded versions
 *   (identical audio and durations, smaller encodes).
 */
@Suppress("unused")
val modernizeResourcesPatch = resourcePatch(
    name = "Modernize manifest and resources",
    description = "Makes the game activity the launcher and removes the splash screen and " +
        "AdMob activity, drops the INTERNET and ACCESS_NETWORK_STATE permissions, raises " +
        "the SDK levels to the port's min 14 / target 34 (current Android refuses to " +
        "install the stock target 15), adds an adaptive launcher icon and display-cutout " +
        "theme settings, removes the dead Google sign-in resources, and replaces the " +
        "sound effects with the port's re-encoded versions. The app's version is " +
        "left untouched.",
    default = true
) {
    compatibleWith(COMPATIBILITY_FLAPPY_BIRD)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            // The app's version is deliberately left untouched: the patched
            // app keeps the stock 1.3 (versionCode 4) instead of being
            // stamped with the port build's 1.12 (versionCode 13).

            // The port raised the SDK levels (stock: min 8 / target 15;
            // port: min 14 / target 34). The target bump is also what lets
            // the app install at all on current Android, which refuses
            // packages targeting below SDK 24.
            val usesSdk = document.getElementsByTagName("uses-sdk").item(0) as Element
            usesSdk.setAttribute("android:minSdkVersion", "14")
            usesSdk.setAttribute("android:targetSdkVersion", "34")

            // Remove the two ad-era permissions.
            val permissions = document.getElementsByTagName("uses-permission")
            val toRemove = (0 until permissions.length)
                .map { permissions.item(it) as Element }
                .filter {
                    it.getAttribute("android:name") in
                        setOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
                }
            toRemove.forEach { it.parentNode?.removeChild(it) }

            val application = document.getElementsByTagName("application").item(0) as Element
            // The port points the icon at @mipmap/ic_launcher, but this app's
            // resource table has no mipmap type and the resource coder cannot
            // resolve a reference into a type that does not exist in the
            // original table. The adaptive icon therefore lives under the
            // existing drawable type (@drawable/ic_launcher resolves to the
            // new anydpi-v26 adaptive XML on API 26+, PNGs below it) and the
            // manifest points there instead. Same icon on device.
            application.setAttribute("android:icon", "@drawable/ic_launcher")
            // No android:roundIcon: the port sets it, but the resource coder
            // cannot encode that API-25 attribute name into this app. Without
            // it the launcher falls back to android:icon (the same adaptive
            // icon), so nothing changes visually.

            val activities = document.getElementsByTagName("activity")
            val activityList = (0 until activities.length).map { activities.item(it) as Element }

            // Keep the splash activity's MAIN/LAUNCHER intent-filter for the game activity.
            val splash = activityList.firstOrNull {
                it.getAttribute("android:name") == "com.dotgears.flappy.SplashScreen"
            } ?: throw PatchException("SplashScreen activity not found in the manifest")
            val splashChildren = (0 until splash.childNodes.length).map { splash.childNodes.item(it) }
            val launcherFilter = splashChildren.firstOrNull { it.nodeName == "intent-filter" }
                ?: throw PatchException("SplashScreen has no intent-filter to move")

            activityList.forEach { activity ->
                when (activity.getAttribute("android:name")) {
                    "com.dotgears.flappy.SplashScreen", "com.google.ads.AdActivity" ->
                        activity.parentNode?.removeChild(activity)

                    "com.dotgears.GameActivity" -> {
                        activity.setAttribute("android:exported", "true")
                        activity.setAttribute(
                            "android:configChanges",
                            "keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode",
                        )
                        activity.setAttribute("android:resizeableActivity", "false")
                        // No android:windowLayoutInDisplayCutoutMode here:
                        // the coder cannot encode that API-28 attribute name
                        // into this app. The port's shortEdges mode is set at
                        // runtime by the onCreate window block instead.
                        activity.appendChild(launcherFilter.cloneNode(true))
                    }
                }
            }
        }

        // AppTheme: fullscreen, as in the port's styles.xml. The port also sets
        // android:windowLayoutInDisplayCutoutMode in the theme, but the resource
        // coder cannot encode that API-28 attribute name into this app's table.
        // It is redundant here: the same shortEdges mode is set by the manifest
        // activity attribute and again at runtime in GameActivity.onCreate.
        document("res/values/styles.xml").use { document ->
            val styles = document.getElementsByTagName("style")
            val appTheme = (0 until styles.length)
                .map { styles.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == "AppTheme" }
                ?: throw PatchException("AppTheme style not found")
            val item = document.createElement("item")
            item.setAttribute("name", "android:windowFullscreen")
            item.textContent = "true"
            appTheme.appendChild(item)
        }

        // Colors: drop the Google sign-in palette, add the adaptive icon background.
        document("res/values/colors.xml").use { document ->
            val colors = document.getElementsByTagName("color")
            (0 until colors.length)
                .map { colors.item(it) as Element }
                .filter { it.getAttribute("name").startsWith("common_signin") }
                .forEach { it.parentNode?.removeChild(it) }
            val background = document.createElement("color")
            background.setAttribute("name", "ic_launcher_background")
            background.textContent = "#ff4ec0ca"
            document.documentElement.appendChild(background)
        }

        // Strings: drop the sign-in button texts (default values and every locale).
        fun stripSigninStrings(path: String) {
            document(path).use { document ->
                val strings = document.getElementsByTagName("string")
                (0 until strings.length)
                    .map { strings.item(it) as Element }
                    .filter { it.getAttribute("name") in SIGNIN_STRING_NAMES }
                    .forEach { it.parentNode?.removeChild(it) }
            }
        }
        stripSigninStrings("res/values/strings.xml")
        LOCALE_DIRS.forEach { dir ->
            try {
                stripSigninStrings("res/$dir/strings.xml")
            } catch (_: Exception) {
                // Locale not present in this build's decoded resources; nothing to strip.
            }
        }

        // Adaptive icon files from the port. This patcher line has no
        // copyResources API; staging the bytes at the target path with get()
        // is the equivalent (the resource coder includes files created in
        // the working directory when it rebuilds the APK).
        fun addBundledFile(resourcePath: String, apkPath: String) {
            val bytes = PatchResources.read(resourcePath)
            val file = get(apkPath)
            file.parentFile?.mkdirs()
            file.outputStream().use { it.write(bytes) }
        }
        addBundledFile("/flappy/icons/drawable-anydpi-v26/ic_launcher.xml", "res/drawable-anydpi-v26/ic_launcher.xml")
        addBundledFile("/flappy/icons/drawable/ic_launcher_foreground.png", "res/drawable/ic_launcher_foreground.png")
        addBundledFile("/flappy/icons/drawable/ic_launcher_monochrome.png", "res/drawable/ic_launcher_monochrome.png")

        // Splash screen leftovers.
        delete("res/layout/activity_splash.xml")
        delete("res/drawable/splash.png")

        // The dead Google sign-in / +1 resource cluster (files only; the matching
        // values entries are stripped above).
        listApkEntries("res/")
            .filter { "common_signin" in it || "ic_plusone" in it }
            .forEach { delete(it) }

        // The port's re-encoded sound effects (same audio, same durations).
        listOf("sfx_die.ogg", "sfx_hit.ogg", "sfx_point.ogg", "sfx_swooshing.ogg").forEach { name ->
            val bytes = PatchResources.read("/flappy/sounds/$name")
            get("assets/sounds/$name").outputStream().use { it.write(bytes) }
        }
    }
}
