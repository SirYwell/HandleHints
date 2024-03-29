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
            if (!returnTypesAreCompatible(type, parameters[1])) {
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    MethodHandleBundle.message("problem.creation.arguments.expected.type",
                        type.signature.returnType.presentableText,
                        parameters[1].presentableText
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
            checkParamNotVoidAt(expression, 0)
        }

        private fun returnTypesAreCompatible(
            type: MhExactType,
            parameter: PsiType
        ): Boolean {
            val returnType = type.signature.returnType
            // void.class in invalid in constant(...), so skip here and handle it later separately
            if (returnType == PsiTypes.voidType()) return true
            if (returnType is PsiPrimitiveType) {
                return TypeConversionUtil.isAssignable(returnType, parameter)
            }
            return TypeConversionUtil.areTypesConvertible(returnType, parameter)
        }

        private fun checkParamNotVoidAt(expression: PsiMethodCallExpression, index: Int) {
            val parameters = expression.argumentList.expressions
            if (parameters.size <= index) return
            if (parameters[index].getConstantOfType<PsiType>() == PsiTypes.voidType()) {
                problemsHolder.registerProblem(
                    parameters[index],
                    MethodHandleBundle.message("problem.creation.arguments.invalid.type",
                        PsiTypes.voidType().presentableText
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }
}