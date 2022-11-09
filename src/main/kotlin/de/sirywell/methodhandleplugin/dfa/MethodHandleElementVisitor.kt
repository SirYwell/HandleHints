package de.sirywell.methodhandleplugin.dfa

import de.sirywell.methodhandleplugin.MethodHandleBundle.message
import de.sirywell.methodhandleplugin.TypeData
import de.sirywell.methodhandleplugin.methodName
import de.sirywell.methodhandleplugin.mhtype.MhSingleType
import de.sirywell.methodhandleplugin.receiverIsMethodHandle
import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.controlFlow.*
import com.intellij.psi.util.PsiEditorUtilBase
import javax.swing.SwingUtilities

class MethodHandleElementVisitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
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
        SsaAnalyzer(controlFlow).doTraversal()
        SwingUtilities.invokeLater {
            PsiEditorUtilBase.findEditorByPsiElement(method)?.let {
                @Suppress("UnstableApiUsage")
                ParameterHintsPassFactory.forceHintsUpdateOnNextPass(it)
            }
        }
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        if (!receiverIsMethodHandle(expression)) return
        val target = expression.methodExpression.qualifierExpression ?: return
        val type = TypeData[target] ?: return
        if (type !is MhSingleType) return // ignore for now
        val (returnType, parameters) = type.signature
        when (expression.methodName) {
            "invoke" -> {
                checkArgumentsCount(parameters, expression)
            }

            "invokeExact" -> {
                checkArgumentsTypes(parameters, expression)
                checkArgumentsCount(parameters, expression)
                checkReturnType(returnType, expression)
            }
        }
    }

    private fun checkArgumentsTypes(parameters: List<PsiType>, expression: PsiMethodCallExpression) {
        if (expression.argumentList.expressionTypes.zip(parameters)
                .any { it.first != it.second }
        ) {
            problemsHolder.registerProblem(
                expression.methodExpression as PsiExpression,
                message("problem.arguments.wrong.types",
                    parameters.map { it.presentableText },
                    expression.argumentList.expressionTypes.map { it.presentableText }
                ),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    private fun checkReturnType(returnType: PsiType, expression: PsiMethodCallExpression) {
        val parent = expression.parent
        if (parent !is PsiTypeCastExpression) {
            if (returnType != PsiType.getJavaLangObject(expression.manager, expression.resolveScope)) {
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    message("problem.returnType.not.object", returnType.presentableText),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        } else {
            val type = parent.castType?.type ?: return
            if (type != returnType) {
                problemsHolder.registerProblem(
                    parent.castType!!,
                    message("problem.returnType.wrong.cast", returnType.presentableText, type.presentableText),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }

    private fun checkArgumentsCount(
        parameters: List<PsiType>,
        expression: PsiMethodCallExpression
    ) {
        if (parameters.size != expression.argumentList.expressionCount) {
            problemsHolder.registerProblem(
                expression.methodExpression as PsiExpression,
                message("problem.arguments.count", parameters.size, expression.argumentList.expressionCount),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }
}
