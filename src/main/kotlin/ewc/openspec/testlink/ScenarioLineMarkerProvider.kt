package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import ewc.openspec.testlink.settings.TestToSpecSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

class ScenarioLineMarkerProvider : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node?.elementType != KtTokens.IDENTIFIER) return null

        val declaration = element.parent
        if (declaration !is KtNamedFunction && declaration !is KtClass) return null
        val namedDeclaration = declaration as KtNamedDeclaration
        if (namedDeclaration.nameIdentifier != element) return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        log.debug("TestToSpec: checking element '${namedDeclaration.name}', scenarioAnnotationFqn='${settings.state.scenarioAnnotationFqn}'")

        if (settings.state.scenarioAnnotationFqn.isBlank()) {
            log.debug("TestToSpec: scenarioAnnotationFqn is blank, skipping")
            return null
        }

        val expectedShortName = settings.state.scenarioAnnotationFqn.substringAfterLast('.')
        log.debug("TestToSpec: looking for annotation short name='$expectedShortName'")

        val allAnnotations = namedDeclaration.annotationEntries.map { it.shortName?.asString() }
        log.debug("TestToSpec: annotations on '${namedDeclaration.name}': $allAnnotations")

        val scenarios = namedDeclaration.annotationEntries
            .filter { it.shortName?.asString() == expectedShortName }
            .mapNotNull { annotation ->
                val capability = extractStringArgument(annotation, settings.state.capabilityAttribute)
                log.debug("TestToSpec: capability arg ('${settings.state.capabilityAttribute}') = $capability")
                capability ?: return@mapNotNull null
                val scenarioName = extractStringArgument(annotation, settings.state.valueAttribute)
                log.debug("TestToSpec: value arg ('${settings.state.valueAttribute}') = $scenarioName")
                scenarioName ?: return@mapNotNull null
                ScenarioRef(capability, scenarioName)
            }

        log.debug("TestToSpec: matched scenarios: $scenarios")
        if (scenarios.isEmpty()) return null

        return ScenarioMarkerHelper.makeLineMarkerInfo(element, scenarios, project)
    }

    private fun extractStringArgument(annotation: org.jetbrains.kotlin.psi.KtAnnotationEntry, name: String): String? {
        for (arg in annotation.valueArguments) {
            val argName = arg.getArgumentName()?.asName?.asString()
            if (argName == name) {
                return arg.getArgumentExpression()?.text?.removeSurrounding("\"")
            }
        }
        return null
    }
}
