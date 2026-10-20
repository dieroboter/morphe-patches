package app.dieroboter.patches.mediainfo

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_MEDIAINFO
import org.w3c.dom.Element

/** The two strings that brand the Preferences category as a premium section. */
private val PREMIUM_STRINGS = setOf("premium_title", "premium_summary")

/** res/menu/menu_main.xml with the subscription item removed (stock content, decoded). */
private val MENU_WITHOUT_SUBSCRIBE = """
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@id/action_settings" android:title="@string/settings_title" />
    <item android:id="@id/action_about" android:title="@string/about_title" />
</menu>
""".trimIndent() + "\n"

/**
 * Remove premium references (MediaInfo 26.10).
 *
 * Two user-visible premium surfaces exist, and both are plain resources:
 *
 * - The main screen's overflow menu (res/menu/menu_main.xml) holds three
 *   items; the middle one, @id/action_subscribe, is the Subscribe /
 *   Manage subscription entry and the only route into SubscribeActivity.
 *   This patch rewrites the menu without it. Removal is safe: the only
 *   code that touches the item is the menu updater (i80), which observes
 *   the billing manager's state holders and swaps the item's title and
 *   visibility - and it null-checks every findItem result before use, so
 *   it tolerates the item's absence. SubscribeActivity stays in the APK,
 *   unreachable.
 * - The Preferences screen (res/xml/fragment_settings.xml) groups
 *   Language, Translate reports and Dark mode under a PreferenceCategory
 *   titled by the strings premium_title ("Premium") and premium_summary
 *   ("Exclusive features for premium subscribers"). The patch blanks both
 *   strings, so the category header renders as nothing and the three
 *   settings - which are real settings and stay - sit in a plain list.
 *   The settings fragment looks its preferences up by their own keys and
 *   never references the category, so blanking its title is safe.
 *
 * The menu rewrite is guarded: if the staged menu does not mention
 * action_subscribe, the patch fails loudly instead of overwriting an
 * unexpected file.
 */
@Suppress("unused")
val removePremiumReferencesPatch = resourcePatch(
    name = "Remove premium references",
    description = "Removes the visible premium branding: the Subscribe / Manage " +
        "subscription entry is deleted from the main screen's menu, and the Premium " +
        "header over the Preferences settings is blanked. The settings themselves stay, " +
        "and the subscribe screen remains in the app but is no longer reachable.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MEDIAINFO)

    execute {
        // Blank the Preferences category's premium title and summary.
        document("res/values/strings.xml").use { document ->
            val nodes = document.getElementsByTagName("string")
            var blanked = 0
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                if (element.getAttribute("name") in PREMIUM_STRINGS) {
                    element.textContent = ""
                    blanked++
                }
            }
            if (blanked != PREMIUM_STRINGS.size) {
                throw PatchException(
                    "MediaInfo anchor failed: blanked $blanked of ${PREMIUM_STRINGS.size} " +
                        "premium strings in res/values/strings.xml."
                )
            }
        }

        // Delete the subscription entry from the main menu.
        val menuFile = get("res/menu/menu_main.xml")
        val staged = menuFile.readText()
        if ("action_subscribe" !in staged) {
            throw PatchException(
                "MediaInfo anchor failed: staged res/menu/menu_main.xml does not mention " +
                    "action_subscribe; refusing to overwrite an unexpected menu."
            )
        }
        menuFile.outputStream().use { it.write(MENU_WITHOUT_SUBSCRIBE.toByteArray(Charsets.UTF_8)) }
    }
}
