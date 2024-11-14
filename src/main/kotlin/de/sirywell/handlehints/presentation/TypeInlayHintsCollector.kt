package de.sirywell.handlehints.presentation

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceExpression
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.getVariable
import de.sirywell.handlehints.type.*

class TypeInlayHintsCollector : SharedBypassCollector {

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        val (pos, belongsToBefore) = when (element) {
            is PsiDeclarationStatement -> (element.getVariable()?.nameIdentifier?.textRange?.endOffset
                ?: element.textRange.endOffset) to true

            is PsiAssignmentExpression -> (element.lExpression.textRange.endOffset) to true
            is PsiReferenceExpression -> {
                // TODO ????
                if (element.parent is PsiParameterList) {
                    element.textRange.startOffset to false
                } else {
                    element.textRange.endOffset to true
                }
            }

            else -> element.textRange.endOffset to true
        }
        val typeData = TypeData.forFile(element.containingFile)
        val type = typeData[element] ?: return
        // don't print
        if (type is TopTypeLatticeElement || type is BotTypeLatticeElement) {
            return
        }
        if (type is PathElementType) {
            return // don't print path elements for now
        }
        sink.addPresentation(InlineInlayPosition(pos, belongsToBefore), hasBackground = true) {
            text(TypePrinter().print(type))
        }
    }
}
