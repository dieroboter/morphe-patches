package app.dieroboter.patches.mediainfo

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_MEDIAINFO

private const val BILLING_MANAGER = "Lif0;"

/**
 * Unlock premium (MediaInfo 26.10).
 *
 * Premium is a supporter subscription (SKU net.mediaarea.mediainfo.inapp1,
 * yearly or lifetime) whose advertised perk is the dark theme plus
 * subscriber features. There is no account system and no server in this
 * app: the entitlement verdict is computed purely from Play Billing,
 * locally, by a singleton billing manager (Lif0;).
 *
 * Lif0 publishes its state through boolean setters onto LiveData-style
 * holders. The subscribed verdict is field c, published exclusively by
 * the setter h(Z)V: the constructor initializes it with h(false), and
 * the purchase-processing path b(List) calls h(true) when a purchase of
 * either subscription SKU is present. Nothing ever calls h(false)
 * again. Every consumer reads field c: the settings fragment (the dark
 * theme gate), the main activity (startup and its options menu), and a
 * menu updater. (Field d is a lifetime-ownership auxiliary published
 * directly by the purchase path and read only alongside c by that menu
 * updater; the gates themselves key on c, so it is left alone. Field b,
 * published by g(Z)V, is the separate billing-ready state and is also
 * untouched.)
 *
 * The patch prepends a single instruction to h(Z)V that overwrites its
 * boolean parameter with true (const/4 p1, 0x1). The original body then
 * publishes Boolean.TRUE onto field c through its own change-guarded
 * path, starting with the constructor's initializing call — so the app
 * is subscribed from birth and no later verdict can disagree. The
 * manager itself, its constructor (an initializer other code relies
 * on), the billing client setup and the purchase flow all run exactly
 * as stock; only the verdict publication is forced.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks MediaInfo's premium (the dark theme and subscriber features). " +
        "The subscription verdict is a purely local Play Billing result held by the app's " +
        "billing manager; this patch makes that verdict permanently 'subscribed'. No " +
        "account or server is involved - the app has neither.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MEDIAINFO)

    execute {
        mediaInfoMethod(BILLING_MANAGER, "h", "Z", "V").addInstructions(
            0,
            "const/4 p1, 0x1"
        )
    }
}
