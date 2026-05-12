# Testing

## Test Framework

| Technology        | Version   | Purpose                                  |
| ----------------- | --------- | ---------------------------------------- |
| JUnit 5 (Jupiter) | 5.10.0    | Primary test framework                   |
| Mockito Core      | 5.12.0    | Java mocking                             |
| Mockito Kotlin    | 5.4.0     | Kotlin-specific Mockito DSL              |
| MockK             | 1.13.9    | Native Kotlin mocking                    |
| Kotlin Test       | (bundled) | Kotlin assertions (`assertEquals`, etc.) |

- Test runner: `./gradlew test` with JUnit Platform (`useJUnitPlatform()`)
- Build plugin depends on tests: `tasks.buildPlugin { dependsOn(tasks.test) }`

## Test Structure

Tests mirror the main source package hierarchy under `src/test/java/io/dyuti/osvplugin/`.

### Test Class Inventory

| Test Class                     | File                                             | Tests | Focus                                                              |
| ------------------------------ | ------------------------------------------------ | ----- | ------------------------------------------------------------------ |
| `OsVApiServiceTest`            | `api/OsVApiServiceTest.kt`                       | 10+   | API query, batch query, CVSS severity mapping, cache clearing      |
| `VulnerabilityTest`            | `api/model/VulnerabilityTest.kt`                 | 5     | Data class construction                                            |
| `ReachabilityResultTest`       | `api/model/ReachabilityResultTest.kt`            | 2     | Reachability model                                                 |
| `MavenParserTest`              | `parser/MavenParserTest.kt`                      | 6     | pom.xml parsing: basic, multi-dep, properties, scope, line numbers |
| `GradleParserTest`             | `parser/GradleParserTest.kt`                     | 4     | build.gradle parsing                                               |
| `OsVInspectionTest`            | `inspection/OsVInspectionTest.kt`                | 4     | Inspection tool behavior                                           |
| `OsVQuickFixTest`              | `inspection/OsVQuickFixTest.kt`                  | 6+    | Quick fix creation and application                                 |
| `OsVConfigTest`                | `config/OsVConfigTest.kt`                        | 4     | Config state persistence                                           |
| `CacheManagerTest`             | `utils/CacheManagerTest.kt`                      | 7     | String cache: get, TTL expiry, overwrite, invalidation, cleanup    |
| `SeverityUtilTest`             | `utils/SeverityUtilTest.kt`                      | 10    | Severity color/icon mapping, threshold checking                    |
| `DiffAnalyzerTest`             | `diff/DiffAnalyzerTest.kt`                       | 4     | Branch diff analysis                                               |
| `HistoricalScanRepositoryTest` | `historical/HistoricalScanRepositoryTest.kt`     | 5     | Historical data persistence                                        |
| `LicenseRegistryServiceTest`   | `license/LicenseRegistryServiceTest.kt`          | 4+    | License registry lookups                                           |
| `PolicyEvaluatorTest`          | `policy/PolicyEvaluatorTest.kt`                  | 3     | Policy rule evaluation                                             |
| `PrivacyHasherTest`            | `privacy/PrivacyHasherTest.kt`                   | 5     | Hashing functions                                                  |
| `RiskScoringTest`              | `risk/RiskScoringTest.kt`                        | 4     | Risk score calculation                                             |
| `SastAnalyzerTest`             | `sast/SastAnalyzerTest.kt`                       | 5     | SQL injection, path traversal, XSS detection                       |
| `SbomGeneratorTest`            | `sbom/SbomGeneratorTest.kt`                      | 5     | CycloneDX/SPDX generation                                          |
| `MaliciousPackageServiceTest`  | `malicious/MaliciousPackageServiceTest.kt`       | 4     | Malicious package detection                                        |
| `TeamConfigManagerTest`        | `notification/TeamConfigManagerTest.kt`          | 4     | Team configuration                                                 |
| `NotificationServiceTest`      | `notificationservice/NotificationServiceTest.kt` | 5     | Notification dispatch                                              |
| `ConfigAuditServiceTest`       | `configaudit/ConfigAuditServiceTest.kt`          | 4     | Config audit scanning                                              |
| `VulnerableApiServiceTest`     | `toolwindow/VulnerableApiServiceTest.kt`         | 3     | Vulnerable API method detection                                    |
| `AutoFixServiceTest`           | `fix/AutoFixServiceTest.kt`                      | 4     | Automatic version fix application                                  |
| `OsVPluginTest`                | `OsVPluginTest.kt`                               | 2     | Plugin state component                                             |
| `TestUtil`                     | `test/TestUtil.kt`                               | —     | Shared test utilities                                              |

**Total: ~28 test classes, ~100+ tests**

## Testing Patterns

### Backtick Descriptive Test Names

```kotlin
@Test
fun `queryVulnerabilities returns empty list for unknown package`() { ... }
```

### Pure Unit Tests (No IntelliJ Platform)

Most tests instantiate classes directly with default constructors:

```kotlin
private val apiService: OsVApiService = OsVApiService()
```

This works because `OsVApiService` accepts optional `HttpClient` and gracefully falls back to non-IntelliJ config.

### String-Based Parser Tests

Parsers are tested with inline string XML/Gradle content (no file I/O):

```kotlin
val pomXml = """<project>...</project>"""
val parser = MavenParser()
val dependencies = parser.parse("pom.xml", pomXml)
assertEquals(1, dependencies.size)
```

### Live API Tests (Not Mocked)

Tests hit the real OSV API:

```kotlin
@Test
fun `queryVulnerabilities returns empty list for unknown package`() {
    val result = apiService.queryVulnerabilities("test-package", "Maven", "1.0.0")
    assertEquals(0, result.size)
}
```

**Note**: As the `TEST_COVERAGE.md` notes, "Mock HTTP responses for API tests (not included in this release)." The tests are designed to pass against the real API or return empty results for unknown packages.

### Cache TTL Testing

```kotlin
@Test
fun `getString returns null after TTL expires`() {
    cacheManager.cacheString("expiring", "value", 1L)
    Thread.sleep(10)
    assertNull(cacheManager.getString("expiring"))
}
```

### Mockito/MockK Usage

```kotlin
// Mockito Kotlin
@Test
fun `service delegates to api`() { ... }

// MockK (native Kotlin)
val mockService = mockk<OsVApiService>()
```

## Coverage Targets (from `TEST_COVERAGE.md`)

| Component    | Target | Claimed |
| ------------ | ------ | ------- |
| API Layer    | 100%   | 100%    |
| Parser Layer | 100%   | 100%    |
| Config       | 100%   | 100%    |
| Inspection   | 100%   | 100%    |
| Utilities    | 100%   | 100%    |
| Plugin Entry | 100%   | 100%    |
| **Total**    | 100%   | 100%    |

**Actual coverage tools**: JaCoCo report generated via `./gradlew jacocoTestReport` at `build/reports/jacoco/test/html/index.html`

## Testing Gaps

1. **No mocked HTTP tests** — API tests rely on live OSV API or non-existent packages
2. **No UI automation tests** — Tool window, settings panels untested
3. **No PSI integration tests** — Inspection behavior tested in isolation only
4. **No end-to-end scan tests** — Full scan flow from file to tree not tested
5. **No performance tests** — Batch query performance, cache behavior under load not measured

## Test Execution

```bash
# All tests
./gradlew test

# By package
./gradlew test --tests "io.dyuti.osvplugin.api.*"
./gradlew test --tests "io.dyuti.osvplugin.parser.*"
./gradlew test --tests "io.dyuti.osvplugin.inspection.*"
```
