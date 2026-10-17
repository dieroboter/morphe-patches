package app.dieroboter.patches.flappybird

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Indices of instructions carrying the given narrow literal
 * (const/4, const/16, const/high16, rsub-int, add-int/lit8, add-int/lit16, ...),
 * optionally restricted to one opcode.
 */
internal fun MutableMethod.literalIndices(literal: Int, opcode: Opcode? = null): List<Int> =
    implementation!!.instructions.indices.filter { i ->
        val insn = implementation!!.instructions[i]
        insn is NarrowLiteralInstruction && insn.narrowLiteral == literal &&
            (opcode == null || insn.opcode == opcode)
    }

/** Same as [literalIndices] but throws unless exactly [count] matches exist. */
internal fun MutableMethod.requireLiteralIndices(
    literal: Int,
    opcode: Opcode? = null,
    count: Int = 1,
): List<Int> {
    val found = literalIndices(literal, opcode)
    if (found.size != count) {
        throw PatchException(
            "Expected $count instruction(s) with literal $literal" +
                (opcode?.let { " and opcode $it" } ?: "") +
                " in $definingClass->$name$parameterTypes, found ${found.size}",
        )
    }
    return found
}

/** Reference string of an instruction, e.g. "Lcom/x;->m()V", or null if it has none. */
internal fun Instruction.refString(): String? =
    (this as? ReferenceInstruction)?.reference?.toString()

/** First instruction index matching [predicate], or throws. */
internal fun MutableMethod.requireFirstIndex(predicate: (Instruction) -> Boolean): Int =
    implementation!!.instructions.indices.firstOrNull { predicate(implementation!!.instructions[it]) }
        ?: throw PatchException("Anchor instruction not found in $definingClass->$name$parameterTypes")

/** All instruction indices matching [predicate]. */
internal fun MutableMethod.allIndices(predicate: (Instruction) -> Boolean): List<Int> =
    implementation!!.instructions.indices.filter { predicate(implementation!!.instructions[it]) }
