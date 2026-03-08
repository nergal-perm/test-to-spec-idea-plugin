package ewc.openspec.testlink

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class TestFinderTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TestToSpecSettings.getInstance(project).state.apply {
            scenarioAnnotationFqn = "ewc.openspec.testlink.Scenario"
            capabilityAttribute = "capability"
            valueAttribute = "value"
        }
        myFixture.addFileToProject(
            "ewc/openspec/testlink/Scenario.kt",
            """
            package ewc.openspec.testlink
            @Repeatable
            @Target(AnnotationTarget.FUNCTION)
            annotation class Scenario(val capability: String, val value: String)
            """.trimIndent()
        )
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Filled icon when one test references the scenario")
    fun `test returns empty when capability does not match`() {
        myFixture.addFileToProject(
            "src/MyTest.kt",
            """
            package test
            import ewc.openspec.testlink.Scenario
            class MyTest {
                @Scenario(capability = "other-cap", value = "My Scenario")
                fun testSomething() {}
            }
            """.trimIndent()
        )
        val results = TestFinder.find(project, "my-cap", "My Scenario")
        assertTrue(results.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Filled icon when one test references the scenario")
    fun `test returns empty when scenario name does not match`() {
        myFixture.addFileToProject(
            "src/MyTest2.kt",
            """
            package test
            import ewc.openspec.testlink.Scenario
            class MyTest2 {
                @Scenario(capability = "my-cap", value = "Other Scenario")
                fun testSomething() {}
            }
            """.trimIndent()
        )
        val results = TestFinder.find(project, "my-cap", "My Scenario")
        assertTrue(results.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Filled icon when multiple tests reference the scenario")
    fun `test returns multiple results when several methods reference the same scenario`() {
        myFixture.addFileToProject(
            "src/TestA.kt",
            """
            package test
            import ewc.openspec.testlink.Scenario
            class TestA {
                @Scenario(capability = "my-cap", value = "My Scenario")
                fun testOne() {}
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/TestB.kt",
            """
            package test
            import ewc.openspec.testlink.Scenario
            class TestB {
                @Scenario(capability = "my-cap", value = "My Scenario")
                fun testTwo() {}
            }
            """.trimIndent()
        )
        val results = TestFinder.find(project, "my-cap", "My Scenario")
        assertEquals(2, results.size)
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Blank annotation FQN setting produces no gutter icon")
    fun `test returns empty when FQN is blank`() {
        TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
        val results = TestFinder.find(project, "my-cap", "My Scenario")
        assertTrue(results.isEmpty())
    }

    @Scenario(capability = "spec-to-test-gutter-link", value = "Filled icon when one test references the scenario")
    fun `test finds matching Kotlin function`() {
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
        val results = TestFinder.find(project, "my-cap", "My Scenario")
        assertEquals(1, results.size)
        assertEquals("testSomething", results[0].name)
    }
}
