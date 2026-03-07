package ewc.openspec.testlink

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import ewc.openspec.testlink.settings.TestToSpecSettings
import java.io.File

object SpecReader {

    private val log = Logger.getInstance(SpecReader::class.java)

    data class ScenarioContent(
        val heading: String,
        val markdown: String,
        val lineNumber: Int,
        val filePath: String
    )

    fun specFilePath(project: Project, capability: String): String? {
        val basePath = project.basePath ?: return null
        val specRootPath = TestToSpecSettings.getInstance(project).state.specRootPath
        return "$basePath/$specRootPath/$capability/spec.md"
    }

    fun readScenario(project: Project, capability: String, scenarioName: String): ScenarioContent? {
        val specPath = specFilePath(project, capability) ?: run {
            log.debug("TestToSpec: project.basePath is null")
            return null
        }
        log.debug("TestToSpec: looking for spec file at '$specPath'")

        val file = File(specPath)
        if (!file.exists()) {
            log.debug("TestToSpec: spec file not found: $specPath")
            return null
        }

        val lines = file.readLines()
        val escapedName = Regex.escape(scenarioName)
        val headingRegex = "^#{1,6}\\s+Scenario:\\s*$escapedName\\s*$".toRegex(RegexOption.IGNORE_CASE)
        log.debug("TestToSpec: searching for heading matching regex pattern for scenario '$scenarioName'")

        var startLine = -1
        var headingLevel = 0
        for ((index, line) in lines.withIndex()) {
            if (headingRegex.matches(line.trim())) {
                startLine = index
                headingLevel = line.trimStart().takeWhile { it == '#' }.length
                log.debug("TestToSpec: found scenario heading at line $index: '${line.trim()}'")
                break
            }
        }

        if (startLine == -1) {
            log.debug("TestToSpec: scenario '$scenarioName' not found in $specPath")
            return null
        }

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
