package de.sirywell.methodhandleplugin.dfa

import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.psi.*
import com.intellij.psi.controlFlow.*
import com.intellij.psi.util.PsiEditorUtilBase
import de.sirywell.methodhandleplugin.TypeData
import javax.swing.SwingUtilities

class MethodHandleElementVisitor : JavaRecursiveElementWalkingVisitor() {
    private val typeData = TypeData()
    override fun visitMethod(method: PsiMethod?) {
        if (method == null || method.body == null) {
            return
        }
        val controlFlowFactory = ControlFlowFactory.getInstance(method.project)
        val controlFlow: ControlFlow
        try {
            controlFlow = controlFlowFactory.getControlFlow(method.body!!, AllVariablesControlFlowPolicy.getInstance())
        } catch (_: AnalysisCanceledException) {
            return // stop
        }
        SsaAnalyzer(controlFlow, typeData).doTraversal()
        SwingUtilities.invokeLater {
            PsiEditorUtilBase.findEditorByPsiElement(method)?.let {
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
