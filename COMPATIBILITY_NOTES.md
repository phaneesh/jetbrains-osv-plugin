# Compatibility Notes

Last updated for **v1.1.2**.

---

## IntelliJ Platform Versions

| Version | Status | Notes |
| --- | --- | --- |
| 2023.3 (233) | ✅ Tested fully | Default build target — lowest supported version |
| 2024.1 (241) | ✅ Compiles | CI-tested; `sinceBuild` covers this implicitly |
| 2024.2 (242) | ✅ Compatible | No upper bound |
| 2024.3 (243) | ✅ Compatible | No upper bound |
| 2025.1 (251) | ✅ Compatible | No upper bound |
| 2025.2 (252) | ✅ Compatible | No upper bound |
| 2025.3 (253) | ✅ Compatible | No upper bound |
| 2026.1 (261) | ✅ Compatible | Latest stable — verified on IU-261.23567.138 |

### Compatibility Strategy

The plugin uses `sinceBuild="233.0"` with **no `untilBuild`** (empty string). This means the plugin is compatible with **all IntelliJ Platform versions 2023.3 and newer**, including all future releases.

All APIs used are stable since 2023.3:
- `com.intellij.openapi` services
- `PsiFile` / `LocalInspectionTool` / `ProblemsHolder`
- `ToolWindowFactory` / `AnAction`
- `PersistentStateComponent`
- `NotificationGroupManager`

If a future IntelliJ version introduces binary incompatibility, we will set a new `untilBuild` and publish an update.

---

## Cross-IDE Support (v1.1.2+)

As of v1.1.2, the plugin installs and runs on **all JetBrains IDEs**:

| IDE | Status | Java Features |
| --- | --- | --- |
| IntelliJ IDEA | ✅ Full | All features available |
| PyCharm | ✅ Full | SAST + reachability skip; Python inspection active |
| WebStorm | ✅ Full | SAST + reachability skip; JS/TS inspection active |
| GoLand | ✅ Full | SAST + reachability skip; Go inspection active |
| PhpStorm | ✅ Full | SAST + reachability skip |
| Rider | ✅ Full | SAST + reachability skip |
| CLion | ✅ Full | SAST + reachability skip |
| RubyMine | ✅ Full | SAST + reachability skip |
| DataGrip | ✅ Full | SAST + reachability skip |

### How It Works

- `com.intellij.modules.java` is marked as **optional** in `plugin.xml`
- Java-only registrations (inspections, module settings) are extracted to `plugin-java.xml`
- `JavaPsiCompatibility` runtime guard detects Java PSI via `Class.forName()` — no `ClassNotFoundError` on non-Java IDEs
- When Java PSI is unavailable, SAST and reachability analysis gracefully skip

---

## Build Verification

```bash
# Default build (2023.3 compatibility)
./gradlew buildPlugin

# Test against specific IntelliJ version
./gradlew compileKotlin -PintellijVersion=2024.1
./gradlew compileKotlin -PintellijVersion=2026.1
```

## CI Matrix

GitHub Actions tests against:
- `2023.3` (minimum supported)
- `2024.1` (forward-compatibility gate)

Both compile and produce identical plugin ZIPs.
