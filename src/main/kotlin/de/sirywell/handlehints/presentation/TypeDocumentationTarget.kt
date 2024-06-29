package de.sirywell.handlehints.presentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.refactoring.suggested.createSmartPointer
import de.sirywell.handlehints.TypeData

@Suppress("UnstableApiUsage")
class TypeDocumentationTarget(private val identifier: PsiIdentifier, private val element: PsiElement) :
    DocumentationTarget {
    override fun computePresentation(): TargetPresentation {
        return TargetPresentation.builder("Analysed Type")
            .presentation()
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val idPointer = identifier.createSmartPointer()
        val elPointer = element.createSmartPointer()
        return Pointer {
            val identifier = idPointer.element ?: return@Pointer null
            val element = elPointer.element ?: return@Pointer null
            TypeDocumentationTarget(identifier, element)
        }
    }

    override fun computeDocumentation(): DocumentationResult? {
        val type = TypeData.forFile(element.containingFile)[element.parent] ?: return null
        return DocumentationResult.documentation("<code>${TypePrinter().print(type)}</code>")
    }
}