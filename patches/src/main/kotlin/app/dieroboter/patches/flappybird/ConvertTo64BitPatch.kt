package app.dieroboter.patches.flappybird

import app.morphe.patcher.patch.resourcePatch
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_FLAPPY_BIRD

/**
 * The core of the 64-bit port.
 *
 * The stock APK ships AndEngine's tiny native helper (libandengine.so, ~11 KB)
 * for armeabi, armeabi-v7a and x86 only, which pins the app to 32-bit ABIs and
 * makes it uninstallable on 64-bit-only devices. The library is an optional
 * accelerator: both of its loaders (BufferUtils and GLES20Fix static
 * initializers) wrap System.loadLibrary("andengine") in a catch for
 * UnsatisfiedLinkError and fall back to pure-Java implementations, so simply
 * deleting the libraries is the whole conversion. The port build shipped and
 * ran on a Pixel 7a in exactly this state.
 */
@Suppress("unused")
val convertTo64BitPatch = resourcePatch(
    name = "Convert to 64-bit",
    description = "Removes the bundled 32-bit native libraries (libandengine.so for armeabi, " +
        "armeabi-v7a and x86). The engine's native loading is exception-guarded in the stock " +
        "code and falls back to its Java implementation, so the game becomes ABI-neutral " +
        "and installs on 64-bit-only devices.",
    default = true
) {
    compatibleWith(COMPATIBILITY_FLAPPY_BIRD)

    execute {
        delete("lib/armeabi/libandengine.so")
        delete("lib/armeabi-v7a/libandengine.so")
        delete("lib/x86/libandengine.so")
    }
}
