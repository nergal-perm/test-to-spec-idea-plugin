## Context

The plugin currently provides one-way navigation: test → spec. The `ScenarioLineMarkerProvider` (Kotlin) and `ScenarioLineMarkerProviderJava` fire on `@` tokens in source files, read `spec.md` via `SpecReader`, and render a popup or navigate on click. The reverse direction — spec → test — requires a `LineMarkerProvider` registered for Markdown files plus a mechanism to find annotated test methods by capability and scenario name.

IntelliJ 2024.3 bundles the Markdown plugin (`org.intellij.plugins.markdown`), which provides a full PSI tree for `.md` files including typed heading elements. The plugin already has `TestToSpecSettings` with all fields needed for the reverse lookup (`scenarioAnnotationFqn`, `capabilityAttribute`, `valueAttribute`, `specRootPath`) — no settings changes are required.

## Goals / Non-Goals

**Goals:**
- Gutter icon on every `Scenario:` heading row in spec `.md` files
- Filled icon when ≥1 test references the scenario; greyed icon when 0
- Single match → direct navigation; multiple matches → IDEA's standard navigation chooser (class + method)
- Greyed icon is non-interactive
- Reuse existing settings; no schema changes

**Non-Goals:**
- Indexing scenarios across multiple projects or workspaces
- Real-time annotation argument evaluation (string constants only, no computed values)
- Support for Java `@Repeatable` container annotations in the finder (handled transparently by `AnnotatedMembersSearch`)

## Decisions

### 1. Markdown PSI anchor point

**Decision:** Anchor the gutter icon to the ATX heading content element (`MarkdownTokenTypes.ATX_CONTENT` or the heading's text child), not the `#` prefix token.

**Rationale:** The heading content node contains the full `Scenario: <name>` text needed for extraction in a single `element.text` call. Anchoring to `#` would require walking up and then down the PSI tree on every call. The content node is unique per heading line, satisfying `LineMarkerProvider`'s requirement for a leaf or near-leaf anchor.

**Alternative considered:** Document-level line scanning (no PSI) — rejected because it would require manual line number tracking and bypass IntelliJ's incremental PSI invalidation, causing stale markers.

### 2. Test lookup strategy

**Decision:** `AnnotatedMembersSearch` over test source roots, resolved at click time (not eagerly on file open).

**Rationale:** `AnnotatedMembersSearch` uses IntelliJ's stub index — fast, incremental, correct. Scoping to `GlobalSearchScopes.projectTestScope(project)` (test source roots only) avoids false positives from production code. Resolving at click time avoids redundant index queries for headings the user never clicks; the stub index is fast enough that there is no perceptible latency.

**Alternative considered:** Custom `FileBasedIndex` — rejected as over-engineering. The stub index already stores annotation arguments; a custom index would duplicate that work.

**Alternative considered:** Eager lookup on file open — rejected because it would query the index for every heading in every spec file on open, with results immediately discarded if the user doesn't click.

### 3. Capability derivation

**Decision:** Derive capability from the spec file's `VirtualFile` path by stripping the `{basePath}/{specRootPath}/` prefix and taking the first remaining path segment.

**Rationale:** Pure string operation, no PSI or I/O, zero chance of failure except when the file is genuinely outside the configured spec root (in which case returning no icon is correct). The capability folder name is already the single source of truth per the existing `SpecReader` convention.

### 4. Icon for zero-match state

**Decision:** Add a new `Icons.SCENARIO_UNLINKED` (greyed/dimmed variant of the existing `Icons.SCENARIO`) rather than reusing the same icon.

**Rationale:** Visual distinction between "click to see tests" and "no tests exist for this scenario" is valuable during spec authoring. A dimmed icon is immediately recognisable without requiring the user to click to discover the empty state. Non-interactive (no click handler registered).

### 5. Selector UI

**Decision:** Use `PsiElementListNavigator.openTargets()` with a `DefaultPsiElementCellRenderer` for the multi-match chooser.

**Rationale:** This is the standard IDEA mechanism used by "Go to Implementation", "Go to Declaration", etc. It gives us the correct look, keyboard navigation, preview on hover, and correct focus handling for free. No custom UI required.

## Risks / Trade-offs

**Markdown plugin PSI stability** → The Markdown plugin's PSI API (`MarkdownElementTypes`, `MarkdownTokenTypes`) is not part of the stable IntelliJ Platform API and could change between IDE versions. Mitigation: pin `sinceBuild = "243"` (already set) and add a smoke test that verifies heading element detection after platform upgrades.

**Annotation argument matching is literal-only** → `AnnotatedMembersSearch` returns `PsiMethod`s; we then read annotation argument values from PSI. String constants defined as `const val` references will not resolve to their value — only string literals in the annotation call site are matched. Mitigation: document this limitation; it matches actual usage in almost all projects.

**Cold index on first open** → On a freshly cloned project before the first index build completes, `AnnotatedMembersSearch` returns nothing and all icons appear greyed. Mitigation: this is inherent to any stub-index approach; the icons update automatically once indexing finishes via normal PSI listener invalidation.

## Open Questions

None — all design decisions resolved during brainstorm review.
