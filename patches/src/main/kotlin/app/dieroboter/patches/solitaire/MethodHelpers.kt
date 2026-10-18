package app.dieroboter.patches.solitaire

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * Content-anchored method lookup and gutting helpers shared by the Solitaire
 * bytecode patches. A method is identified by its defining class, name,
 * concatenated parameter types and return type; zero or multiple matches fail
 * the patch with a [PatchException] instead of silently skipping an edit.
 *
 * Gutting prepends an immediate return, the same technique as the Geometry
 * Dash patches: void methods get `return-void`, boolean methods return false
 * and object methods return null. The original body stays in place as dead
 * code behind the return.
 */
internal fun BytecodePatchContext.solitaireMethod(
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
            "Solitaire anchor failed: $classType->$name($parameters)$returnType matched " +
                "${matches.size} methods (expected exactly 1)."
        )
    }
    return matches.single()
}

/** Prepend `return-void` to a void method, making it a no-op. */
internal fun BytecodePatchContext.noOpMethod(classType: String, name: String, parameters: String = "") {
    solitaireMethod(classType, name, parameters, "V").addInstructions(0, "return-void")
}

/** Prepend `return false` to a boolean method. */
internal fun BytecodePatchContext.falseMethod(classType: String, name: String, parameters: String = "") {
    solitaireMethod(classType, name, parameters, "Z").addInstructions(
        0,
        """
            const/4 v0, 0x0
            return v0
        """
    )
}

/** Prepend `return null` to an object-returning method. */
internal fun BytecodePatchContext.nullMethod(
    classType: String,
    name: String,
    parameters: String,
    returnType: String
) {
    solitaireMethod(classType, name, parameters, returnType).addInstructions(
        0,
        """
            const/4 v0, 0x0
            return-object v0
        """
    )
}
