# Spec-to-Test Gutter Link Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add gutter icons to `Scenario:` headings in spec `.md` files that navigate back to annotated test methods.

**Architecture:** A new `SpecLineMarkerProvider` registered for the `Markdown` language fires on `ATX_HEADER` tokens, extracts the scenario name from the parent heading element, derives the capability from the file path, and delegates test discovery to a new `TestFinder` object that uses `AnnotatedMembersSearch` over test source roots. Icons, navigation, and settings are reused from the existing plugin.

**Tech Stack:** Kotlin, IntelliJ Platform SDK 2024.3, `org.intellij.plugins.markdown` (bundled), `AnnotatedMembersSearch`, `PsiElementListNavigator`, JUnit 4 + `BasePlatformTestCase`.

**TDD Rule:** Write one failing test. Run it and confirm it fails. Write only the code needed to make it pass. Run again and confirm it passes. Then commit. Never implement ahead of a test.

---

## 1. Greyed Icon Asset

- [ ] 1.1 Create `src/main/resources/icons/scenario_unlinked.svg` — copy `scenario.svg` and change the three `stroke="#389FD6"` lines to `stroke="#C0C0C0"` to produce a visually dimmed variant
- [ ] 1.2 Create `src/main/resources/icons/scenario_unlinked_dark.svg` — same approach using `scenario_dark.svg` as the base
- [ ] 1.3 Add `SCENARIO_UNLINKED` to `Icons.kt`:
  ```kotlin
  @JvmField
  val SCENARIO_UNLINKED: Icon = IconLoader.getIcon("/icons/scenario_unlinked.svg", Icons::class.java)
  ```
- [ ] 1.4 Run `./gradlew compileKotlin` — must compile cleanly
- [ ] 1.5 Commit: `feat: add SCENARIO_UNLINKED greyed icon asset`

## 2. Markdown Plugin Dependency

- [ ] 2.1 In `build.gradle.kts`, inside the `intellijPlatform { }` dependencies block, add:
  ```kotlin
  bundledPlugin("org.intellij.plugins.markdown")
  ```
- [ ] 2.2 Run `./gradlew compileKotlin` — confirms the Markdown PSI classes (`MarkdownTokenTypes`, `MarkdownElementTypes`) are now on the classpath
- [ ] 2.3 Commit: `build: add bundled Markdown plugin dependency`

## 3. TestFinder — TDD, one scenario at a time

**Files:**
- Create: `src/main/kotlin/ewc/openspec/testlink/TestFinder.kt`
- Create: `src/test/kotlin/ewc/openspec/testlink/TestFinderTest.kt`

### 3.1 — Finds a matching Kotlin function

- [ ] 3.1 Write the failing test in `TestFinderTest.kt`:
  ```kotlin
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
  ```
- [ ] 3.2 Run `./gradlew test --tests "ewc.openspec.testlink.TestFinderTest.test finds matching Kotlin function"` — confirm it fails with "unresolved reference: TestFinder"
- [ ] 3.3 Create `TestFinder.kt` with minimal implementation:
  ```kotlin
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
  ```
- [ ] 3.4 Run the test again — confirm it passes
- [ ] 3.5 Commit: `feat: add TestFinder with basic Kotlin function lookup`

### 3.2 — Returns empty when capability does not match

- [ ] 3.6 Add to `TestFinderTest`:
  ```kotlin
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
  ```
- [ ] 3.7 Run `./gradlew test --tests "ewc.openspec.testlink.TestFinderTest"` — confirm new test passes (filtering already implemented)
- [ ] 3.8 Commit: `test: verify TestFinder filters by capability`

### 3.3 — Returns empty when scenario name does not match

- [ ] 3.9 Add to `TestFinderTest`:
  ```kotlin
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
  ```
- [ ] 3.10 Run `./gradlew test --tests "ewc.openspec.testlink.TestFinderTest"` — confirm passes
- [ ] 3.11 Commit: `test: verify TestFinder filters by scenario name`

### 3.4 — Returns multiple results

- [ ] 3.12 Add to `TestFinderTest`:
  ```kotlin
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
  ```
- [ ] 3.13 Run `./gradlew test --tests "ewc.openspec.testlink.TestFinderTest"` — confirm passes
- [ ] 3.14 Commit: `test: verify TestFinder returns multiple results`

### 3.5 — Returns empty when FQN is blank

- [ ] 3.15 Add to `TestFinderTest`:
  ```kotlin
  @Scenario(capability = "spec-to-test-gutter-link", value = "Blank annotation FQN setting produces no gutter icon")
  fun `test returns empty when FQN is blank`() {
      TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
      val results = TestFinder.find(project, "my-cap", "My Scenario")
      assertTrue(results.isEmpty())
  }
  ```
