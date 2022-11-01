package com.github.sirywell.methodhandleplugin

import com.github.sirywell.methodhandleplugin.mhtype.MhType
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

@Suppress("UnstableApiUsage")
class MethodTypeInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    companion object {
        val methodTypeKey = Key<MhType>(MethodTypeInlayHintsCollector::class.qualifiedName!!)
    }

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val pos = when (element) {
            is PsiDeclarationStatement -> element.getVariable()?.nameIdentifier?.endOffset ?: element.endOffset
            is PsiReferenceExpression -> {
                // TODO ????
                if (element.parent is PsiParameterList) {
                    element.startOffset
                } else {
                    element.endOffset
                }
            }
            else -> element.endOffset
        }
        val signature = element.getUserData(methodTypeKey) ?: return true
        sink.addInlineElement(
            pos,
            true,
            createInlayPresentation(signature.toString()),
            false
        )
        return true
    }

    private fun createInlayPresentation(text: String): InlayPresentation {
        return InsetPresentation(factory.roundWithBackground(factory.text(text)))
    }
}
