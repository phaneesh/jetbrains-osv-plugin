# Compatibility Notes

## IntelliJ Platform Versions

| Version      | Status          | Notes                                             |
| ------------ | --------------- | ------------------------------------------------- |
| 2023.3 (233) | ✅ Tested fully | Default build target — lowest supported version   |
| 2024.1 (241) | ✅ Compiles     | CI-tested; `sinceBuild` covers this implicitly    |
| 2024.2 (242) | ✅ Compatible   | No upper bound                                    |
| 2024.3 (243) | ✅ Compatible   | No upper bound                                    |
| 2025.1 (251) | ✅ Compatible   | No upper bound                                    |
| 2025.2 (252) | ✅ Compatible   | No upper bound                                    |
| 2025.3 (253) | ✅ Compatible   | No upper bound                                    |
| 2026.1 (261) | ✅ Compatible   | Latest stable (build IU-261.23567.138) — verified |

## Compatibility Strategy

The plugin uses `sinceBuild="233.0"` with **no `untilBuild`** (empty string). This means the plugin is compatible with **all IntelliJ IDEA versions 2023.3 and newer**, including all future releases.

### Why no upper bound?

The OSV plugin code does **not** use version-specific IntelliJ APIs that break between major releases. All APIs used are:

- Standard `com.intellij.openapi` services
- `PsiFile` / `LocalInspectionTool` / `ProblemsHolder`
- `ToolWindowFactory` / `AnAction`
- `PersistentStateComponent`
- `NotificationGroup`

These APIs have been stable since 2023.3. If a future IntelliJ version introduces a binary incompatibility, we will address it by:

1. Setting a new `untilBuild` on the affected release
2. Publishing an updated plugin version

## Build Verification

```bash
# Default build (2023.3 compatibility)
./gradlew buildPlugin

# Test against specific IntelliJ version
./gradlew compileKotlin -PintellijVersion=2024.1
./gradlew compileKotlin -PintellijVersion=2026.1
```

## CI Matrix

The GitHub Actions workflow tests against:

- `2023.3` (minimum supported)
- `2024.1` (forward-compatibility gate)

Both compile and produce identical plugin ZIPs since Maven artifacts are cached.
