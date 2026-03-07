package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import ewc.openspec.testlink.settings.TestToSpecSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

class ScenarioLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node?.elementType != KtTokens.IDENTIFIER) return null

        val declaration = element.parent
        if (declaration !is KtNamedFunction && declaration !is KtClass) return null
        val namedDeclaration = declaration as KtNamedDeclaration
        if (namedDeclaration.nameIdentifier != element) return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        if (settings.state.scenarioAnnotationFqn.isBlank()) return null

        val expectedShortName = settings.state.scenarioAnnotationFqn.substringAfterLast('.')

        val scenarios = namedDeclaration.annotationEntries
            .filter { it.shortName?.asString() == expectedShortName }
            .mapNotNull { annotation ->
                val capability = extractStringArgument(annotation, settings.state.capabilityAttribute)
                    ?: return@mapNotNull null
                val scenarioName = extractStringArgument(annotation, settings.state.valueAttribute)
                    ?: return@mapNotNull null
                ScenarioRef(capability, scenarioName)
            }

        if (scenarios.isEmpty()) return null

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

    private fun extractStringArgument(annotation: KtAnnotationEntry, name: String): String? {
        for (arg in annotation.valueArguments) {
            val argName = arg.getArgumentName()?.asName?.asString()
            if (argName == name) {
                return arg.getArgumentExpression()?.text?.removeSurrounding("\"")
            }
        }
        return null
    }

    private fun navigateToScenario(project: Project, ref: ScenarioRef) {
        val content = SpecReader.readScenario(project, ref.capability, ref.name) ?: return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(content.filePath) ?: return
        OpenFileDescriptor(project, virtualFile, content.lineNumber, 0).navigate(true)
    }

    private fun showScenarioPopup(project: Project, scenarios: List<ScenarioRef>, mouseEvent: MouseEvent) {
        val bodyHtml = buildString {
            for ((index, ref) in scenarios.withIndex()) {
                val content = SpecReader.readScenario(project, ref.capability, ref.name)
                if (content != null) {
                    append(MarkdownRenderer.renderToHtml(content.markdown))
                    append("""<p><a href="navigate:$index">Open in spec.md</a></p>""")
                } else {
                    append("""<p><em>Scenario not found: ${ref.capability} / ${ref.name}</em></p>""")
                }
                if (index < scenarios.size - 1) {
                    append("<hr/>")
                }
            }
        }

        val fullHtml = MarkdownRenderer.wrapInHtml(bodyHtml)

        var popup: com.intellij.openapi.ui.popup.JBPopup? = null

        val editorPane = JEditorPane("text/html", fullHtml).apply {
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

        val title = if (scenarios.size == 1) {
            "Scenario: ${scenarios[0].name}"
        } else {
            "${scenarios.size} Scenarios"
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, editorPane)
            .setTitle(title)
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.show(RelativePoint(mouseEvent))
    }

    private data class ScenarioRef(val capability: String, val name: String)
}
