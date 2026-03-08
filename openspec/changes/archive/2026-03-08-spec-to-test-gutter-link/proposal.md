## Why

Navigating from a spec scenario to its implementing test is currently manual — you have to search the codebase for the matching `@Scenario` annotation. The plugin already bridges test → spec; this change closes the loop by making spec files navigable back to their tests.

## What Changes

- Add a gutter icon to every `#### Scenario: <name>` heading row in spec `.md` files
- Filled icon when one or more tests reference the scenario; greyed-out icon when none do
- Clicking a filled icon with a single match navigates directly to the test method
- Clicking a filled icon with multiple matches opens IDEA's standard navigation chooser (class + method name per entry), scoped to test source roots
- Greyed-out icon is non-interactive

## Capabilities

### New Capabilities

- `spec-to-test-gutter-link`: Gutter icon on spec scenario headings linking back to annotated test methods

### Modified Capabilities

- `scenario-gutter-marker`: No requirement changes — existing test→spec direction is unaffected

## Impact

- New `LineMarkerProvider` for the `Markdown` language (requires `org.intellij.plugins.markdown` bundled plugin dependency)
- New `TestFinder` component using `AnnotatedMembersSearch` against test source roots
- `build.gradle.kts`: add `bundledPlugin("org.intellij.plugins.markdown")`
- `plugin.xml`: register new line marker provider for `language="Markdown"`
- No changes to settings schema — reuses existing `scenarioAnnotationFqn`, `capabilityAttribute`, `valueAttribute`, and `specRootPath`
