package app.dieroboter.patches.videotogif

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_VIDEO_TO_GIF
import org.w3c.dom.Element

private val HIDDEN_SETTINGS_KEYS = setOf(
    "pref_support",
    "pref_rate_us",
    "pref_share_app"
)

/**
 * Hide three rows of the PremiumHelper settings screen.
 *
 * The settings screen is inflated from res/xml/ph_settings.xml
 * (PremiumHelper 5.3.2). The "VIP Customer Support" row
 * (PremiumSupportPreference, key pref_support), the "Rate us" row
 * (RateUsPreference, key pref_rate_us) and the "Share app" row
 * (SafeClickPreference, key pref_share_app) each get
 * app:isPreferenceVisible="false" — the same attribute the file itself
 * already uses to keep "Manage subscriptions" hidden until it applies.
 *
 * The rows stay in the hierarchy: the settings fragment
 * (PhSettingsFragment) looks every key up with findPreference and
 * null-checks the results, and its only setVisible call targets
 * pref_delete_account, so nothing re-shows these rows and nothing
 * dereferences a missing preference. Hiding the support row affects
 * only the settings screen — the support button on the home screen's
 * bar is a separate view in the main activity.
 *
 * The "Rate us" row is additionally hidden by "Disable rate prompts"
 * (a constructor-level setVisible(false)); this patch owns the row
 * independently of that one. The patch fails loudly if any of the
 * three keys is absent, so an upstream settings rework cannot turn it
 * into a silent no-op.
 */
@Suppress("unused")
val hideSettingsButtonsPatch = resourcePatch(
    name = "Hide settings buttons",
    description = "Hides the 'VIP Customer Support', 'Rate us' and 'Share app' rows " +
        "in the settings screen. The support button on the home screen is not affected.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VIDEO_TO_GIF)

    execute {
        document("res/xml/ph_settings.xml").use { document ->
            val elements = document.getElementsByTagName("*")
            val hidden = mutableSetOf<String>()
            for (i in 0 until elements.length) {
                val element = elements.item(i) as Element
                val key = element.getAttribute("app:key")
                    .ifEmpty { element.getAttribute("android:key") }
                if (key in HIDDEN_SETTINGS_KEYS) {
                    element.setAttribute("app:isPreferenceVisible", "false")
                    hidden += key
                }
            }
            val missing = HIDDEN_SETTINGS_KEYS - hidden
            if (missing.isNotEmpty()) {
                throw PatchException("Settings rows not found in ph_settings.xml: $missing")
            }
        }
    }
}
