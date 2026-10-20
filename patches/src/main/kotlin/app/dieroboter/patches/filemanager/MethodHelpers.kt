package app.dieroboter.patches.filemanager

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * Content-anchored method lookup and gutting helpers shared by the File
 * Manager bytecode patches. A method is identified by its defining class,
 * name, concatenated parameter types and return type; zero or multiple
 * matches fail the patch with a [PatchException] instead of silently
 * skipping an edit.
 *
 * Gutting prepends a return instruction, leaving the original body as dead
 * code behind it (the same technique as the Geometry Dash and Klondike
 * patches). Only pure query/sink methods are targeted: no initializer,
 * constructor or singleton factory is ever touched, so nothing another
 * method dereferences is removed.
 */
internal fun BytecodePatchContext.fileManagerMethod(
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
            "File Manager anchor failed: $classType->$name($parameters)$returnType matched " +
                "${matches.size} methods (expected exactly 1)."
        )
    }
    return matches.single()
}

/** Prepend `return-void` to a void method, making it a no-op. */
internal fun BytecodePatchContext.noOpMethod(classType: String, name: String, parameters: String = "") {
    fileManagerMethod(classType, name, parameters, "V").addInstructions(0, "return-void")
}

/** Prepend a constant `true` return to a boolean method. */
internal fun BytecodePatchContext.trueMethod(classType: String, name: String, parameters: String = "") {
    fileManagerMethod(classType, name, parameters, "Z").addInstructions(
        0,
        "const/4 v0, 0x1\nreturn v0"
    )
}
