## ADDED Requirements

### Requirement: Gutter icon appears on every Scenario heading row in spec files
The plugin SHALL place a gutter icon on the same row as any Markdown heading that matches `Scenario: <name>` (any heading level `#` through `######`) in a spec file located under the configured spec root.

#### Scenario: Heading with matching scenario name shows a gutter icon
- **WHEN** a spec file contains a heading matching `Scenario: <name>` at any heading level
- **THEN** a gutter icon appears on that heading row

#### Scenario: Non-scenario heading shows no gutter icon
- **WHEN** a spec file contains a heading that does not match the `Scenario: <name>` pattern
- **THEN** no gutter icon appears on that heading row

#### Scenario: Blank annotation FQN setting produces no gutter icon
- **WHEN** the `@Scenario` annotation FQN setting is blank
- **THEN** no gutter icon appears on any spec heading row

#### Scenario: Spec file outside configured spec root shows no gutter icon
- **WHEN** a Markdown file containing a `Scenario:` heading is opened from outside the configured spec root path
- **THEN** no gutter icon appears on that heading row

### Requirement: Icon state reflects test coverage
The plugin SHALL display a filled icon when at least one test method references the scenario, and a visually distinct greyed icon when no test methods reference it.

#### Scenario: Filled icon when one test references the scenario
- **WHEN** exactly one test method is annotated with `@Scenario` matching the heading's capability and scenario name
- **THEN** a filled gutter icon appears on the heading row

#### Scenario: Filled icon when multiple tests reference the scenario
- **WHEN** two or more test methods are annotated with `@Scenario` matching the heading's capability and scenario name
- **THEN** a filled gutter icon appears on the heading row

#### Scenario: Greyed icon when no tests reference the scenario
- **WHEN** no test method is annotated with `@Scenario` matching the heading's capability and scenario name
- **THEN** a greyed (visually dimmed) gutter icon appears on the heading row

### Requirement: Filled icon navigates to referencing test methods
Clicking a filled gutter icon SHALL navigate to the referencing test method(s). A single match navigates directly; multiple matches open the IDE's standard navigation chooser.

#### Scenario: Single match navigates directly to the test method
- **WHEN** the user clicks a filled gutter icon
- **AND** exactly one test method references that scenario
- **THEN** the editor navigates directly to that test method

#### Scenario: Multiple matches open the navigation chooser
- **WHEN** the user clicks a filled gutter icon
- **AND** two or more test methods reference that scenario
- **THEN** the IDE's standard navigation chooser opens
- **THEN** each entry displays the containing class name and the test method name

### Requirement: Greyed icon is non-interactive
The plugin SHALL not attach a click handler to the greyed icon — it is a visual indicator only.

#### Scenario: Clicking greyed icon does nothing
> **Testing**: Manual / UI test only — requires a running IDE instance.
- **WHEN** the user clicks a greyed gutter icon on a scenario heading
- **THEN** no navigation occurs and no popup is shown
