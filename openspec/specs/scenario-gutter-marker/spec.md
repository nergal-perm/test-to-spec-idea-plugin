## ADDED Requirements

### Requirement: Gutter icon appears on the annotation row
The plugin SHALL place the gutter icon on the same row as the `@Scenario` annotation, not on the function or method declaration row.

#### Scenario: Single annotation on a Kotlin function
- **WHEN** a Kotlin test function has one `@Scenario` annotation with a valid capability and scenario name
- **THEN** the gutter icon appears on the row containing the `@Scenario` annotation
- **THEN** no gutter icon appears on the `fun` declaration row

#### Scenario: Single annotation on a Java method
- **WHEN** a Java test method has one `@Scenario` annotation with a valid capability and scenario name
- **THEN** the gutter icon appears on the row containing the `@Scenario` annotation
- **THEN** no gutter icon appears on the method declaration row

#### Scenario: Annotation on a Kotlin class produces no gutter icon
- **WHEN** a `@Scenario` annotation appears on a Kotlin class declaration (not a function)
- **THEN** no gutter icon is produced for that annotation

#### Scenario: Annotation on a Java class produces no gutter icon
- **WHEN** a `@Scenario` annotation appears on a Java class declaration (not a method)
- **THEN** no gutter icon is produced for that annotation

> **Note**: In practice, `@Scenario` annotations are typically defined with `@Target(AnnotationTarget.FUNCTION)`, which prevents the compiler from accepting class-level usage. These scenarios guard against cases where the annotation target is broader or undefined.

### Requirement: One gutter icon per annotation
The plugin SHALL produce one independent gutter icon for each `@Scenario` annotation on a function or method.

#### Scenario: Multiple annotations on a Kotlin function
- **WHEN** a Kotlin test function has two or more `@Scenario` annotations
- **THEN** each annotation row displays its own gutter icon
- **THEN** each icon is independent and links to its own scenario

#### Scenario: Multiple annotations on a Java method
- **WHEN** a Java test method has two or more `@Scenario` annotations
- **THEN** each annotation row displays its own gutter icon
- **THEN** each icon is independent and links to its own scenario

### Requirement: Icon behaviour is unchanged
The tooltip, click action, and visual appearance of each gutter icon SHALL be identical to the previous behaviour — only the row placement changes.

#### Scenario: Tooltip on annotation-row icon
- **WHEN** the user hovers over the gutter icon on an annotation row
- **THEN** the tooltip reads "View scenario: <scenario name>" for a single scenario

#### Scenario: Click opens scenario popup
> **Testing**: Manual / UI test only — requires a running IDE instance.
- **WHEN** the user clicks the gutter icon on an annotation row
- **THEN** the scenario popup is shown with the linked spec content

#### Scenario: Cmd/Ctrl-click navigates to spec file
> **Testing**: Manual / UI test only — requires a running IDE instance.
- **WHEN** the user Cmd-clicks (macOS) or Ctrl-clicks (Windows/Linux) the gutter icon on an annotation row
- **THEN** the editor navigates to the scenario's location in the spec file
