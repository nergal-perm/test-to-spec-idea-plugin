# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

An IntelliJ IDEA plugin (Kotlin/Java) that adds gutter icons to test methods annotated with `@Scenario`, linking them back to their specification files. Built with the IntelliJ Platform Gradle Plugin v2.

## Commands

```bash
# Build distributable zip
./gradlew buildPlugin

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "ewc.openspec.testlink.ScenarioLineMarkerProviderTest"

# Launch sandboxed IntelliJ with plugin loaded
./gradlew runIde
```

## Architecture

The plugin has two parallel stacks — one for Kotlin PSI, one for Java PSI — that share common helpers:

- **`ScenarioLineMarkerProvider`** (Kotlin) / **`ScenarioLineMarkerProviderJava`** (Java) — `LineMarkerProvider` implementations that fire on `@` tokens. Each inspects the parent annotation, checks it matches the configured FQN, and delegates to `ScenarioMarkerHelper`.
- **`ScenarioAnnotator`** — Annotates both Kotlin and Java files; registered for both languages in `plugin.xml`.
- **`ScenarioMarkerHelper`** — Builds the `LineMarkerInfo`, handles click logic (plain click → popup, Ctrl/Cmd+click → navigate), and assembles the popup UI.
- **`SpecReader`** — Reads `{specRoot}/{capability}/spec.md` from disk/VFS and extracts scenario content by parsing heading levels.
- **`MarkdownRenderer`** — Converts markdown to HTML for the popup.
- **`SpecFileWatcher`** — `postStartupActivity` that watches spec files for live refresh.
- **`TestToSpecSettings`** — Per-project `PersistentStateComponent` stored in `.idea/testToSpec.xml`. Holds: `specRootPath`, `scenarioAnnotationFqn`, `capabilityAttribute`, `valueAttribute`.

**Data flow:** PSI element → annotation check → `ScenarioRef(capability, name)` → `SpecReader.readScenario()` → `MarkdownRenderer` → popup or navigation.

**Version** is derived at build time from the latest git tag (`git describe --tags --abbrev=0`), so a tag must exist. Dev builds fall back to `0.0.0-dev`.

## Versioning

This project uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- **PATCH** — bug fixes and internal changes with no API/behavior change
- **MINOR** — new features, backward-compatible
- **MAJOR** — breaking changes to settings schema, plugin behavior, or supported IDE range

Releases are published by pushing a version tag:

```bash
git tag v1.2.0
git push origin v1.2.0
```

GitHub Actions builds and publishes the release automatically. The tag is the single source of truth for the version — do not hardcode it elsewhere.

## Testing Strategy

Tests live in `src/test/kotlin`. There are three layers; use the lightest layer that covers the behavior:

### 1. Plain JUnit (no platform)
For pure logic (parsers, string manipulation, data transformations). Use JUnit 4 (currently on classpath). Keep all IntelliJ API calls out of this layer by pushing them behind interfaces.

### 2. Light platform tests (primary layer)
Extend `BasePlatformTestCase` (JUnit 4) for anything touching PSI, the editor, or project model. Use `myFixture.configureByText(...)` to create in-memory files. See `ScenarioLineMarkerProviderTest` and `ScenarioLineMarkerProviderJavaTest` for the established pattern:

```kotlin
class MyTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        // configure settings on `project`
    }

    fun `test something`() {
        val file = myFixture.configureByText("Test.kt", code)
        // assert via PSI or provider directly
    }
}
```

Test methods in light platform tests use `@Scenario` annotations (from `src/test/kotlin/ewc/openspec/testlink/Scenario.kt`) to link them to specs in `openspec/specs/`.

### 3. Heavy/UI tests
Avoid unless strictly necessary. Keep the vast majority of coverage in plain JUnit + light platform tests.

**Key rules:**
- Separate pure logic from IntelliJ API calls so it can be tested without the platform.
- Use `testData/` files for before/after content when testing inspections or intentions.
- The `Scenario.kt` in `src/test/` is the annotation used by the test suite itself — it is not production code.

## Spec File Convention

```
{specRoot}/
  {capability}/
    spec.md       ← scenario headings: "#### Scenario: <name>"
```

Heading level can be `#`–`######`. A scenario's content runs from its heading to the next heading of equal or lesser depth.
