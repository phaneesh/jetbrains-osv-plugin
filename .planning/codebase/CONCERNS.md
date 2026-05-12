# Concerns & Technical Debt

## High-Priority Issues

### 1. CacheManager Singleton Bug

**File**: `src/main/java/io/dyuti/osvplugin/utils/CacheManager.kt`

```kotlin
companion object {
    fun getInstance(): CacheManager = CacheManager()  // BUG: New instance each call!
}
```

`getInstance()` returns a **new instance** rather than caching the singleton. This breaks all caching across the plugin — each component gets its own empty cache. The cache is also declared as a non-static field, so each `CacheManager` instance is independent.

This also affects `OsVApiService.getInstance()` which calls `CacheManager.getInstance()` internally.

### 2. Hardcoded API URL

**File**: `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`

```kotlin
private val osvApiUrl = "https://api.osv.dev/v1/query"
```

The OSV API URL is hardcoded. `OsVConfig` has no field for custom API endpoint despite `sarifExportPath` being configurable.

### 3. Real API Calls in Unit Tests

**File**: `src/test/java/io/dyuti/osvplugin/api/OsVApiServiceTest.kt`
Tests call the live OSV API with no mocking mechanism. This makes tests:

- Flaky (network-dependent)
- Slow (actual HTTP round-trips)
- Unreliable in CI (rate limits, API downtime)

### 4. Rate Limiting is Per-Instance, Not Global

**File**: `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`

```kotlin
private var requestsThisHour = 0
private var rateLimitWindowStart = System.currentTimeMillis()
```

Rate limit counters are instance fields. Multiple `OsVApiService` instances (or fresh instances per request) would reset the counter, defeating rate limiting.

## Medium-Priority Issues

### 5. Regex-Based Parser Fragility

**Files**: `MavenParser.kt`, `GradleParser.kt`
Dependency parsers use regex against raw text instead of structured parsing:

- **Maven**: `RegexOption.DOT_MATCHES_ALL` with broad patterns; fragile for nested XML, comments, CDATA
- **Gradle**: Only matches `implementation("group:artifact:version")` syntax — misses `platform()`, `enforcedPlatform()`, version catalogs, `libs.versions.toml`
- **Npm/Pip**: Not examined deeply but likely similar regex approaches

### 6. Missing `vulnerability.packageName` Population

**File**: `src/main/java/io/dyuti/osvplugin/api/model/Vulnerability.kt`

```kotlin
val packageName: String = "", // Package this vulnerability affects (derived from query context)
```

The `packageName` field has a default empty value and is not populated during parsing. Code comments suggest it should be derived from query context but the actual assignment is missing in `OsVApiService.parseVulnerability()`.

### 7. Incomplete Code Paths

Several areas have placeholder or stub implementations:

- `OsVPlugin.kt` — Minimal state component, no actual plugin lifecycle logic
- `SastAnalyzer.kt` — Explicitly documented as "lightweight" with no data-flow or inter-procedural analysis
- `PrivacyService.kt` — Privacy-preserving hash mode exists but scope unclear
- Many feature flags default to `false` (GitHub Advisory, Jira, license scanning, org management)

### 8. Exception Swallowing

**File**: `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`

```kotlin
try {
    vuln.getAsJsonArray("affected")
} catch (_: Exception) {
    null
} ?: return emptyList()
```

Broad `catch (_: Exception)` suppresses parse errors silently. Failed API responses may return empty lists without logging.

### 9. Synchronized Blocks in Async Pipeline

**File**: `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`

```kotlin
synchronized(results) { results[dep] = vulns }
```

The parallel batch query uses `synchronized` blocks inside `CompletableFuture.handle()`. While thread-safe, this can become a bottleneck with many concurrent requests.

### 10. Timer Debounce Without Cancellation Safety

**File**: `src/main/java/io/dyuti/osvplugin/inspection/OsVInspection.kt`