- [ ] 3.16 Run `./gradlew test --tests "ewc.openspec.testlink.TestFinderTest"` — confirm passes (early return already in place)
- [ ] 3.17 Commit: `test: verify TestFinder short-circuits on blank FQN`

## 4. SpecLineMarkerProvider — TDD, one scenario at a time

**Files:**
- Create: `src/main/kotlin/ewc/openspec/testlink/SpecLineMarkerProvider.kt`
- Create: `src/test/kotlin/ewc/openspec/testlink/SpecLineMarkerProviderTest.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Markdown PSI note:** `MarkdownTokenTypes.ATX_HEADER` is the leaf token for the `####` prefix of ATX headings. Its parent element (`element.parent`) is the heading composite (`MarkdownElementTypes.ATX_1` through `ATX_6`). The full heading text including `#` prefix is in `element.parent.text`. Strip leading `#` characters and spaces to get the heading content.

If `ATX_HEADER` is not the right token type in your IntelliJ version, open **Tools → PSI Viewer** on a `spec.md` file to inspect the actual tree and adjust the token type check accordingly.

### 4.1 — Scenario heading shows a gutter icon

- [ ] 4.1 Create `SpecLineMarkerProviderTest.kt`:
  ```kotlin
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
  }
  ```
- [ ] 4.2 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest.test scenario heading shows a gutter icon"` — confirm it fails with "unresolved reference: SpecLineMarkerProvider"
- [ ] 4.3 Register in `plugin.xml` (inside `<extensions defaultExtensionNs="com.intellij">`):
  ```xml
  <codeInsight.lineMarkerProvider
          language="Markdown"
          implementationClass="ewc.openspec.testlink.SpecLineMarkerProvider"/>
  ```
- [ ] 4.4 Create `SpecLineMarkerProvider.kt` with minimal implementation:
  ```kotlin
  package ewc.openspec.testlink

  import com.intellij.codeInsight.daemon.LineMarkerInfo
  import com.intellij.codeInsight.daemon.LineMarkerProvider
  import com.intellij.openapi.editor.markup.GutterIconRenderer
  import com.intellij.psi.PsiElement
  import ewc.openspec.testlink.settings.TestToSpecSettings
  import org.intellij.plugins.markdown.lang.MarkdownTokenTypes

  class SpecLineMarkerProvider : LineMarkerProvider {

      override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
          if (element.node?.elementType != MarkdownTokenTypes.ATX_HEADER) return null

          val headingText = element.parent?.text?.trimStart('#')?.trim() ?: return null
          val match = SCENARIO_REGEX.matchEntire(headingText) ?: return null
          val scenarioName = match.groupValues[1].trim()

          val project = element.project
          val settings = TestToSpecSettings.getInstance(project)
          if (settings.state.scenarioAnnotationFqn.isBlank()) return null

          val capability = deriveCapability(element, settings) ?: return null

          return LineMarkerInfo(
              element,
              element.textRange,
              Icons.SCENARIO_UNLINKED,
              { "Scenario: $scenarioName" },
              null,
              GutterIconRenderer.Alignment.LEFT,
              { "Scenario: $scenarioName" }
          )
      }

      private fun deriveCapability(element: PsiElement, settings: TestToSpecSettings): String? {
          val project = element.project
          val basePath = project.basePath ?: return null
          val specRoot = settings.state.specRootPath
          val filePath = element.containingFile?.virtualFile?.path ?: return null
          val prefix = "$basePath/$specRoot/"
          if (!filePath.startsWith(prefix)) return null
          val relative = filePath.removePrefix(prefix)
          return relative.substringBefore('/')
              .takeIf { it.isNotEmpty() && it != relative }
      }

      companion object {
          private val SCENARIO_REGEX = """Scenario:\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)
      }
  }
  ```
- [ ] 4.5 Run the test — confirm it passes
- [ ] 4.6 Commit: `feat: add SpecLineMarkerProvider stub with heading detection`

### 4.2 — Non-scenario heading shows no icon

- [ ] 4.7 Add to `SpecLineMarkerProviderTest`:
  ```kotlin
  @Scenario(capability = "spec-to-test-gutter-link", value = "Non-scenario heading shows no gutter icon")
  fun `test non-scenario heading shows no gutter icon`() {
      val markers = collectMarkers(
          "openspec/my-cap/spec.md",
          "#### Introduction\n\nSome content."
      )
      assertTrue(markers.isEmpty())
  }
  ```
- [ ] 4.8 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest"` — confirm passes (regex already filters non-Scenario headings)
- [ ] 4.9 Commit: `test: verify non-scenario heading produces no marker`

