# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 2026-03-09

### Fixed
- Spec heading gutter icons no longer show duplicate navigation entries for test methods carrying multiple `@Scenario` annotations (closes #2)
- Navigation chooser now uses `PsiTargetNavigator`, replacing a deprecated API call

## [1.3.0] - 2026-03-08

### Added
- Gutter icons on `#### Scenario:` headings in spec `.md` files, linking back to annotated test methods
- Filled icon when one or more test methods reference the scenario via `@Scenario`; greyed icon when none do
- Clicking a filled icon with a single match navigates directly to the test method
- Clicking a filled icon with multiple matches opens the IDE's standard navigation chooser
- New `TestFinder` component using `AnnotatedMembersSearch` to locate test methods by capability and scenario name
- Bundled Markdown plugin declared as a runtime dependency (`org.intellij.plugins.markdown`)

## [1.2.0] - 2026-03-08

### Added
- Gutter icon now appears on the `@Scenario` annotation row instead of the function/method declaration row
- Each `@Scenario` annotation produces its own independent gutter icon — multiple annotations on the same function each get their own icon on their own row
- `@Scenario` annotation class (in test sources) used to dogfood the plugin
- Light platform tests for both Kotlin and Java gutter icon providers, covering single annotation, multiple annotations, class-level suppression, and tooltip text

### Changed
- `ScenarioLineMarkerProvider` (Kotlin) now triggers on the `@` token (`KtTokens.AT`) of a `KtAnnotationEntry` rather than the function name identifier; only function-level annotations produce icons (class-level annotations are ignored)
- `ScenarioLineMarkerProviderJava` now triggers on the `PsiIdentifier` inside `PsiAnnotation.getNameReferenceElement()` rather than the method name identifier; only method-level annotations produce icons

## [1.1.1] - 2026-03-07

### Fixed
- Scenario popup now reads spec content from IntelliJ's in-memory `Document` when available, so unsaved edits are reflected immediately without waiting for an auto-save
- Popup content refreshes automatically when switching back to a Kotlin or Java editor file (via `FileEditorManagerListener`)

## [1.1.0] - 2026-03-07

### Added
- Gutter icons on Java test methods annotated with `@Scenario`
- `ScenarioAnnotator` for Kotlin and Java: underlines the `value` attribute with a warning when the referenced scenario is not found in the spec
- `SpecFileWatcher` project activity: restarts the code analyser when any `.md` file in the spec root changes, keeping annotation underlines up to date
- Scenario popup live-refreshes when the corresponding spec file is edited on disk

### Changed
- Shared marker logic (scenario popup, navigation, `ScenarioRef`) extracted into `ScenarioMarkerHelper`
- Scenario name matching is now case-insensitive
- `SpecReader.specFilePath` extracted to eliminate duplicated path construction

### Fixed
- `MarkdownRenderer` no longer crashes Swing's HTML renderer due to unsupported CSS properties

## [1.0.1] - 2026-03-07

### Fixed
- Declared `supportsKotlinPluginMode` with `supportsK2=true` so the plugin is not disabled in K2 mode (default from IntelliJ 2025.1)

## [1.0.0] - 2026-03-07

### Added
- Initial release: gutter icons on Kotlin test functions annotated with `@Scenario`
- Click gutter icon to open a rendered Markdown popup of the linked spec scenario
- Cmd/Ctrl-click to navigate directly to the scenario in the spec file
- Configurable annotation FQN and spec root directory via plugin settings
