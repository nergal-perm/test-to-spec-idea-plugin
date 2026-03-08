package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderJava : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProviderJava::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null
        val ref = element.parent as? PsiJavaCodeReferenceElement ?: return null
        val annotation = ref.parent as? PsiAnnotation ?: return null
        // PsiAnnotation.getOwner() returns the PsiModifierList, not the method directly
        val method = (annotation.owner as? PsiModifierList)?.parent as? PsiMethod
            ?: return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        val fqn = settings.state.scenarioAnnotationFqn

        if (fqn.isBlank()) return null

        log.debug("TestToSpec Java: checking annotation on '${method.name}', fqn='$fqn'")

        if (annotation.qualifiedName != fqn) return null

        val capability = (annotation.findAttributeValue(settings.state.capabilityAttribute) as? PsiLiteralExpression)?.value as? String
        log.debug("TestToSpec Java: capability='$capability'")
        capability ?: return null

        val scenarioName = (annotation.findAttributeValue(settings.state.valueAttribute) as? PsiLiteralExpression)?.value as? String
        log.debug("TestToSpec Java: value='$scenarioName'")
        scenarioName ?: return null

        return ScenarioMarkerHelper.makeLineMarkerInfo(element, listOf(ScenarioRef(capability, scenarioName)), project)
    }
}
