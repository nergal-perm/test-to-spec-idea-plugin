package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TestToSpecSettings.getInstance(project).state.apply {
            scenarioAnnotationFqn = "ewc.openspec.testlink.Scenario"
            capabilityAttribute = "capability"
            valueAttribute = "value"
        }
    }

    private fun collectMarkers(code: String): List<LineMarkerInfo<*>> {
        val file = myFixture.configureByText("Test.kt", code)
        val provider = ScenarioLineMarkerProvider()
        val result = mutableListOf<LineMarkerInfo<*>>()
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                provider.getLineMarkerInfo(element)?.let { result.add(it) }
            }
        })
        return result
    }

    private fun lineOf(marker: LineMarkerInfo<*>): Int {
        val doc = PsiDocumentManager.getInstance(project).getDocument(myFixture.file)!!
        return doc.getLineNumber(marker.startOffset)
    }

    private fun annotationLineIn(code: String): Int =
        code.lines().indexOfFirst { it.trimStart().startsWith("@Scenario") }

    @Scenario(capability = "scenario-gutter-marker", value = "Single annotation on a Kotlin function")
    fun `test single Scenario annotation produces icon on annotation row not fun row`() {
        val code = """
            annotation class Scenario(val capability: String, val value: String)
            @Scenario(capability = "cap", value = "s1")
            fun testFoo() {}
        """.trimIndent()
        val markers = collectMarkers(code)
        assertEquals(1, markers.size)
        assertEquals(annotationLineIn(code), lineOf(markers[0]))
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Multiple annotations on a Kotlin function")
    fun `test multiple Scenario annotations each produce their own icon`() {
        val code = """
            annotation class Scenario(val capability: String, val value: String)
            @Scenario(capability = "cap", value = "s1")
            @Scenario(capability = "cap2", value = "s2")
            fun testFoo() {}
        """.trimIndent()
        val markers = collectMarkers(code)
        assertEquals(2, markers.size)
        val lines = markers.map { lineOf(it) }.sorted()
        val annotationLines = code.lines()
            .mapIndexedNotNull { i, l -> if (l.trimStart().startsWith("@Scenario")) i else null }
        assertEquals(annotationLines, lines)
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Annotation on a Kotlin class produces no gutter icon")
    fun `test Scenario annotation on Kotlin class produces no gutter icon`() {
        val code = """
            annotation class Scenario(val capability: String, val value: String)
            @Scenario(capability = "cap", value = "s1")
            class TestClass
        """.trimIndent()
        val markers = collectMarkers(code)
        assertTrue("Expected no markers for class-level annotation", markers.isEmpty())
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Blank FQN produces no gutter icon")
    fun `test blank FQN setting produces no gutter icon on Kotlin function`() {
        TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
        val code = """
            annotation class Scenario(val capability: String, val value: String)
            @Scenario(capability = "cap", value = "s1")
            fun testFoo() {}
        """.trimIndent()
        val markers = collectMarkers(code)
        assertTrue("Expected no markers when FQN is blank", markers.isEmpty())
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Tooltip on annotation-row icon")
    fun `test tooltip reads View scenario colon scenario name`() {
        val code = """
            annotation class Scenario(val capability: String, val value: String)
            @Scenario(capability = "cap", value = "my scenario")
            fun testFoo() {}
        """.trimIndent()
        val markers = collectMarkers(code)
        assertEquals(1, markers.size)
        assertEquals("View scenario: my scenario", markers[0].lineMarkerTooltip)
    }
}
