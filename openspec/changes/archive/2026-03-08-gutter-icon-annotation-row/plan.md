# Gutter Icon Annotation Row — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move the `@Scenario` gutter icon from the function/method declaration row to the `@Scenario` annotation row, producing one icon per annotation.

**Architecture:** Change both `LineMarkerProvider` implementations to trigger on the annotation token (`@` in Kotlin, annotation name identifier in Java) rather than the function name identifier. Each annotation produces an independent `LineMarkerInfo`. Tests use the IntelliJ Platform light test framework with in-memory PSI files. The `@Scenario` annotation is also created in the test sources for dogfooding — it annotates the test methods themselves.

**Tech Stack:** Kotlin, IntelliJ Platform Gradle Plugin 2.x, IntelliJ Platform light test framework (`LightJavaCodeInsightFixtureTestCase`), JUnit 4.

---

## Task 1: Add test infrastructure to build.gradle.kts

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Add the import and test dependencies**

Add `import org.jetbrains.intellij.platform.gradle.TestFrameworkType` at the top of `build.gradle.kts`, then extend the `dependencies` block:

```kotlin
import org.jetbrains.intellij.platform.gradle.TestFrameworkType  // add at top

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)   // ADD THIS
    }
    implementation("org.commonmark:commonmark:0.24.0")
    testImplementation("junit:junit:4.13.2")        // ADD THIS
}
```

**Step 2: Verify the build compiles**

```bash
./gradlew compileTestKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: add IntelliJ Platform test framework dependency"
```

---

## Task 2: Create the @Scenario annotation for dogfooding

**Files:**
- Create: `src/test/kotlin/ewc/openspec/testlink/Scenario.kt`

**Step 1: Create the annotation class**

```kotlin
package ewc.openspec.testlink

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Scenario(val capability: String, val value: String)
```

This annotation is defined in test sources so it can be used to annotate our own test methods. When a developer opens this project with the plugin installed and configured, the gutter icons will appear on these annotation rows — demonstrating the feature being built.

**Step 2: Verify it compiles**

```bash
./gradlew compileTestKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/test/kotlin/ewc/openspec/testlink/Scenario.kt
git commit -m "test: add @Scenario annotation for dogfooding"
```

---

## Task 3: Write the first failing Kotlin test (single annotation)

**Files:**
- Create: `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderTest.kt`

**Step 1: Create the test class with a shared helper**

```kotlin
package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderTest : LightJavaCodeInsightFixtureTestCase() {

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
}
```

**Step 2: Run the test to confirm it fails**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderTest.test single Scenario annotation produces icon on annotation row not fun row"
```

Expected: FAIL — the marker is currently on the `fun` row, not the `@Scenario` row.

---

## Task 4: Implement the Kotlin provider changes

**Files:**
- Modify: `src/main/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProvider.kt`

**Step 1: Replace the entire `getLineMarkerInfo` and update imports**

Remove the imports for `KtClass`, `KtNamedDeclaration`. Add `KtAnnotationEntry`. Replace `getLineMarkerInfo`:

```kotlin
package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import ewc.openspec.testlink.settings.TestToSpecSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction

class ScenarioLineMarkerProvider : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node?.elementType != KtTokens.AT) return null

        val annotation = element.parent as? KtAnnotationEntry ?: return null
        val owningDeclaration = annotation.parent?.parent
        if (owningDeclaration !is KtNamedFunction) return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)

        if (settings.state.scenarioAnnotationFqn.isBlank()) return null

        val expectedShortName = settings.state.scenarioAnnotationFqn.substringAfterLast('.')
        if (annotation.shortName?.asString() != expectedShortName) return null

        val capability = extractStringArgument(annotation, settings.state.capabilityAttribute) ?: return null
        val scenarioName = extractStringArgument(annotation, settings.state.valueAttribute) ?: return null

        val anchor = annotation.atSign ?: annotation.firstChild
        log.debug("TestToSpec: marker for '$scenarioName' anchored to ${anchor.node?.elementType}")

        return ScenarioMarkerHelper.makeLineMarkerInfo(anchor, listOf(ScenarioRef(capability, scenarioName)), project)
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
```

**Note on PSI parent chain**: `KtAnnotationEntry` sits inside a `KtAnnotationUseSiteTarget` or directly inside `KtModifierList`. The owning declaration is the parent of the `KtModifierList`. So `annotation.parent?.parent` navigates: `KtAnnotationEntry → KtModifierList → KtNamedFunction`. Verify this in the debugger if the test still fails after this step.

**Step 2: Run the failing test**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderTest.test single Scenario annotation produces icon on annotation row not fun row"
```

