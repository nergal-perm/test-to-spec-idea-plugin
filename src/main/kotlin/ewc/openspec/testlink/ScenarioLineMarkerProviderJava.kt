package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderJava : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProviderJava::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null
        val method = element.parent as? PsiMethod ?: return null
        if (method.nameIdentifier != element) return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        val fqn = settings.state.scenarioAnnotationFqn
        log.debug("TestToSpec Java: checking method '${method.name}', fqn='$fqn'")

        if (fqn.isBlank()) return null

        val annotation = method.getAnnotation(fqn) ?: run {
            log.debug("TestToSpec Java: no annotation found on '${method.name}'")
            return null
        }

        val capability = (annotation.findAttributeValue(settings.state.capabilityAttribute) as? PsiLiteralExpression)?.value as? String
        log.debug("TestToSpec Java: capability='$capability'")
        capability ?: return null

        val scenarioName = (annotation.findAttributeValue(settings.state.valueAttribute) as? PsiLiteralExpression)?.value as? String
        log.debug("TestToSpec Java: value='$scenarioName'")
        scenarioName ?: return null

        return ScenarioMarkerHelper.makeLineMarkerInfo(element, listOf(ScenarioRef(capability, scenarioName)), project)
    }
}
