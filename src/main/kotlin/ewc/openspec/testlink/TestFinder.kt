package ewc.openspec.testlink

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import ewc.openspec.testlink.settings.TestToSpecSettings

object TestFinder {

    fun find(project: Project, capability: String, scenarioName: String): List<PsiMethod> {
        val settings = TestToSpecSettings.getInstance(project)
        val fqn = settings.state.scenarioAnnotationFqn
        if (fqn.isBlank()) return emptyList()

        val scope = GlobalSearchScope.projectScope(project)
        val annotationClass = JavaPsiFacade.getInstance(project).findClass(fqn, scope)
            ?: return emptyList()

        return AnnotatedMembersSearch.search(annotationClass, scope)
            .filterIsInstance<PsiMethod>()
            .distinct()
            .filter { method ->
                method.annotations
                    .filter { it.qualifiedName == fqn }
                    .any { annotation ->
                        val cap = (annotation.findAttributeValue(settings.state.capabilityAttribute)
                                as? PsiLiteralExpression)?.value as? String
                        val name = (annotation.findAttributeValue(settings.state.valueAttribute)
                                as? PsiLiteralExpression)?.value as? String
                        cap == capability && name == scenarioName
                    }
            }
    }
}
