package de.sirywell.methodhandleplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.TypeConversionUtil
import de.sirywell.methodhandleplugin.*
import de.sirywell.methodhandleplugin.mhtype.MhExactType

class MethodHandleCreationInspection: LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return Visitor(holder)
    }

    class Visitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (!receiverIsMethodHandles(expression)) return
            val typeData = TypeData.forFile(expression.containingFile)
            val type = typeData[expression] ?: return
            if (type !is MhExactType) return
            when (expression.methodName) {
                "constant" -> checkConstant(expression, type)
                "identity" -> checkParamNotVoidAt(expression, 0)
            }
        }

        private fun checkConstant(expression: PsiMethodCallExpression, type: MhExactType) {
            val parameters = expression.argumentList.expressionTypes
            if (parameters.size != 2) return
            // TODO this method does not match the actual behavior
            if (!TypeConversionUtil.areTypesConvertible(type.signature.returnType, parameters[1])) {
                // TODO fix message
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    MethodHandleBundle.message("problem.invocation.arguments.wrong.types",
                        parameters.map { it.presentableText },
                        expression.argumentList.expressionTypes.map { it.presentableText }
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
            checkParamNotVoidAt(expression, 0)
        }

        private fun checkParamNotVoidAt(expression: PsiMethodCallExpression, index: Int) {
            val parameters = expression.argumentList.expressions
            if (parameters.size <= index) return
            if (parameters[index].getConstantOfType<PsiType>() == PsiType.VOID) {
                problemsHolder.registerProblem(
                    parameters[index],
                    MethodHandleBundle.message("problem.creation.arguments.invalid.type",
                        PsiType.VOID.presentableText
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }
}