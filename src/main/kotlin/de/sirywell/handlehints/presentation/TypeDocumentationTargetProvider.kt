package de.sirywell.handlehints.presentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable
import de.sirywell.handlehints.isUnrelated

class TypeDocumentationTargetProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element is PsiVariable
            && element.containingFile == originalElement?.containingFile
            && !isUnrelated(element)
            && originalElement is PsiIdentifier && isVariableName(originalElement)) {
            return TypeDocumentationTarget(originalElement, element)
        }
        return null
    }

    private fun isVariableName(originalElement: PsiIdentifier): Boolean {
        if (originalElement.parent is PsiVariable) {
            return (originalElement.parent as PsiVariable).nameIdentifier == originalElement
        }
        return originalElement.parent is PsiReferenceExpression
    }
}