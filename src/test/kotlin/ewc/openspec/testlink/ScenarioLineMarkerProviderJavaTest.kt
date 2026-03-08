package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderJavaTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TestToSpecSettings.getInstance(project).state.apply {
            scenarioAnnotationFqn = "ewc.openspec.testlink.Scenario"
            capabilityAttribute = "capability"
            valueAttribute = "value"
        }
        // Add the annotation class so Java PSI can resolve its FQN.
        // @Repeatable is declared so the fixture mirrors real-world Java annotation usage
        // (required when multiple @Scenario annotations appear on the same method).
        myFixture.addFileToProject(
            "ewc/openspec/testlink/Scenario.java",
            """
            package ewc.openspec.testlink;
            import java.lang.annotation.*;
            @Repeatable(Scenario.Container.class)
            public @interface Scenario {
                String capability();
                String value();
                @interface Container { Scenario[] value(); }
            }
            """.trimIndent()
        )
    }

    private fun collectMarkers(javaCode: String): List<LineMarkerInfo<*>> {
        val file = myFixture.configureByText("Test.java", javaCode)
        val provider = ScenarioLineMarkerProviderJava()
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

    private fun annotationLinesIn(code: String): List<Int> =
        code.lines().mapIndexedNotNull { i, l -> if (l.trimStart().startsWith("@Scenario")) i else null }

    @Scenario(capability = "scenario-gutter-marker", value = "Single annotation on a Java method")
    fun `test single Scenario annotation on Java method produces icon on annotation row`() {
        val code = """
            import ewc.openspec.testlink.Scenario;
            public class Test {
                @Scenario(capability = "cap", value = "s1")
                public void testFoo() {}
            }
        """.trimIndent()
        val markers = collectMarkers(code)
        assertEquals(1, markers.size)
        assertEquals(annotationLinesIn(code).first(), lineOf(markers[0]))
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Multiple annotations on a Java method")
    fun `test multiple Scenario annotations on Java method each produce their own icon`() {
        val code = """
            import ewc.openspec.testlink.Scenario;
            public class Test {
                @Scenario(capability = "cap", value = "s1")
                @Scenario(capability = "cap2", value = "s2")
                public void testFoo() {}
            }
        """.trimIndent()
        val markers = collectMarkers(code)
        assertEquals(2, markers.size)
        val markerLines = markers.map { lineOf(it) }.sorted()
        assertEquals(annotationLinesIn(code), markerLines)
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Blank FQN produces no gutter icon on Java method")
    fun `test blank FQN setting produces no gutter icon on Java method`() {
        TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
        val code = """
            import ewc.openspec.testlink.Scenario;
            public class Test {
                @Scenario(capability = "cap", value = "s1")
                public void testFoo() {}
            }
        """.trimIndent()
        val markers = collectMarkers(code)
        assertTrue("Expected no markers when FQN is blank", markers.isEmpty())
    }

    @Scenario(capability = "scenario-gutter-marker", value = "Annotation on a Java class produces no gutter icon")
    fun `test Scenario annotation on Java class produces no gutter icon`() {
        val code = """
            import ewc.openspec.testlink.Scenario;
            @Scenario(capability = "cap", value = "s1")
            public class TestClass {}
        """.trimIndent()
        val markers = collectMarkers(code)
        assertTrue("Expected no markers for class-level annotation", markers.isEmpty())
    }
}
