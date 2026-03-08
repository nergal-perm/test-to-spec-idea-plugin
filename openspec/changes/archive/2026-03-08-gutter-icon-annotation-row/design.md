## Context

The plugin registers two `LineMarkerProvider` implementations — one for Kotlin (`ScenarioLineMarkerProvider`) and one for Java (`ScenarioLineMarkerProviderJava`). Both currently trigger on the function/method name identifier (a leaf PSI element) and anchor the returned `LineMarkerInfo` to that same element, placing the gutter icon on the declaration row.

The goal is to move the anchor to the `@Scenario` annotation itself, so each annotation gets its own icon on its own row.

## Goals / Non-Goals

**Goals:**
- Each `@Scenario` annotation on a function or method produces an independent gutter icon on its own row.
- Only function/method-level annotations are recognised — class-level `@Scenario` annotations do not produce a gutter icon.
- Kotlin and Java providers behave identically from the user's perspective.
- `ScenarioMarkerHelper.makeLineMarkerInfo` is not modified.

**Non-Goals:**
- Changing icon appearance, tooltip format, or click behaviour.
- Supporting annotation use-sites other than function/method declarations (classes, properties, constructors, parameters, etc.).
- Adding automated tests for popup or navigation behaviour (UI-only).

## Decisions

### Trigger on annotation token instead of function name identifier

**Kotlin**: Change the trigger element from `KtTokens.IDENTIFIER` (function name) to `KtTokens.AT` (the `@` sigil of a `KtAnnotationEntry`). When the element is the `@` token, check that its parent is a `KtAnnotationEntry` whose short name matches the configured annotation, and that the annotation's owning declaration is a `KtNamedFunction` (not a `KtClass`, property, or other declaration type). Extract capability and scenario from that entry and pass the `@` element as the anchor to `makeLineMarkerInfo`. Support for class-level `@Scenario` annotations is intentionally dropped.

Fallback: `atSign` is `@Nullable` in the PSI contract. If a matching `KtAnnotationEntry` has no `@` token in the tree, fall back to `annotation.firstChild` as the anchor. This is defensive and consistent with the plugin's existing approach.

**Java**: Change the trigger element from `PsiIdentifier` (method name) to the identifier leaf inside `PsiAnnotation.getNameReferenceElement()`. When the element is a `PsiIdentifier` whose parent is a `PsiJavaCodeReferenceElement` inside a `PsiAnnotation`, check the annotation's FQN against the configured value and pass the identifier as the anchor.

**Why this over triggering on the function name and shifting the anchor**: The `getLineMarkerInfo` contract returns one marker per call. Triggering on the annotation token naturally produces one call (and one marker) per annotation, which is exactly the "one icon per annotation" requirement. Triggering on the function name and trying to return multiple markers from one call is not possible with this API without switching to `collectSlowLineMarkers`.

### Keep `ScenarioMarkerHelper` unchanged

The helper's `makeLineMarkerInfo(element, scenarios, project)` already accepts any `PsiElement` as the anchor. Passing the annotation token instead of the function name identifier requires no signature change. The `scenarios` list will always contain exactly one `ScenarioRef` per call under the new design, but accepting a list remains harmless.

### Test approach

Light platform tests (`LightJavaCodeInsightFixtureTestCase`) for the five automatable scenarios: configure an in-memory source file, call `getLineMarkerInfo` across all leaf elements, and assert that markers are present on annotation rows and absent on declaration rows. The `lineMarkerTooltip` accessor on `LineMarkerInfo` covers the tooltip scenario without UI. The two click/navigation scenarios are documented in the spec as manual-only.

## Risks / Trade-offs

- **`atSign` nullability**: The `@` token may be absent in synthetic or partially-parsed PSI. Mitigation: fall back to `firstChild`, which is always non-null for a well-formed annotation entry.
- **K2 compiler mode (Kotlin)**: The plugin already declares K2 compatibility. The PSI token types (`KtTokens.AT`) are stable across K1/K2 and this change does not affect compatibility.
- **Multiple `@Scenario` on one function**: Previously collapsed into one icon; now each gets its own. This is intentional and specified. No migration needed — users gain icons, they lose nothing.
