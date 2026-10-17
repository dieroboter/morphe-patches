package app.dieroboter.patches.flappybird

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import app.dieroboter.patches.shared.Constants.COMPATIBILITY_FLAPPY_BIRD

private fun MutableMethod.replaceConst16Literal(old: Int, new: Int, count: Int = 1) {
    requireLiteralIndices(old, Opcode.CONST_16, count).forEach { index ->
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        replaceInstruction(index, "const/16 v$register, $new")
    }
}

private fun MutableMethod.replaceRsubIntLiteral(old: Int, new: Int, count: Int = 1) {
    requireLiteralIndices(old, Opcode.RSUB_INT, count).forEach { index ->
        val insn = getInstruction<TwoRegisterInstruction>(index)
        replaceInstruction(index, "rsub-int v${insn.registerA}, v${insn.registerB}, $new")
    }
}

/** Deletes the [iget P; iget f.d; add-int/2addr; add-int/lit8] offset block (port: removed). */
private fun MutableMethod.deletePipeOffsetBlock() {
    val instrs = implementation!!.instructions
    val start = instrs.indices.firstOrNull { i ->
        instrs[i].refString() == "Lcom/dotgears/flappy/c;->P:Lcom/dotgears/f;" &&
            instrs[i + 1].refString() == "Lcom/dotgears/f;->d:I" &&
            instrs[i + 2].opcode == Opcode.ADD_INT_2ADDR &&
            instrs[i + 3].opcode == Opcode.ADD_INT_LIT8
    } ?: throw PatchException("Pipe offset block not found in $definingClass->$name")
    removeInstructions(start, 4)
}

/**
 * Replaces a K:/R: pipe setup span (position computed from atlas fields, then
 * f.a(II) positions the element) with the port's fixed flag setup:
 * const/4 v1, 0; f.F = false; f.G = false.
 */
private fun MutableMethod.replacePipeSetupWithFlags(fieldRef: String, span: Int) {
    val instrs = implementation!!.instructions
    val start = instrs.indices.firstOrNull { i ->
        instrs[i].refString() == fieldRef &&
            instrs[i + span - 1].refString() == "Lcom/dotgears/f;->a(II)V"
    } ?: throw PatchException("Pipe setup span not found in $definingClass->$name")
    removeInstructions(start, span)
    addInstructions(
        start,
        """
        const/4 v1, 0x0
        iput-boolean v1, v0, Lcom/dotgears/f;->F:Z
        iput-boolean v1, v0, Lcom/dotgears/f;->G:Z
        """,
    )
}

private const val ONCREATE_WINDOW_BLOCK = """
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    sget v1, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v2, 0x1c
    if-lt v1, v2, :flappy_port_cond
    invoke-virtual {v0}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager${'$'}LayoutParams;
    move-result-object v1
    const/4 v2, 0x1
    iput v2, v1, Landroid/view/WindowManager${'$'}LayoutParams;->layoutInDisplayCutoutMode:I
    invoke-virtual {v0, v1}, Landroid/view/Window;->setAttributes(Landroid/view/WindowManager${'$'}LayoutParams;)V
    sget v1, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
    const/16 v2, 0x1e
    if-lt v1, v2, :flappy_port_cond
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/view/Window;->setDecorFitsSystemWindows(Z)V
    invoke-virtual {v0}, Landroid/view/Window;->getInsetsController()Landroid/view/WindowInsetsController;
    move-result-object v1
    if-eqz v1, :flappy_port_cond
    const/4 v2, 0x2
    invoke-interface {v1, v2}, Landroid/view/WindowInsetsController;->setSystemBarsBehavior(I)V
    const/4 v2, 0x3
    invoke-interface {v1, v2}, Landroid/view/WindowInsetsController;->hide(I)V
    :flappy_port_cond
    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;
    move-result-object v1
    const/16 v2, 0x1706
    invoke-virtual {v1, v2}, Landroid/view/View;->setSystemUiVisibility(I)V
"""

private const val SCENE_HEAD_BLOCK = """
    iget v5, v0, Lcom/dotgears/i;->d:F
    iget v6, v0, Lcom/dotgears/i;->e:F
    iget v7, v0, Lcom/dotgears/i;->f:F
    iget v8, v0, Lcom/dotgears/i;->g:F
    const/4 v1, 0x0
    const/4 v2, 0x0
    const/16 v3, 0x120
    const/16 v4, 0x280
    move-object v0, p0
    invoke-virtual/range {v0 .. v9}, Lcom/dotgears/flappy/c;->a(IIIIFFFFF)V
    const/4 v8, 0x0
"""

