package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import ewc.openspec.testlink.settings.TestToSpecSettings
import java.awt.event.MouseEvent
import org.intellij.plugins.markdown.lang.MarkdownTokenTypes

class SpecLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node?.elementType != MarkdownTokenTypes.ATX_HEADER) return null

        val headingText = element.parent?.text?.trimStart('#')?.trim() ?: return null
        val match = SCENARIO_REGEX.matchEntire(headingText) ?: return null
        val scenarioName = match.groupValues[1].trim()

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        if (settings.state.scenarioAnnotationFqn.isBlank()) return null

        val capability = deriveCapability(element, settings) ?: return null

        val tests = TestFinder.find(project, capability, scenarioName)
        val icon = if (tests.isEmpty()) Icons.SCENARIO_UNLINKED else Icons.SCENARIO
        val navHandler: ((MouseEvent, PsiElement) -> Unit)? = if (tests.isEmpty()) null else { e, _ ->
            if (tests.size == 1) {
                (tests[0] as? NavigatablePsiElement)?.navigate(true)
            } else {
                PsiTargetNavigator(tests.filterIsInstance<NavigatablePsiElement>().toTypedArray())
                    .navigate(e, "Tests for '$scenarioName'", project)
            }
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { when {
                tests.isEmpty() -> "Scenario: $scenarioName (no tests)"
                tests.size == 1 -> "Go to test: ${tests[0].name}"
                else -> "Go to tests for: $scenarioName"
            } },
            navHandler,
            GutterIconRenderer.Alignment.LEFT,
            { "Scenario: $scenarioName" }
        )
    }

    private fun deriveCapability(element: PsiElement, settings: TestToSpecSettings): String? {
        val project = element.project
        val specRoot = settings.state.specRootPath
        val virtualFile = element.containingFile?.virtualFile ?: return null
        val filePath = virtualFile.path

        // Collect candidate root paths. Content roots are checked first because they
        // cover light platform tests (where an in-memory VFS root is the content root
        // and basePath does not match the virtual file paths). project.basePath is added
        // as a fallback for multi-module Maven/Gradle projects where the root aggregator
        // directory may not be registered as a content root, yet spec files live there.
        val contentRootPaths = ProjectRootManager.getInstance(project).contentRoots.map { it.path }
        val candidates = if (project.basePath != null && project.basePath !in contentRootPaths)
            contentRootPaths + project.basePath!!
        else
            contentRootPaths

        for (rootPath in candidates) {
            val prefix = "$rootPath/$specRoot/"
            if (filePath.startsWith(prefix)) {
                val relative = filePath.removePrefix(prefix)
                return relative.substringBefore('/')
                    .takeIf { it.isNotEmpty() && it != relative }
            }
        }

        return null
    }

    companion object {
        private val SCENARIO_REGEX = """Scenario:\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)
    }
}
