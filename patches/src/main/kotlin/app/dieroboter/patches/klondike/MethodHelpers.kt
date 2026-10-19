package app.dieroboter.patches.klondike

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * Content-anchored method lookup and gutting helpers shared by the Klondike
 * bytecode patches. A method is identified by its defining class, name,
 * concatenated parameter types and return type; zero or multiple matches fail
 * the patch with a [PatchException] instead of silently skipping an edit.
 *
 * All gutting targets in this app are void methods: prepending `return-void`
 * makes them no-ops, leaving the original body as dead code behind the
 * return (the same technique as the Geometry Dash patches).
 */
internal fun BytecodePatchContext.klondikeMethod(
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
            "Klondike anchor failed: $classType->$name($parameters)$returnType matched " +
                "${matches.size} methods (expected exactly 1)."
        )
    }
    return matches.single()
}

/** Prepend `return-void` to a void method, making it a no-op. */
internal fun BytecodePatchContext.noOpMethod(classType: String, name: String, parameters: String = "") {
    klondikeMethod(classType, name, parameters, "V").addInstructions(0, "return-void")
}
