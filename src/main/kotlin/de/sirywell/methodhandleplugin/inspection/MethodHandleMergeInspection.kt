package de.sirywell.methodhandleplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import de.sirywell.methodhandleplugin.TypeData
import de.sirywell.methodhandleplugin.mhtype.BoundTop

class MethodHandleMergeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return Visitor(holder)
    }

    class Visitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitExpression(expression: PsiExpression) {
            val typeData = TypeData.forFile(expression.containingFile)
            val type = typeData[expression] ?: return
            if (type is BoundTop && type.expression == expression) {
                problemsHolder.registerProblem(
                    type.target ?: expression,
                    type.message,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

    }
}