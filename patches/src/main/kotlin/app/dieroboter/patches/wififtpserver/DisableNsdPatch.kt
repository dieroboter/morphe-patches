package app.dieroboter.patches.wififtpserver

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_WIFI_FTP_SERVER

/**
 * Patch 11 (analysis list): disable the mDNS/NSD advertisement.
 *
 * FTPService.e(I)V registers the server with Android NSD as "WiFiFTP" on
 * "_ftp._tcp" whenever the server starts. Returning early stops the broadcast;
 * the FTP server itself is unaffected and clients can still connect by IP.
 * The matching unregister call in the stop path is already exception-guarded.
 */
@Suppress("unused")
val disableNsdAdvertisementPatch = bytecodePatch(
    name = "Disable mDNS service advertisement",
    description = "Stops the server from announcing itself as 'WiFiFTP' over mDNS/NSD " +
        "(_ftp._tcp) on the local network. Direct connections by IP address are unaffected.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WIFI_FTP_SERVER)

    execute {
        NsdRegisterFingerprint.method.addInstructions(0, "return-void")
    }
}