```kotlin
debounceTimers[filePath]?.cancel()
debounceTimers.remove(filePath)
```

`Timer.cancel()` does not interrupt running tasks, only prevents future scheduled executions. A `ScheduledExecutorService` with proper task management would be more robust.

## Lower-Priority Concerns

### 11. Plugin Manifest Documentation vs. Reality

The `plugin.xml` change notes and `README.md` list features like "Data Flow Analysis", "Organization Management", "Jira Integration", "SARIF Export" — many are implemented as stubs or behind disabled feature flags.

### 12. Build Plugin Shadow JAR Configuration

The shadow JAR replaces the regular JAR in the plugin distribution, but the minimize configuration:

```kotlin
minimize {
    exclude(dependency("org.jetbrains.kotlin:.*"))
    exclude(dependency("org.jetbrains:.*"))
}
```

This may not correctly preserve all required classes for the platform, leading to `NoClassDefFoundError` at runtime.

### 13. Test Resource File Missing

`src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` exists with inline mock maker config, but no tests appear to use inline mocking of final classes, suggesting the config may be unnecessary.

### 14. Version Compatibility Claim

**File**: `build.gradle.kts`

```kotlin
sinceBuild.set("233.0")  // 2023.3
untilBuild.set("262.*")  // 2026.1
```

The change notes claim "Updated compatibility to support IntelliJ IDEA 2026.1.x" but the plugin targets platform 2023.3 with a very wide `untilBuild`. This may break with platform API changes in 2024.x–2026.x.

## Security Concerns

### 15. Jira Token Storage

**File**: `src/main/java/io/dyuti/osvplugin/config/OsVConfig.kt`

```kotlin
var jiraToken: String? = null
var githubToken: String? = null
```

Sensitive tokens are stored in plain XML via `PersistentStateComponent`. No encryption at rest.

### 16. Privacy Salt Storage

```kotlin
var privacySalt: String? = null
```

Salt for privacy-preserving hashing also stored in plain XML.

### 17. System.err for Logging

Multiple files use `System.err.println()` for errors instead of proper SLF4J/IntelliJ logger:

```kotlin
System.err.println("OSV: Error scanning file $fileName: ${e.message}")
```

In production, this pollutes IDE logs without proper log levels or rotation.

## Performance Concerns

### 18. Synchronous File Tree Traversal

**File**: `src/main/java/io/dyuti/osvplugin/toolwindow/OsVToolWindowPanel.kt`

```kotlin
for (child in directory.children) {
    if (child.isDirectory) collectModuleFiles(child.path, moduleFiles)
}
```

Manifest file discovery uses recursive VFS traversal on the UI thread background task. Large projects may experience slow scan startup.

### 19. No Pagination for Batch Queries

Batch queries send all uncached dependencies simultaneously (up to semaphore limit). Very large dependency lists could strain the OSV API and the local thread pool.

## Areas Requiring Attention

| Priority  | Issue                                                | File(s)                             |
| --------- | ---------------------------------------------------- | ----------------------------------- |
| 🔴 High   | Fix `CacheManager.getInstance()` to return singleton | `CacheManager.kt`                   |
| 🔴 High   | Mock HTTP client for unit tests                      | `OsVApiServiceTest.kt`              |
| 🔴 High   | Hardcoded API URL                                    | `OsVApiService.kt`, `OsVConfig.kt`  |
| 🟡 Medium | Replace regex parsers with structured parsing        | `MavenParser.kt`, `GradleParser.kt` |
| 🟡 Medium | Encrypt sensitive config fields                      | `OsVConfig.kt`                      |
| 🟡 Medium | Replace `System.err` with proper logging             | Multiple                            |
| 🟢 Low    | Verify `untilBuild` compatibility range              | `build.gradle.kts`                  |
| 🟢 Low    | Add proper plugin lifecycle in `OsVPlugin.kt`        | `OsVPlugin.kt`                      |
