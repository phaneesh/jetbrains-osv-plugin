# Contributing to OSV IntelliJ Plugin

Thank you for your interest! This guide covers setup, code style, and the PR process.

## Development Setup

1. JDK 17+ (Temurin recommended)
2. IntelliJ IDEA 2023.3+ (Community or Ultimate)
3. Clone the repo:
   ```bash
   git clone https://github.com/dyuti/jetbrains-osv-plugin.git
   cd jetbrains-osv-plugin
   ```
4. Import as Gradle project in IntelliJ

## Building from Source

### Quick Build

```bash
./gradlew buildPlugin
```

Produces `build/distributions/jetbrains-osv-plugin-*.zip` (~4.3 MB). Install via **Settings → Plugins → ⚙️ → Install from Disk**.

### Run in Sandbox IDE

```bash
./gradlew runIde
```

Launches a dedicated IntelliJ instance with the plugin pre-installed. Changes are picked up on restart.

### Compile Only

```bash
./gradlew compileKotlin compileTestKotlin
```

Check for compilation errors and deprecation warnings quickly without running tests.

### Clean Build

```bash
./gradlew clean buildPlugin
```

Recommended after dependency changes or before release.

## Testing

### Run All Tests

```bash
./gradlew test
```

Target: maintain or improve the current test count (**413+ tests, 0 failures**). All tests must pass before opening a PR.

### Run a Specific Test

```bash
./gradlew test --tests "OsVApiServiceTest"
./gradlew test --tests "io.dyuti.osvplugin.parser.MavenParserTest"
```

### Headless Test Mode

Tests run headless by default (no UI required). The build uses `test.jvmargs=-Djava.awt.headless=true`.

### Multi-Version IDE Verification

```bash
# Verify against IntelliJ 2023.3 (minimum supported)
./gradlew runIde -PplatformVersion=2023.3

# Verify against IntelliJ 2024.1
./gradlew runIde -PplatformVersion=2024.1
```

### Coverage Report

```bash
./gradlew test jacocoTestReport
```

Open `build/reports/jacoco/test/html/index.html` for the HTML report.

## Troubleshooting Build Issues

| Issue                                    | Solution                                                                  |
| ---------------------------------------- | ------------------------------------------------------------------------- |
| `PluginException: Short name not unique` | Delete `build/` and rebuild: `./gradlew clean buildPlugin`                |
| `No tests found`                         | Ensure test class names end with `Test` (not `Tests`)                     |
| `OutOfMemoryError`                       | Increase Gradle heap: `export GRADLE_OPTS=-Xmx4g`                         |
| Kotlin compilation warnings              | Fix all warnings; zero warnings is the project target                     |
| Shadow JAR too large                     | Check transitive dependencies; remove unused ones from `build.gradle.kts` |

## Code Style

- Kotlin with 4-space indentation
- Max line length: 120 characters
- Use `JBColor` for all UI colors (never raw `java.awt.Color`)
- All long-running operations go in `ProgressManager.run(Backgroundable(...))`
- All document mutations use `WriteCommandAction.runWriteCommandAction()`
- All logging uses `com.intellij.openapi.diagnostic.logger<T>()` (never `System.err.println()`)

## Testing

- All new features must include unit tests
- Use MockWebServer for HTTP-dependent tests (no live API calls in CI)
- Use `BasePlatformTestCase` / `HeavyPlatformTestCase` for PSI inspection tests
- Run full suite: `./gradlew test`
- Minimum target: maintain or improve current test count (413+)
- All parser tests use static string input (no file IO) for CI compatibility

## Pull Request Process

1. Branch from `master`: `git checkout -b feature/short-description`
2. Include tests for all new code
3. Ensure `./gradlew test` passes locally
4. Update `CHANGELOG.md` under the latest version header
5. Open PR with a clear description of what changed and why

## Reporting Bugs

- Use [GitHub Issues](https://github.com/dyuti/jetbrains-osv-plugin/issues)
- Include: IDE version, plugin version, OS, steps to reproduce, stack trace if applicable
