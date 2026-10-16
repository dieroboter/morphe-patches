package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val MAIN_ACTIVITY = "Lcom/medhaapps/wififtpserver/activity/MainActivity;"
private const val FTP_SERVICE = "Lcom/medhaapps/wififtpserver/service/FTPService;"

/**
 * MainActivity.s()V
 * One-shot, AtomicBoolean-guarded: MobileAds.initialize(context), then v() to build the banner.
 */
object AdsInitFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "s",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/MobileAds;",
            name = "initialize"
        )
    )
)

/**
 * MainActivity.v()V
 * Creates the AdView, sets size from R.string.ad_type, sets the ad unit id below,
 * adds it to @id/adContainer and posts the delayed load runnable.
 */
object BannerBuildFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "v",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("ca-app-pub-5422934368992988/8908724157")
    )
)

/**
 * The GMS consent component's can-request-ads gate, a()Z in an R8-obfuscated class.
 * Identified only by its SharedPreferences keys: returns true when
 * "is_pub_misconfigured" is set or "consent_status" is NOT_REQUIRED (1) or OBTAINED (3).
 * No defining class / name is declared: both change between builds, the strings do not.
 */
object ConsentGateFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("is_pub_misconfigured"),
        string("consent_status")
    )
)

/**
 * The consent info update task, run()V in an R8-obfuscated class.
 * Matched by name + the "consent_status" string it writes after a consent update.
 */
object ConsentUpdateTaskFingerprint : Fingerprint(
    name = "run",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("consent_status")
    )
)

/**
 * The app-wide Firebase Crashlytics wrapper (R8-obfuscated class, static a() accessor,
 * b(String) = log, c(Throwable) = recordException). Identified by the error string in a()
 * for when the Crashlytics component is missing.
 */
object CrashlyticsWrapperFingerprint : Fingerprint(
    filters = listOf(
        string("FirebaseCrashlytics component is not present.")
    )
)

/**
 * FTPService.e(I)V
 * Registers the mDNS/NSD service advertisement ("WiFiFTP", "_ftp._tcp") on the server port.
 */
object NsdRegisterFingerprint : Fingerprint(
    definingClass = FTP_SERVICE,
    name = "e",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("WiFiFTP"),
        string("_ftp._tcp")
    )
)
