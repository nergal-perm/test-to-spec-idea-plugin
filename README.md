# Test to Spec

An IntelliJ IDEA plugin that links `@Scenario` annotations in unit tests back to their specification files, making it possible to read scenario content without leaving the test code.

## Why

In specification-driven development, unit tests are written to verify concrete scenarios defined in spec files. Over time, navigating between a test and its corresponding scenario in a spec file becomes tedious. This plugin bridges that gap by adding a gutter icon to every test method annotated with `@Scenario`, giving instant access to the scenario's content and a one-click path to the spec file.

## Features

- **Gutter icon** on any function or class annotated with `@Scenario`
- **Click** the icon — opens a popup with the scenario content rendered as markdown, styled to match the current IntelliJ theme
- **Ctrl/Cmd+Click** the icon — navigates directly to the scenario heading in the spec file
- **"Open in spec.md" link** inside the popup for mouse-driven navigation
- Supports **multiple `@Scenario` annotations** on a single test — all scenarios shown in one popup
- Fully **configurable** per project: spec root path, annotation FQN, and attribute names

## Usage Example

Given a capability spec at `specs/main-isic-code-mapping/spec.md`:

```markdown
## Main ISIC Code Mapping

...

#### Scenario: No locations in deal

When a deal has no locations attached, the system should fall back to
the entity's registered country for ISIC code resolution.

**Given** a deal with no locations
**When** the ISIC code mapping is requested
**Then** the entity's registered country is used as the fallback
```

And a Kotlin test annotated like this:

```kotlin
@Scenario(capability = "main-isic-code-mapping", value = "No locations in deal")
@Test
fun `ISIC code falls back to registered country when deal has no locations`() {
    // ...
}
```

The plugin places a gutter icon next to the function name. Clicking it shows:

```
┌─────────────────────────────────────────────────────────┐
│ Scenario: No locations in deal                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ When a deal has no locations attached, the system       │
│ should fall back to the entity's registered country     │
│ for ISIC code resolution.                               │
│                                                         │
│ Given a deal with no locations                          │
│ When the ISIC code mapping is requested                 │
│ Then the entity's registered country is used            │
│                                                         │
│ Open in spec.md →                                       │
└─────────────────────────────────────────────────────────┘
```

## Configuration

Open **Settings → Tools → Test to Spec** and configure the following per-project settings:

| Setting | Description | Example |
|---|---|---|
| Spec files root | Path to the specs folder, relative to the project root | `specs` |
| @Scenario annotation FQN | Fully qualified class name of the annotation | `com.example.testing.Scenario` |
| Capability attribute name | The annotation parameter that holds the capability subfolder name | `capability` |
| Scenario name attribute | The annotation parameter that holds the scenario name | `value` |

Settings are stored in `.idea/testToSpec.xml` and can be committed to version control to share the configuration across the team.

## Defining the @Scenario Annotation

The plugin reads your own `@Scenario` annotation — you define it in your project and point the plugin to its fully qualified name in settings.

### Kotlin

```kotlin
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Scenario(val capability: String, val value: String)
```

`@Repeatable` (Kotlin 1.6+) is required to allow multiple `@Scenario` annotations on the same function. No container class is needed. `SOURCE` retention is sufficient because the plugin reads PSI at edit time, not at runtime.

### Java

Java requires `@Repeatable` to allow multiple `@Scenario` annotations on the same method:

```java
import java.lang.annotation.*;

@Repeatable(Scenario.Container.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Scenario {
    String capability();
    String value();

    @interface Container {
        Scenario[] value();
    }
}
```

Without `@Repeatable`, the IDE will reject a second `@Scenario` on the same method as a compile error.

## Releasing

Releases are published automatically via GitHub Actions when a version tag is pushed:

```bash
git tag v1.2.0
git push origin v1.2.0
```

This triggers the release workflow, which builds the plugin zip and publishes it as a GitHub release with the zip attached and auto-generated release notes.

## Building

Prerequisites: JDK 21+

```bash
# Clone the repository
git clone <repo-url>
cd test-to-spec

# Build the plugin zip
./gradlew buildPlugin
```

The distributable zip is produced at:

```
build/distributions/test-to-spec-<version>.zip
```

To launch a sandboxed IntelliJ instance with the plugin loaded for development:

```bash
./gradlew runIde
```

## Installation

1. Build the plugin zip as described above (or download a release zip)
2. Open IntelliJ IDEA
3. Go to **Settings → Plugins**
4. Click the **gear icon** (⚙) → **Install Plugin from Disk...**
5. Select the `test-to-spec-1.0.0.zip` file
6. Restart IntelliJ when prompted
7. Open your project and configure the plugin under **Settings → Tools → Test to Spec**

## Spec File Conventions

The plugin expects spec files to follow this structure:

```
{specRoot}/
  {capability}/
    spec.md
```

Scenario headings inside `spec.md` must follow the pattern:

```
#### Scenario: <name>
```

The heading level can be `#` through `######`. The scenario's content extends from its heading to the next heading of equal or lesser depth, or to the end of the file.
