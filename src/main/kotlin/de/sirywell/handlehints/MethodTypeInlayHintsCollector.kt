package de.sirywell.handlehints

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReferenceExpression
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import de.sirywell.handlehints.type.BotSignature
import de.sirywell.handlehints.type.TopSignature

class MethodTypeInlayHintsCollector : SharedBypassCollector {

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        val (pos, belongsToBefore) = when (element) {
            is PsiDeclarationStatement -> (element.getVariable()?.nameIdentifier?.endOffset
                ?: element.endOffset) to true

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
        if (type.signature is TopSignature || type.signature is BotSignature) {
            return
        }
        sink.addPresentation(InlineInlayPosition(pos, belongsToBefore), hasBackground = true) {
            text(type.toString())
        }
    }
}