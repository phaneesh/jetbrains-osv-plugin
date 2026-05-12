# Contributing to OSV IntelliJ Plugin

Thank you for your interest! This guide covers setup, code style, and the PR process.

## Development Setup

1. JDK 17+ (Temurin recommended)
2. IntelliJ IDEA 2023.3+ (Community or Ultimate)
3. Clone the repo:
   ```bash
   git clone https://github.com/dyuti/jetbrains-osv-plugin.git
   ```
4. Import as Gradle project
5. Run: `./gradlew runIde` (starts a sandbox IntelliJ with the plugin loaded)

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
- Minimum target: maintain or improve current test count (311+)

## Pull Request Process

1. Branch from `master`: `git checkout -b feature/short-description`
2. Include tests for all new code
3. Ensure `./gradlew test` passes locally
4. Update `CHANGELOG.md` under the latest version header
5. Open PR with a clear description of what changed and why

## Reporting Bugs

- Use [GitHub Issues](https://github.com/dyuti/jetbrains-osv-plugin/issues)
- Include: IDE version, plugin version, OS, steps to reproduce, stack trace if applicable