private const val SETUP_BLOCK = """
    sget-object v0, Lcom/dotgears/g;->D:Lcom/dotgears/g;
    iget-object v0, v0, Lcom/dotgears/g;->C:Lcom/dotgears/p;
    sget-object v1, Lcom/dotgears/g;->D:Lcom/dotgears/g;
    iget v1, v1, Lcom/dotgears/g;->y:I
    sget-object v2, Lcom/dotgears/g;->D:Lcom/dotgears/g;
    iget v2, v2, Lcom/dotgears/g;->z:I
    const/16 v3, 0xa
    const/16 v4, 0x14
    const/16 v5, 0x1e
    const/16 v6, 0x28
    invoke-virtual/range {v0 .. v6}, Lcom/dotgears/p;->a(IIIIII)V
    return-void
"""

/**
 * The display adaptation of the 64-bit port:
 *
 * - The game world widens from 512 to 640 units (engine camera width and the
 *   cascade of +64/+128 geometry constants across the scene, bird, pipe and
 *   texture classes), with the scene's background call switched to the
 *   region-based overload using the element's own frame.
 * - The pipe/ground elements stop being positioned from atlas offsets and get
 *   the fixed F/G flag setup; a score-overlay tap block and per-frame element
 *   updates tied to the old layout are removed, and a scene trigger is
 *   re-gated on the port's state flag.
 * - GameActivity.onCreate gains the port's window handling: display-cutout
 *   shortEdges mode on API 28+, decor-not-fitted + hidden transient system
 *   bars on API 30+, and the immersive systemUiVisibility flags — the fix for
 *   the black bar on tall screens.
 * - The texture-region setup that the stock code re-ran every frame moves to
 *   a one-time init call (the port's flicker fix). It needs seven registers,
 *   more than the init method has, so it is added as a small static helper
 *   method on the same class and invoked from init.
 */
