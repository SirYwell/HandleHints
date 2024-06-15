package de.sirywell.handlehints

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiFile

class MethodTypeInlayProvider : InlayHintsProvider {

    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): com.intellij.codeInsight.hints.declarative.InlayHintsCollector? {
        if (file.project.service<DumbService>().isDumb) return null
        return MethodTypeInlayHintsCollector()
    }
}
