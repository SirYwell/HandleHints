package de.sirywell.methodhandleplugin.dfa

import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.psi.*
import com.intellij.psi.controlFlow.*
import com.intellij.psi.util.PsiEditorUtilBase
import de.sirywell.methodhandleplugin.TypeData
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

    private fun scanElement(body: PsiElement) {
        val controlFlowFactory = ControlFlowFactory.getInstance(body.project)
        val controlFlow: ControlFlow
        try {
            controlFlow = controlFlowFactory.getControlFlow(body, AllVariablesControlFlowPolicy.getInstance())
        } catch (_: AnalysisCanceledException) {
            return // stop
        }
        SsaAnalyzer(controlFlow, typeData).doTraversal()
        SwingUtilities.invokeLater {
            PsiEditorUtilBase.findEditorByPsiElement(body.parent)?.let {
                @Suppress("UnstableApiUsage")
                ParameterHintsPassFactory.forceHintsUpdateOnNextPass(it)
            }
        }
    }

    fun scan(param: PsiFile): TypeData {
        visitFile(param)
        return typeData
    }
}
