package app.dieroboter.patches.mediainfo

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_MEDIAINFO
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element
import org.w3c.dom.Node

/** The two strings that brand the Preferences category as a premium section. */
private val PREMIUM_STRINGS = setOf("premium_title", "premium_summary")

/**
 * Every values file that defines the premium strings in 26.10: the default
 * file plus 14 locale overrides. The device locale selects the override
 * (English renders premium_summary as "Premium subscribers features", not
 * the default text), so blanking only the default file leaves the header
 * visible on any device whose locale has its own definition.
 */
private val PREMIUM_STRING_FILES = listOf(
    "res/values/strings.xml",
    "res/values-cs/strings.xml",
    "res/values-de/strings.xml",
    "res/values-en/strings.xml",
    "res/values-es/strings.xml",
    "res/values-fr/strings.xml",
    "res/values-id/strings.xml",
    "res/values-it/strings.xml",
    "res/values-ja/strings.xml",
    "res/values-pt/strings.xml",
    "res/values-pt-rBR/strings.xml",
    "res/values-ru/strings.xml",
    "res/values-tr/strings.xml",
    "res/values-uk/strings.xml",
    "res/values-zh-rCN/strings.xml"
)

/** res/menu/menu_main.xml with the subscription item removed (stock content, decoded). */
private val MENU_WITHOUT_SUBSCRIBE = """
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@id/action_settings" android:title="@string/settings_title" />
    <item android:id="@id/action_about" android:title="@string/about_title" />
</menu>
""".trimIndent() + "\n"

/** Blanks every premium string defined in [document]; returns how many were blanked. */
private fun blankPremiumStrings(document: Document): Int {
    val nodes = document.getElementsByTagName("string")
    var blanked = 0
    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as Element
        if (element.getAttribute("name") in PREMIUM_STRINGS) {
            element.textContent = ""
            blanked++
        }
    }
    return blanked
}

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
 *   ("Exclusive features for premium subscribers" by default, "Premium
 *   subscribers features" in the English override). The patch unwraps
 *   the category: its three child preferences are promoted to direct
 *   children of the PreferenceScreen, in order, and the category element
 *   is deleted. (Blanking the title alone left the category's section
 *   spacing behind as an empty gap above the settings.) The category
 *   carries nothing but the title and summary - no key, no other
 *   attributes - and the settings fragment looks its preferences up by
 *   their own keys, which works at any depth, so nothing misses it.
 *   Both strings are also blanked in every values file that defines
 *   them - the default file and all 14 locale overrides - so no premium
 *   wording survives anywhere the strings might still be resolved.
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
        "section over the Preferences settings is removed outright (its settings " +
        "move up into the plain list). The settings themselves stay, and the " +
        "subscribe screen remains in the app but is no longer reachable.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MEDIAINFO)

    execute {
        // Blank the Preferences category's premium title and summary in the
        // default values file (the anchor: both strings must be there)...
        val defaultFile = PREMIUM_STRING_FILES.first()
        document(defaultFile).use { document ->
            val blanked = blankPremiumStrings(document)
            if (blanked != PREMIUM_STRINGS.size) {
                throw PatchException(
                    "MediaInfo anchor failed: blanked $blanked of ${PREMIUM_STRINGS.size} " +
                        "premium strings in $defaultFile."
                )
            }
        }
        // ...and in every locale override that defines its own text.
        for (path in PREMIUM_STRING_FILES.drop(1)) {
            try {
                document(path).use { document -> blankPremiumStrings(document) }
            } catch (_: Exception) {
                // Locale not present in this build's decoded resources; nothing to blank.
            }
        }

        // Unwrap the Premium category in the Preferences screen: promote its
        // child preferences into the screen, then delete the category, so no
        // empty section header (or its spacing) remains above them.
        val settingsPath = "res/xml/fragment_settings.xml"
        val settingsFile = get(settingsPath)
        val settingsText = settingsFile.readText()
        if ("PreferenceCategory" !in settingsText || "premium_title" !in settingsText) {
            throw PatchException(
                "MediaInfo anchor failed: staged $settingsPath does not contain the " +
                    "premium PreferenceCategory; refusing to edit an unexpected file."
            )
        }
        val settingsDoc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(settingsFile)
        val categories = settingsDoc.getElementsByTagName("PreferenceCategory")
        if (categories.length != 1) {
            throw PatchException(
                "MediaInfo anchor failed: expected exactly 1 PreferenceCategory in " +
                    "$settingsPath, found ${categories.length}."
            )
        }
        val category = categories.item(0) as Element
        val screen = category.parentNode
            ?: throw PatchException("MediaInfo anchor failed: premium category has no parent.")
        val promoted = mutableListOf<Node>()
        for (i in 0 until category.childNodes.length) {
            val child = category.childNodes.item(i)
            if (child is Element) promoted += child
        }
        if (promoted.size != 3) {
            throw PatchException(
                "MediaInfo anchor failed: expected 3 preferences inside the premium " +
                    "category, found ${promoted.size}."
            )
        }
        // insertBefore moves each node out of the category, keeping order.
        for (child in promoted) screen.insertBefore(child, category)
        screen.removeChild(category)
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8")
        settingsFile.outputStream().use { transformer.transform(DOMSource(settingsDoc), StreamResult(it)) }

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
