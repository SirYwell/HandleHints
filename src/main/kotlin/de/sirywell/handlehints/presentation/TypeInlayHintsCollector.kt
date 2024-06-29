package de.sirywell.handlehints.presentation

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceExpression
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.getVariable
import de.sirywell.handlehints.type.*

class TypeInlayHintsCollector : SharedBypassCollector {

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        val (pos, belongsToBefore) = when (element) {
            is PsiDeclarationStatement -> (element.getVariable()?.nameIdentifier?.endOffset
                ?: element.endOffset) to true

            is PsiAssignmentExpression -> (element.lExpression.endOffset) to true
            is PsiReferenceExpression -> {
                // TODO ????
                if (element.parent is PsiParameterList) {
                    element.startOffset to false
                } else {
                    element.endOffset to true
                }
            }

            else -> element.endOffset to true
        }
        val typeData = TypeData.forFile(element.containingFile)
        val type = typeData[element] ?: return
        // don't print
        if (type is TopTypeLatticeElement || type is BotTypeLatticeElement) {
            return
        }
        sink.addPresentation(InlineInlayPosition(pos, belongsToBefore), hasBackground = true) {
            text(TypePrinter().print(type))
        }
    }
}
