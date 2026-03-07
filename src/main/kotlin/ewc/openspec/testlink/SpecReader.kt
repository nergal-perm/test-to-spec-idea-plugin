package ewc.openspec.testlink

import com.intellij.openapi.project.Project
import ewc.openspec.testlink.settings.TestToSpecSettings
import java.io.File

object SpecReader {

    data class ScenarioContent(
        val heading: String,
        val markdown: String,
        val lineNumber: Int,
        val filePath: String
    )

    fun readScenario(project: Project, capability: String, scenarioName: String): ScenarioContent? {
        val settings = TestToSpecSettings.getInstance(project)
        val basePath = project.basePath ?: return null
        val specPath = "$basePath/${settings.state.specRootPath}/$capability/spec.md"

        val file = File(specPath)
        if (!file.exists()) return null

        val lines = file.readLines()
        val escapedName = Regex.escape(scenarioName)
        val headingRegex = "^#{1,6}\\s+Scenario:\\s*$escapedName\\s*$".toRegex()

        var startLine = -1
        var headingLevel = 0
        for ((index, line) in lines.withIndex()) {
            if (headingRegex.matches(line.trim())) {
                startLine = index
                headingLevel = line.trimStart().takeWhile { it == '#' }.length
                break
            }
        }

        if (startLine == -1) return null

        var endLine = lines.size
        for (i in (startLine + 1) until lines.size) {
            val trimmed = lines[i].trimStart()
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level <= headingLevel) {
                    endLine = i
                    break
                }
            }
        }

        val markdown = lines.subList(startLine, endLine).joinToString("\n")

        return ScenarioContent(
            heading = "Scenario: $scenarioName",
            markdown = markdown,
            lineNumber = startLine,
            filePath = specPath
        )
    }
}
