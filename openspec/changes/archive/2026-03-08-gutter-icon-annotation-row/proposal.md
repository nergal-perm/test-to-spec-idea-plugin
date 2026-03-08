## Why

The gutter icon that links a test to its spec currently appears on the function/method declaration row — the same row that already hosts the test-runner icon. This creates two problems: the icon's placement doesn't signal its connection to the `@Scenario` annotation that defines the link, and the extra icon on an already-occupied gutter row forces a wider gutter column, which is visually unpleasant.

Moving the icon to the `@Scenario` annotation row fixes both: the icon is co-located with the annotation it represents, and the function declaration row is left to the test runner alone.

## What Changes

- The Kotlin line marker provider changes its trigger from the function name identifier to the `@` token of each matching `KtAnnotationEntry`. Each `@Scenario` annotation on a function produces its own gutter icon.
- The Java line marker provider changes its trigger from the method name identifier to the annotation name identifier inside `PsiAnnotation.getNameReferenceElement()`. Each `@Scenario` annotation on a method produces its own gutter icon.
- Behaviour is identical across Kotlin and Java: same icon, same tooltip, same click actions — only the row changes.
- `ScenarioMarkerHelper` is unchanged.

## Capabilities

### New Capabilities
- `scenario-gutter-marker`: Gutter icon placement and per-annotation behaviour for `@Scenario`-annotated tests.

### Modified Capabilities
<!-- None — no existing specs exist yet. -->

## Impact

- `ScenarioLineMarkerProvider.kt` — trigger logic rewritten
- `ScenarioLineMarkerProviderJava.kt` — trigger logic rewritten
- No changes to `ScenarioMarkerHelper.kt`, `SpecReader.kt`, or `plugin.xml`
