package de.sirywell.methodhandleplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReferenceExpression
import de.sirywell.methodhandleplugin.TypeData

class MethodHandleMergeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return Visitor(holder)
    }

    class Visitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitElement(element: PsiElement) {
            val reporter = TypeData.forFile(element.containingFile).problemFor(element)
            if (reporter != null) {
                reporter(problemsHolder)
            }
        }

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            visitElement(expression)
        }

    }
}