package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class SpecLineMarkerProviderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TestToSpecSettings.getInstance(project).state.apply {
            scenarioAnnotationFqn = "ewc.openspec.testlink.Scenario"
            capabilityAttribute = "capability"
            valueAttribute = "value"
            specRootPath = "openspec"
        }
    }

    private fun collectMarkers(relativePath: String, content: String): List<LineMarkerInfo<*>> {
        val file = myFixture.addFileToProject(relativePath, content)
        myFixture.openFileInEditor(file.virtualFile)
        val provider = SpecLineMarkerProvider()
        val result = mutableListOf<LineMarkerInfo<*>>()
        myFixture.file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                provider.getLineMarkerInfo(element)?.let { result.add(it) }
            }
        })
        return result
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Heading with matching scenario name shows a gutter icon")
    fun `test scenario heading shows a gutter icon`() {
        val markers = collectMarkers(
            "openspec/my-cap/spec.md",
            "#### Scenario: My Scenario\n\nSome content."
        )
        assertEquals(1, markers.size)
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Non-scenario heading shows no gutter icon")
    fun `test non-scenario heading shows no gutter icon`() {
        val markers = collectMarkers(
            "openspec/my-cap/spec.md",
            "#### Introduction\n\nSome content."
        )
        assertTrue(markers.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Blank annotation FQN setting produces no gutter icon")
    fun `test blank FQN setting produces no gutter icon`() {
        TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
        val markers = collectMarkers(
            "openspec/my-cap/spec.md",
            "#### Scenario: My Scenario\n\nSome content."
        )
        assertTrue(markers.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Spec file outside configured spec root shows no gutter icon")
    fun `test file outside spec root shows no gutter icon`() {
        val markers = collectMarkers(
            "other/my-cap/spec.md",
            "#### Scenario: My Scenario\n\nSome content."
        )
        assertTrue(markers.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Greyed icon when no tests reference the scenario")
    fun `test greyed icon when no tests reference scenario`() {
        val markers = collectMarkers(
            "openspec/my-cap/spec.md",
            "#### Scenario: My Scenario\n\nSome content."
        )
        assertEquals(1, markers.size)
        assertEquals(Icons.SCENARIO_UNLINKED, markers[0].icon)
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Filled icon when one test references the scenario")
    fun `test filled icon when one test references scenario`() {
        myFixture.addFileToProject(
            "ewc/openspec/testlink/Scenario.kt",
            """
            package ewc.openspec.testlink
            @Repeatable
            @Target(AnnotationTarget.FUNCTION)
            annotation class Scenario(val capability: String, val value: String)
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/MyTest.kt",
            """
            package test
            import ewc.openspec.testlink.Scenario
            class MyTest {
                @Scenario(capability = "my-cap", value = "My Scenario")
                fun testSomething() {}
            }
            """.trimIndent()
        )
        val markers = collectMarkers(
            "openspec/my-cap/spec.md",
            "#### Scenario: My Scenario\n\nSome content."
        )
        assertEquals(1, markers.size)
        assertEquals(Icons.SCENARIO, markers[0].icon)
    }
}