Expected: PASS

**Step 3: Commit**

```bash
git add src/main/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProvider.kt
git commit -m "feat: trigger Kotlin gutter icon on @Scenario annotation row"
```

---

## Task 5: Write and run remaining Kotlin tests

**Files:**
- Modify: `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderTest.kt`

**Step 1: Add the remaining test methods to `ScenarioLineMarkerProviderTest`**

```kotlin
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
```

**Step 2: Run all Kotlin tests**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderTest"
```

Expected: all 4 tests PASS

**Step 3: Commit**

```bash
git add src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderTest.kt
git commit -m "test: Kotlin provider — annotation row placement, multiple annotations, class guard, tooltip"
```

---

## Task 6: Write the first failing Java test (single annotation)

**Files:**
- Create: `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJavaTest.kt`

**Step 1: Create the Java test class**

```kotlin
package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderJavaTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        TestToSpecSettings.getInstance(project).state.apply {
            scenarioAnnotationFqn = "ewc.openspec.testlink.Scenario"
            capabilityAttribute = "capability"
            valueAttribute = "value"
        }
        // Add the annotation class so Java PSI can resolve its FQN
        myFixture.addFileToProject(
            "ewc/openspec/testlink/Scenario.java",
            """
            package ewc.openspec.testlink;
            public @interface Scenario {
                String capability();
                String value();
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
}
```

**Step 2: Run the test to confirm it fails**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderJavaTest.test single Scenario annotation on Java method produces icon on annotation row"
```

Expected: FAIL — marker currently on the method declaration row.

---

## Task 7: Implement the Java provider changes

**Files:**
- Modify: `src/main/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJava.kt`

**Step 1: Replace the entire provider**

```kotlin
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
import ewc.openspec.testlink.settings.TestToSpecSettings

class ScenarioLineMarkerProviderJava : LineMarkerProvider {

    private val log = Logger.getInstance(ScenarioLineMarkerProviderJava::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null
        val ref = element.parent as? PsiJavaCodeReferenceElement ?: return null
        val annotation = ref.parent as? PsiAnnotation ?: return null
        val method = annotation.owner as? PsiMethod ?: return null

        val project = element.project
        val settings = TestToSpecSettings.getInstance(project)
        val fqn = settings.state.scenarioAnnotationFqn

        log.debug("TestToSpec Java: checking annotation on '${method.name}', fqn='$fqn'")

        if (fqn.isBlank()) return null
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
```

**Note on `annotation.owner`**: `PsiAnnotation.getOwner()` returns the element the annotation is applied to — typically the `PsiModifierList`. Cast its parent to `PsiMethod`. If `annotation.owner` is null or the owner's parent is not a `PsiMethod`, return null (guards against class-level annotations).

**Step 2: Run the failing test**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderJavaTest.test single Scenario annotation on Java method produces icon on annotation row"
```

Expected: PASS

**Step 3: Commit**

```bash
git add src/main/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJava.kt
git commit -m "feat: trigger Java gutter icon on @Scenario annotation row"
```

---

## Task 8: Write and run remaining Java tests

**Files:**
- Modify: `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJavaTest.kt`

**Step 1: Add the remaining test methods**

```kotlin
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
```

**Step 2: Run all Java tests**

```bash
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderJavaTest"
```

Expected: all 3 tests PASS

**Step 3: Run the full test suite**

```bash
./gradlew test
```

Expected: all tests PASS

**Step 4: Commit**

```bash
git add src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJavaTest.kt
git commit -m "test: Java provider — annotation row placement, multiple annotations, class guard"
```

---

## Task 9: Manual verification checklist

Before closing the change, verify manually with the plugin installed in a sandbox IDE:

- [ ] `@Scenario` annotation on a Kotlin function → icon on annotation row, not on `fun` row
- [ ] `@Scenario` annotation on a Java method → icon on annotation row, not on method row
- [ ] Two `@Scenario` annotations on one function → two separate icons, one per row
- [ ] Hovering an icon → tooltip reads "View scenario: \<name\>"
- [ ] Clicking an icon → scenario popup appears with spec content
- [ ] Cmd/Ctrl-click → navigates to the scenario in the spec file
- [ ] Gutter column no wider than with a single icon (test runner icon and scenario icon no longer share a row)
