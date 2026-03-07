package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

data class ScenarioRef(val capability: String, val name: String)

object ScenarioMarkerHelper {

    fun makeLineMarkerInfo(
        element: PsiElement,
        scenarios: List<ScenarioRef>,
        project: Project
    ): LineMarkerInfo<*> {
        val tooltipText = if (scenarios.size == 1) {
            "View scenario: ${scenarios[0].name}"
        } else {
            "View ${scenarios.size} scenarios"
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            Icons.SCENARIO,
            { tooltipText },
            { e: MouseEvent, _: PsiElement ->
                if (e.isMetaDown || e.isControlDown) {
                    navigateToScenario(project, scenarios[0])
                } else {
                    showScenarioPopup(project, scenarios, e)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { tooltipText }
        )
    }

    fun navigateToScenario(project: Project, ref: ScenarioRef) {
        val content = SpecReader.readScenario(project, ref.capability, ref.name) ?: return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(content.filePath) ?: return
        OpenFileDescriptor(project, virtualFile, content.lineNumber, 0).navigate(true)
    }

    fun showScenarioPopup(project: Project, scenarios: List<ScenarioRef>, mouseEvent: MouseEvent) {
        fun buildHtml(): String {
            val bodyHtml = buildString {
                for ((index, ref) in scenarios.withIndex()) {
                    val content = SpecReader.readScenario(project, ref.capability, ref.name)
                    if (content != null) {
                        append(MarkdownRenderer.renderToHtml(content.markdown))
                        append("""<p><a href="navigate:$index">Open in spec.md</a></p>""")
                    } else {
                        val expectedPath = SpecReader.specFilePath(project, ref.capability) ?: "${ref.capability}/spec.md"
                        append("""<p><em>Scenario "${ref.name}" not found in $expectedPath</em></p>""")
                    }
                    if (index < scenarios.size - 1) append("<hr/>")
                }
            }
            return MarkdownRenderer.wrapInHtml(bodyHtml)
        }

        var popup: com.intellij.openapi.ui.popup.JBPopup? = null

        val editorPane = JEditorPane("text/html", buildHtml()).apply {
            isEditable = false
            caretPosition = 0
            addHyperlinkListener { e ->
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    val idx = e.description.removePrefix("navigate:").toIntOrNull()
                    if (idx != null && idx in scenarios.indices) {
                        popup?.closeOk(null)
                        navigateToScenario(project, scenarios[idx])
                    }
                }
            }
        }

        val scrollPane = JBScrollPane(editorPane).apply {
            preferredSize = Dimension(600, 400)
        }

        val title = if (scenarios.size == 1) "Scenario: ${scenarios[0].name}" else "${scenarios.size} Scenarios"

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, editorPane)
            .setTitle(title)
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.show(RelativePoint(mouseEvent))
    }
}
