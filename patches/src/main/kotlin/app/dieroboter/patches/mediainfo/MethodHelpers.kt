package app.dieroboter.patches.mediainfo

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * Content-anchored method lookup shared by the MediaInfo bytecode
 * patches. A method is identified by its defining class, name,
 * concatenated parameter types and return type; zero or multiple
 * matches fail the patch with a [PatchException] instead of silently
 * skipping an edit. MediaInfo's app code is R8-obfuscated into short
 * class names, so the anchors are obfuscated names pinned to this exact
 * version (26.10) by the patch compatibility.
 */
internal fun BytecodePatchContext.mediaInfoMethod(
    classType: String,
    name: String,
    parameters: String,
    returnType: String
): MutableMethod {
    val clazz = mutableClassDefBy(classType)
    val matches = clazz.methods.filter {
        it.name == name && it.returnType == returnType &&
            it.parameterTypes.joinToString(separator = "") == parameters
    }
    if (matches.size != 1) {
        throw PatchException(
            "MediaInfo anchor failed: $classType->$name($parameters)$returnType matched " +
                "${matches.size} methods (expected exactly 1)."
        )
    }
    return matches.single()
}