@Suppress("unused")
val widescreenPatch = bytecodePatch(
    name = "Adapt to modern displays",
    description = "Widens the game world from 512 to 640 units with the matching geometry " +
        "adjustments, reworks the scene layout for the wider field, adds display-cutout " +
        "and immersive window handling (fixes the black bar on tall screens), and moves " +
        "the per-frame texture setup to a one-time init call (the port's flicker fix).",
    default = true
) {
    compatibleWith(COMPATIBILITY_FLAPPY_BIRD)

    execute {
        // Engine camera width: 512.0f -> 640.0f. (For const/high16, dexlib2
        // reports the narrow literal already shifted: 0x44000000.)
        EngineConfigFingerprint.method.apply {
            val index = requireLiteralIndices(0x44000000, Opcode.CONST_HIGH16).single()
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const/high16 v$register, 0x44200000")
        }

        // onCreate: prepend the port's cutout/immersive window block.
        GameActivityOnCreateFingerprint.method.addInstructions(0, ONCREATE_WINDOW_BLOCK)

        // Bird: init position 0xf6 -> 0x136; update rsub-int 0x190 -> 0x210 (x2).
        BirdInitFingerprint.method.replaceConst16Literal(0xf6, 0x136)
        BirdUpdateFingerprint.method.replaceRsubIntLiteral(0x190, 0x210, count = 2)

        // Scene update: the complex method, edited region by region.
        SceneUpdateFingerprint.method.apply {
            // 1. Delete the H-gated score-overlay block (11 instructions ending
            //    in the a(i, I, I, F) call).
            run {
                val instrs = implementation!!.instructions
                val start = instrs.indices.firstOrNull { i ->
                    instrs[i].refString() == "Lcom/dotgears/flappy/c;->H:Z" &&
                        instrs[i + 1].opcode == Opcode.IF_EQZ
                } ?: throw PatchException("H-gated block not found in scene update")
                if (instrs[start + 10].refString() != "Lcom/dotgears/flappy/c;->a(Lcom/dotgears/i;IIF)V") {
                    throw PatchException("Unexpected H-gated block shape in scene update")
                }
                removeInstructions(start, 11)
            }

            // 2. Delete the P atlas-offset block.
            deletePipeOffsetBlock()

            // 3. K pipe setup span (8 instructions) -> fixed F/G flags.
            replacePipeSetupWithFlags("Lcom/dotgears/flappy/c;->K:Lcom/dotgears/f;", span = 8)

            // 4. The remaining 0x154 width constant -> 0x1d4.
            replaceConst16Literal(0x154, 0x1d4)

            // 5. Delete the per-frame P element updates.
            run {
                val instrs = implementation!!.instructions
                val start = instrs.indices.firstOrNull { i ->
                    instrs[i].refString() == "Lcom/dotgears/flappy/c;->P:Lcom/dotgears/f;" &&
                        instrs[i + 1].refString() == "Lcom/dotgears/f;->a(F)V" &&
                        instrs[i + 2].refString() == "Lcom/dotgears/flappy/c;->P:Lcom/dotgears/f;" &&
                        instrs[i + 3].refString() == "Lcom/dotgears/f;->a(Lcom/dotgears/g;)V"
                } ?: throw PatchException("Per-frame P updates not found in scene update")
                removeInstructions(start, 4)
            }

            // 6. Scene trigger rework. Stock layout here:
            //      if-eqz K.h -> :cond_b        (guards the two calls below)
            //      const/4 v0, 0x5
            //      invoke a(ZIF)                 (call A)
            //      const/16 v0, 0xa
            //      invoke c(10, v8)              (call B — kept by the port)
            //      :cond_b
            //      iget-object P; iget-boolean P.h; if-eqz -> :cond_c
            //      invoke c(v12, v8)             (trigger — deleted by the port)
            //      const/16 v0, 0xa
            //      invoke c(10, v8)              (call C — deleted by the port)
            //      :cond_c
            //      iget-object R ...             (landing instruction)
            //    The port deletes the whole P.h block (trigger + call C),
            //    inserts an e.F gate (iget am; iget-boolean F; if-nez) between
            //    call A and call B, and lands both the K.h branch and the new
            //    gate branch on the landing instruction.
            run {
                val instrs = implementation!!.instructions
                val aCall = instrs.indices.firstOrNull {
                    instrs[it].refString() == "Lcom/dotgears/flappy/c;->a(ZIF)V"
                } ?: throw PatchException("a(ZIF) call not found in scene update")
                // Call B (kept) must follow immediately: const/16 0xa + invoke c(II).
                if ((instrs[aCall + 1] as? NarrowLiteralInstruction)?.narrowLiteral != 0xa ||
                    instrs[aCall + 2].refString() != "Lcom/dotgears/flappy/c;->c(II)V"
                ) {
                    throw PatchException("Unexpected shape after a(ZIF) in scene update")
                }
                // The trigger invoke: c(II) whose first argument register is v12.
                val trigger = instrs.indices.firstOrNull { i ->
                    i > aCall && instrs[i].refString() == "Lcom/dotgears/flappy/c;->c(II)V" &&
                        (instrs[i] as? FiveRegisterInstruction)?.registerD == 12
                } ?: throw PatchException("Trigger invoke c(v12, ...) not found in scene update")
                // Block start = iget-object P, three before the trigger; verify shape.
                val blockStart = trigger - 3
                if (instrs[blockStart].refString() != "Lcom/dotgears/flappy/c;->P:Lcom/dotgears/f;" ||
                    instrs[blockStart + 1].refString() != "Lcom/dotgears/f;->h:Z" ||
                    instrs[blockStart + 2].opcode != Opcode.IF_EQZ ||
                    (instrs[trigger + 1] as? NarrowLiteralInstruction)?.narrowLiteral != 0xa ||
                    instrs[trigger + 2].refString() != "Lcom/dotgears/flappy/c;->c(II)V"
                ) {
                    throw PatchException("Unexpected trigger block shape in scene update")
                }
                // Landing = first instruction after the deleted block; it
                // carries the :cond_c label. Also read p0's register number
                // from the block's iget-object (its object register).
                val landingLabel = implementation!!.newLabelForIndex(trigger + 3)
                val thisRegister = getInstruction<TwoRegisterInstruction>(blockStart).registerB

                // Repoint the K.h branch (two before call A, right after the
                // iget-boolean K.h) at the landing instruction.
                val outerBranch = getInstruction<BuilderInstruction21t>(aCall - 2)
                if (outerBranch.opcode != Opcode.IF_EQZ) {
                    throw PatchException("Unexpected K.h branch in scene update")
                }
                replaceInstruction(
                    aCall - 2,
                    BuilderInstruction21t(Opcode.IF_EQZ, outerBranch.registerA, landingLabel),
                )

                // Delete the P.h block: iget P, iget-boolean h, if, trigger
                // invoke, const 0xa, call C invoke (6 instructions).
                removeInstructions(blockStart, 6)

                // Insert the e.F gate between call A and call B, built as
                // builder instructions so its branch targets the landing
                // label object directly.
                addInstructions(
                    aCall + 1,
                    listOf(
                        BuilderInstruction22c(
                            Opcode.IGET_OBJECT,
                            0,
                            thisRegister,
                            ImmutableFieldReference(
                                "Lcom/dotgears/flappy/c;",
                                "am",
                                "Lcom/dotgears/flappy/e;",
                            ),
                        ),
                        BuilderInstruction22c(
                            Opcode.IGET_BOOLEAN,
                            0,
                            0,
                            ImmutableFieldReference("Lcom/dotgears/flappy/e;", "F", "Z"),
                        ),
                        BuilderInstruction21t(Opcode.IF_NEZ, 0, landingLabel),
                    ),
                )
            }

            // 7. Scene head call: swap the a(i, I, I, F) call for the port's
            //    region-based a(IIIIFFFFF) setup block.
            run {
                val head = requireFirstIndex {
                    it.refString() == "Lcom/dotgears/flappy/c;->a(Lcom/dotgears/i;IIF)V"
                }
                removeInstruction(head)
                addInstructions(head, SCENE_HEAD_BLOCK)
            }

            // 8. Remaining geometry constants.
            replaceConst16Literal(0xb4, 0xd0)
            replaceConst16Literal(0x168, 0x1a0)
            replaceRsubIntLiteral(0x190, 0x210)
            replaceRsubIntLiteral(0x200, 0x280, count = 2)
            replaceConst16Literal(0x96, 0xd6)
            run {
                val index = requireLiteralIndices(0x96, Opcode.ADD_INT_LIT16).single()
                val insn = getInstruction<TwoRegisterInstruction>(index)
                replaceInstruction(index, "add-int/lit16 v${insn.registerA}, v${insn.registerB}, 0xd6")
            }
        }

        // Scene layout: width constant, offset-block deletion, K and R spans.
        SceneLayoutFingerprint.method.apply {
            replaceConst16Literal(0x154, 0x1d4)
            deletePipeOffsetBlock()
            replacePipeSetupWithFlags("Lcom/dotgears/flappy/c;->K:Lcom/dotgears/f;", span = 7)
            replacePipeSetupWithFlags("Lcom/dotgears/flappy/c;->R:Lcom/dotgears/f;", span = 6)
        }

        // e: delete the per-frame texture setup block (12 instructions around
        //    the p.a(IIIIII) call).
        EUpdateFingerprint.method.apply {
            val call = requireFirstIndex { it.refString() == "Lcom/dotgears/p;->a(IIIIII)V" }
            val instrs = implementation!!.instructions
            if (instrs[call - 9].refString() != "Lcom/dotgears/g;->D:Lcom/dotgears/g;" ||
                instrs[call + 2].refString() != "Lcom/dotgears/g;->c(II)V"
            ) {
                throw PatchException("Unexpected texture setup block shape in e.a(F)")
            }
            removeInstructions(call - 9, 12)
        }

        // e init: add the one-time setup as a static helper (it needs seven
        // registers; the init method only has five locals), set f = 1, and
        // call the helper. The trailing g.c(II) call switches its last
        // argument to a literal 0, as in the port.
        run {
            val eClass = mutableClassDefBy(E_CLASS)
            val helper = MutableMethod(
                ImmutableMethod(
                    E_CLASS,
                    "flappyPortSetup",
                    emptyList(),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                    emptySet(),
                    emptySet(),
                    ImmutableMethodImplementation(7, emptyList(), emptyList(), emptyList()),
                ),
            )
            eClass.methods.add(helper)
            helper.addInstructions(0, SETUP_BLOCK)
        }
        EInitFingerprint.method.apply {
            val iput = requireFirstIndex {
                it.opcode == Opcode.IPUT && it.refString() == "Lcom/dotgears/flappy/e;->f:I"
            }
            replaceInstruction(iput, "const/4 v0, 0x1")
            addInstructions(
                iput + 1,
                """
                iput v0, p0, Lcom/dotgears/flappy/e;->f:I
                invoke-static {}, Lcom/dotgears/flappy/e;->flappyPortSetup()V
                """,
            )
            val call = requireFirstIndex { it.refString() == "Lcom/dotgears/g;->c(II)V" }
            replaceInstruction(call, "const/4 v2, 0x0")
            addInstructions(call + 1, "invoke-virtual {v0, v1, v2}, Lcom/dotgears/g;->c(II)V")
        }

        // e draw helper: add-int/lit16 offset 0x82 -> 0xc2.
        EDrawFingerprint.method.apply {
            val index = requireLiteralIndices(0x82, Opcode.ADD_INT_LIT16).single()
            val insn = getInstruction<TwoRegisterInstruction>(index)
            replaceInstruction(index, "add-int/lit16 v${insn.registerA}, v${insn.registerB}, 0xc2")
        }

        // f draw helper: position constants 0x92 -> 0xd2, 0xdc -> 0x11c.
        FDrawFingerprint.method.replaceConst16Literal(0x92, 0xd2)
        FDrawFingerprint.method.replaceConst16Literal(0xdc, 0x11c)

        // p: constructor rsub-int 0x200 -> 0x280; region width 0x1f8 -> 0x278.
        PConstructorFingerprint.method.replaceRsubIntLiteral(0x200, 0x280)
        PRegionFingerprint.method.replaceConst16Literal(0x1f8, 0x278)
    }
}
