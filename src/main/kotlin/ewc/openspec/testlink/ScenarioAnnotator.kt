package ewc.openspec.testlink

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiAnnotation
import ewc.openspec.testlink.settings.TestToSpecSettings
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

class ScenarioAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val settings = TestToSpecSettings.getInstance(element.project)
        val fqn = settings.state.scenarioAnnotationFqn
        if (fqn.isBlank()) return

        when (element) {
            is KtStringTemplateExpression -> annotateKotlin(element, holder, settings, fqn)
            is PsiLiteralExpression -> annotateJava(element, holder, settings, fqn)
        }
    }

    private fun annotateKotlin(
        element: KtStringTemplateExpression,
        holder: AnnotationHolder,
        settings: TestToSpecSettings,
        fqn: String
    ) {
        val valueArg = element.parent as? KtValueArgument ?: return
        if (valueArg.getArgumentName()?.asName?.asString() != settings.state.valueAttribute) return

        val argList = valueArg.parent as? KtValueArgumentList ?: return
        val annotation = argList.parent as? KtAnnotationEntry ?: return
        if (annotation.shortName?.asString() != fqn.substringAfterLast('.')) return

        val capability = argList.arguments
            .find { it.getArgumentName()?.asName?.asString() == settings.state.capabilityAttribute }
            ?.getArgumentExpression()?.text?.removeSurrounding("\"") ?: return

        val scenarioName = element.text.removeSurrounding("\"")

        if (SpecReader.readScenario(element.project, capability, scenarioName) == null) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Scenario \"$scenarioName\" not found in spec")
                .range(element)
                .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                .create()
        }
    }

    private fun annotateJava(
        element: PsiLiteralExpression,
        holder: AnnotationHolder,
        settings: TestToSpecSettings,
        fqn: String
    ) {
        val stringValue = element.value as? String ?: return
        val nameValuePair = element.parent as? PsiNameValuePair ?: return
        val attrName = nameValuePair.name ?: "value"
        if (attrName != settings.state.valueAttribute) return

        val annotation = nameValuePair.parent?.parent as? PsiAnnotation ?: return
        if (annotation.qualifiedName != fqn) return

        val capability = (annotation.findAttributeValue(settings.state.capabilityAttribute) as? PsiLiteralExpression)
            ?.value as? String ?: return

        if (SpecReader.readScenario(element.project, capability, stringValue) == null) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Scenario \"$stringValue\" not found in spec")
                .range(element)
                .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                .create()
        }
    }
}
