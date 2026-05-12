# Compatibility Notes

## IntelliJ Platform Versions

| Version | Status | Notes |
|---------|--------|-------|
| 2023.3 (233) | Tested fully | Default build target |
| 2024.1 (241) | Compiles, not yet fully tested | Warnings about `sinceBuild` < target version |
| 2024.2 (242) | Not tested | Expected compatible |
| 2024.3 (243) | Not tested | Expected compatible |

## Known Warnings

### 2024.1 Build
Compilation succeeds, but the IntelliJ Gradle Plugin emits:

```
The 'since-build' property is lower than the target IntelliJ Platform major version: 233.0 < 241.
```

This is expected — `sinceBuild=233.0` means the plugin supports 2023.3+, while the CI also tests
against 2024.1. The plugin will install on 2024.1, but this warning indicates we should
potentially raise `sinceBuild` if we begin using 2024.x-specific APIs.

## Planned Expansions

- Test 2024.2 and 2024.3 in CI after their release
- Raise `sinceBuild` to 241 when 2024.x-only APIs are adopted
- Validate no binary incompatibilities via `runIde` on each version

## Multi-Version Build Command

```bash
./gradlew buildPlugin -PintellijVersion=2024.1
```

## untilBuild

Currently set to `243.*` (2024.3). This will be expanded after multi-version verification.
