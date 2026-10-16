package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER
import org.w3c.dom.Element

/**
 * Internal resource step for "Disable Crashlytics reporting":
 * the documented Firebase kill switch, evaluated by the SDK itself at startup.
 */
private val disableCrashlyticsCollectionResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            val metaData = document.createElement("meta-data")
            metaData.setAttribute("android:name", "firebase_crashlytics_collection_enabled")
            metaData.setAttribute("android:value", "false")
            application.appendChild(metaData)
        }
    }
}

/**
 * Patch 6 (analysis list): disable Firebase Crashlytics reporting.
 *
 * Two layers:
 * - The manifest flag turns off Crashlytics (and Firebase Sessions) data collection,
 *   which also stops the SDK's own uncaught-exception reporting.
 * - The app's Crashlytics wrapper methods b(String) log / c(Throwable) recordException
 *   are emptied, so the app stops sending its breadcrumbs (server URL, SSL mode,
 *   start/stop events) and caught exceptions. The wrapper itself is left in place:
 *   its accessor can return null when the component is absent and callers do not
 *   null-check it, so removing the component instead would risk crashes.
 */
@Suppress("unused")
val disableCrashlyticsPatch = bytecodePatch(
    name = "Disable Crashlytics reporting",
    description = "Disables Firebase Crashlytics data collection and empties the app's " +
        "Crashlytics log/exception calls, which otherwise report the server URL and " +
        "configuration on every start and stop.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)
    dependsOn(disableCrashlyticsCollectionResourcePatch)

    execute {
        val wrapperClassType = CrashlyticsWrapperFingerprint.method.definingClass
        val wrapperClass = mutableClassDefBy(wrapperClassType)

        wrapperClass.methods
            .filter { method ->
                (method.name == "b" && method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")) ||
                    (method.name == "c" && method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/Throwable;"))
            }
            .forEach { method ->
                method.addInstructions(0, "return-void")
            }
    }
}
