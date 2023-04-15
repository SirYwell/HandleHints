package de.sirywell.methodhandleplugin.dfa

import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.controlFlow.*
import com.intellij.psi.util.PsiEditorUtilBase
import de.sirywell.methodhandleplugin.TypeData
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType.methodType
import javax.swing.SwingUtilities

class MethodHandleElementVisitor : JavaRecursiveElementWalkingVisitor() {
    private val typeData = TypeData()
    override fun visitMethod(method: PsiMethod) {
        if (method.body == null) return
        scanElement(method.body!!)
    }

    override fun visitClassInitializer(initializer: PsiClassInitializer) {
        scanElement(initializer.body)
    }

    override fun visitField(field: PsiField) {
        @Suppress("UnstableApiUsage")
        if (!field.hasModifier(JvmModifier.FINAL)) return
        if (field.initializer == null) return
        var controlFlow = buildControlFlow(field) ?: return
        controlFlow = addFieldWrite(controlFlow, field)
        applyAnalysis(controlFlow, field)
    }

    private fun scanElement(body: PsiElement) {
        val controlFlow = buildControlFlow(body) ?: return
        applyAnalysis(controlFlow, body)
    }

    private fun applyAnalysis(controlFlow: ControlFlow, body: PsiElement) {
        SsaAnalyzer(controlFlow, typeData).doTraversal()
        SwingUtilities.invokeLater {
            PsiEditorUtilBase.findEditorByPsiElement(body.parent)?.let {
                @Suppress("UnstableApiUsage")
                ParameterHintsPassFactory.forceHintsUpdateOnNextPass(it)
            }
        }
    }

    private fun buildControlFlow(body: PsiElement): ControlFlow? {
        val controlFlowFactory = ControlFlowFactory.getInstance(body.project)
        return try {
            controlFlowFactory.getControlFlow(body, AllVariablesControlFlowPolicy.getInstance())
        } catch (_: AnalysisCanceledException) {
            null// stop
        }
    }

    fun scan(param: PsiFile): TypeData {
        visitFile(param)
        return typeData
    }

    private fun addFieldWrite(controlFlow: ControlFlow, field: PsiField): ControlFlow {
        if (controlFlow.instructions.lastOrNull() is WriteVariableInstruction) return controlFlow
        val instr = buildWriteInstruction(field)
        val instructions = (controlFlow.instructions + instr).toMutableList()
        return object : ControlFlow by controlFlow {
            override fun getInstructions() = instructions

            override fun getSize() = instructions.size

            override fun getStartOffset(element: PsiElement): Int {
                return if (element === field) {
                    controlFlow.size
                } else {
                    controlFlow.getStartOffset(element)
                }
            }

            override fun getEndOffset(element: PsiElement): Int {
                return if (element === field) {
                    controlFlow.size
                } else {
                    controlFlow.getEndOffset(element)
                }
            }

            override fun getElement(offset: Int): PsiElement? {
                return if (offset < controlFlow.size) {
                    controlFlow.getElement(offset)
                } else if (offset == controlFlow.size) {
                    field
                } else {
                    null
                }
            }
        }
    }

    private fun buildWriteInstruction(variable: PsiVariable): WriteVariableInstruction {
        return WRITE_VARIABLE_CTOR.invokeExact(variable) as WriteVariableInstruction
    }

    companion object {
        private val WRITE_VARIABLE_CTOR: MethodHandle by lazy {
            val writeVariableInstructionClass = WriteVariableInstruction::class.java
            val lookup = MethodHandles.privateLookupIn(writeVariableInstructionClass, MethodHandles.lookup())
            lookup.findConstructor(writeVariableInstructionClass, methodType(Void.TYPE, PsiVariable::class.java))
        }
    }
}
