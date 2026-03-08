## 1. Test Infrastructure

- [x] 1.1 Add `testFramework(TestFrameworkType.Platform)` and `testImplementation("junit:junit:4.13.2")` to `build.gradle.kts`
- [x] 1.2 Verify `./gradlew compileTestKotlin` succeeds
- [x] 1.3 Commit build change

## 2. Dogfooding — @Scenario Annotation

- [x] 2.1 Create `src/test/kotlin/ewc/openspec/testlink/Scenario.kt` with `@Target(FUNCTION)` annotation class
- [x] 2.2 Verify `./gradlew compileTestKotlin` succeeds
- [x] 2.3 Commit annotation class

## 3. TDD — Kotlin Provider (single annotation)

- [x] 3.1 Create `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderTest.kt` with `collectMarkers` helper and first test: single `@Scenario` → icon on annotation row (annotate the test method itself with `@Scenario`)
- [x] 3.2 Run test, confirm it FAILS
- [x] 3.3 Rewrite `ScenarioLineMarkerProvider.getLineMarkerInfo` to trigger on `KtTokens.AT`, guard `owningDeclaration is KtNamedFunction`, anchor to `annotation.atSign ?: annotation.firstChild`
- [x] 3.4 Run test, confirm it PASSES
- [x] 3.5 Commit Kotlin provider implementation

## 4. TDD — Kotlin Provider (remaining scenarios)

- [x] 4.1 Add test: multiple `@Scenario` → one icon per annotation row (annotate test method with `@Scenario`)
- [x] 4.2 Add test: `@Scenario` on a class → no gutter icon (annotate test method with `@Scenario`)
- [x] 4.3 Add test: tooltip reads `"View scenario: <name>"` (annotate test method with `@Scenario`)
- [x] 4.4 Run all Kotlin tests, confirm they all PASS
- [x] 4.5 Commit Kotlin tests

## 5. TDD — Java Provider (single annotation)

- [x] 5.1 Create `src/test/kotlin/ewc/openspec/testlink/ScenarioLineMarkerProviderJavaTest.kt` with `Scenario.java` added to fixture and first test: single `@Scenario` → icon on annotation row (annotate test method with `@Scenario`)
- [x] 5.2 Run test, confirm it FAILS
- [x] 5.3 Rewrite `ScenarioLineMarkerProviderJava.getLineMarkerInfo` to trigger on `PsiIdentifier` inside `PsiAnnotation.getNameReferenceElement()`, guard `annotation.owner`'s parent is `PsiMethod`, check FQN via `annotation.qualifiedName`
- [x] 5.4 Run test, confirm it PASSES
- [x] 5.5 Commit Java provider implementation

## 6. TDD — Java Provider (remaining scenarios)

- [x] 6.1 Add test: multiple `@Scenario` on a Java method → one icon per annotation row (annotate test method with `@Scenario`)
- [x] 6.2 Add test: `@Scenario` on a Java class → no gutter icon (annotate test method with `@Scenario`)
- [x] 6.3 Run all Java tests, confirm they all PASS
- [x] 6.4 Run full `./gradlew test`, confirm all tests PASS
- [x] 6.5 Commit Java tests

## 7. Manual Verification

- [x] 7.1 Verify Kotlin: icon appears on `@Scenario` row, not on `fun` row
- [x] 7.2 Verify Java: icon appears on `@Scenario` row, not on method row
- [x] 7.3 Verify multiple annotations produce separate icons per row
- [x] 7.4 Verify tooltip, popup click, and Cmd/Ctrl-click navigation still work
- [x] 7.5 Verify the gutter column is no wider on the function row