### 4.3 — Blank FQN setting shows no icon

- [ ] 4.10 Add to `SpecLineMarkerProviderTest`:
  ```kotlin
  @Scenario(capability = "spec-to-test-gutter-link", value = "Blank annotation FQN setting produces no gutter icon")
  fun `test blank FQN setting produces no gutter icon`() {
      TestToSpecSettings.getInstance(project).state.scenarioAnnotationFqn = ""
      val markers = collectMarkers(
          "openspec/my-cap/spec.md",
          "#### Scenario: My Scenario\n\nSome content."
      )
      assertTrue(markers.isEmpty())
  }
  ```
- [ ] 4.11 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest"` — confirm passes
- [ ] 4.12 Commit: `test: verify blank FQN produces no spec marker`

### 4.4 — File outside spec root shows no icon

- [ ] 4.13 Add to `SpecLineMarkerProviderTest`:
  ```kotlin
  @Scenario(capability = "spec-to-test-gutter-link", value = "Spec file outside configured spec root shows no gutter icon")
  fun `test file outside spec root shows no gutter icon`() {
      val markers = collectMarkers(
          "other/my-cap/spec.md",
          "#### Scenario: My Scenario\n\nSome content."
      )
      assertTrue(markers.isEmpty())
  }
  ```
- [ ] 4.14 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest"` — confirm passes (path prefix check already in `deriveCapability`)
- [ ] 4.15 Commit: `test: verify file outside spec root produces no marker`

### 4.5 — Greyed icon when no tests reference scenario

- [ ] 4.16 Add to `SpecLineMarkerProviderTest`:
  ```kotlin
  @Scenario(capability = "spec-to-test-gutter-link", value = "Greyed icon when no tests reference the scenario")
  fun `test greyed icon when no tests reference scenario`() {
      val markers = collectMarkers(
          "openspec/my-cap/spec.md",
          "#### Scenario: My Scenario\n\nSome content."
      )
      assertEquals(1, markers.size)
      assertEquals(Icons.SCENARIO_UNLINKED, markers[0].icon)
  }
  ```
- [ ] 4.17 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest"` — confirm passes (provider already returns `SCENARIO_UNLINKED` by default)
- [ ] 4.18 Commit: `test: verify greyed icon when no tests reference scenario`

### 4.6 — Filled icon when a test references the scenario

- [ ] 4.19 Add to `SpecLineMarkerProviderTest`:
  ```kotlin
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
  ```
- [ ] 4.20 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest.test filled icon when one test references scenario"` — confirm it fails (provider always returns `SCENARIO_UNLINKED`)
- [ ] 4.21 Update `SpecLineMarkerProvider.getLineMarkerInfo` to call `TestFinder` and pick the icon:
  ```kotlin
  val tests = TestFinder.find(project, capability, scenarioName)
  val icon = if (tests.isEmpty()) Icons.SCENARIO_UNLINKED else Icons.SCENARIO
  val navHandler: GutterIconNavigationHandler<PsiElement>? = if (tests.isEmpty()) null else { e, _ ->
      if (tests.size == 1) {
          (tests[0] as? NavigatablePsiElement)?.navigate(true)
      } else {
          PsiElementListNavigator.openTargets(
              e,
              tests.filterIsInstance<NavigatablePsiElement>().toTypedArray(),
              "Tests for '$scenarioName'",
              null,
              DefaultPsiElementCellRenderer()
          )
      }
  }
  return LineMarkerInfo(
      element,
      element.textRange,
      icon,
      { if (tests.isEmpty()) "Scenario: $scenarioName (no tests)" else "Go to test: ${tests[0].name}" },
      navHandler,
      GutterIconRenderer.Alignment.LEFT,
      { "Scenario: $scenarioName" }
  )
  ```
  Add required imports: `com.intellij.codeInsight.navigation.PsiElementListNavigator`, `com.intellij.ide.util.DefaultPsiElementCellRenderer`, `com.intellij.psi.NavigatablePsiElement`, `com.intellij.openapi.editor.markup.GutterIconNavigationHandler`
- [ ] 4.22 Run `./gradlew test --tests "ewc.openspec.testlink.SpecLineMarkerProviderTest"` — confirm all tests pass
- [ ] 4.23 Commit: `feat: wire TestFinder into SpecLineMarkerProvider, pick icon by coverage`

## 5. Full Test Run and Smoke Test

- [ ] 5.1 Run `./gradlew test` — all tests must pass
- [ ] 5.2 Run `./gradlew runIde` — open a project with spec files, verify gutter icons appear on Scenario headings, clicking a filled icon navigates to the test method
- [ ] 5.3 Commit any cleanup: `chore: post-integration cleanup`
