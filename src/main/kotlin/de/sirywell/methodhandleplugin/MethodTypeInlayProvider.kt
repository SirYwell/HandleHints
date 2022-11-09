package de.sirywell.methodhandleplugin

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class MethodTypeInlayProvider : InlayHintsProvider<NoSettings> {
    private val settingsKey: SettingsKey<NoSettings> = SettingsKey("methodhandle.hints");
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent {
            return panel {
                row {
                    text("This is a test")
                }
            }
        }

    }

    override val key: SettingsKey<NoSettings>
        get() = settingsKey
    override val name: String
        get() = "MethodTypInlayProvider"
    override val previewText: String?
        get() = "(int, int)void"

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (file.project.service<DumbService>().isDumb) return null
        return MethodTypeInlayHintsCollector(editor)
    }
}
