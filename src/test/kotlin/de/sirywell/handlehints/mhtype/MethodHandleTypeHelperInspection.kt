package de.sirywell.handlehints.mhtype

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.presentation.TypePrinter

/**
 * We (mis)use this inspection to add the type info as highlighting to the source code.
 * This way, we can reuse existing test infrastructure that is meant to assert presence of inspection results.
 * I'm aware this is a bit hacky, but it's just too simple to go a different route.
 */
class MethodHandleTypeHelperInspection(private val elementFilter: (PsiElement) -> Boolean) : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!elementFilter(element)) return
                val foundType = TypeData.forFile(element.containingFile)[element] ?: return
                holder.registerProblem(element, TypePrinter().print(foundType), ProblemHighlightType.INFORMATION)
            }
        }
    }
}