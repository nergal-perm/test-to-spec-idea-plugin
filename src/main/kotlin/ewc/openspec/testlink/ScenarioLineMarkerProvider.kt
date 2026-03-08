package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import ewc.openspec.testlink.settings.TestToSpecSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction

class ScenarioLineMarkerProvider : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node?.elementType != KtTokens.AT) return null

        val annotation = element.parent as? KtAnnotationEntry ?: return null
        val modifierList = annotation.parent as? KtModifierList
            ?: (annotation.parent as? KtAnnotationUseSiteTarget)?.parent as? KtModifierList
            ?: return null
        val owningDeclaration = modifierList.parent
        if (owningDeclaration !is KtNamedFunction) return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)

        if (settings.state.scenarioAnnotationFqn.isBlank()) return null

        val expectedShortName = settings.state.scenarioAnnotationFqn.substringAfterLast('.')
        if (annotation.shortName?.asString() != expectedShortName) return null

        val capability = extractStringArgument(annotation, settings.state.capabilityAttribute) ?: return null
        val scenarioName = extractStringArgument(annotation, settings.state.valueAttribute) ?: return null

        // element IS the AT token (guarded by the KtTokens.AT check above), so it already anchors the gutter icon
        // to the '@' sign. KtAnnotationEntry.atSign does not exist in this version of the Kotlin PSI API,
        // so we use element directly rather than going through the annotation.
        log.debug("TestToSpec: marker for '$scenarioName' (capability='$capability') on function '${owningDeclaration.name}'")

        return ScenarioMarkerHelper.makeLineMarkerInfo(element, listOf(ScenarioRef(capability, scenarioName)), project)
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
}
