# Coding Conventions

## Language & Style

- **Primary language**: Kotlin (all source files use `.kt` extension)
- **Java interop**: Minimal; uses Java APIs from IntelliJ Platform SDK and `java.net.http`
- **Indentation**: 4 spaces (inferred from source)
- **Line length**: ~120 characters typical

## Naming Conventions

| Construct                     | Convention                                | Example                                                             |
| ----------------------------- | ----------------------------------------- | ------------------------------------------------------------------- |
| Classes                       | PascalCase, `OsV` prefix for plugin types | `OsVApiService`, `VulnerabilityTreeNode`                            |
| Interfaces / Abstract classes | PascalCase                                | `DependencyParser`                                                  |
| Functions                     | camelCase                                 | `queryVulnerabilities()`, `buildVisitor()`                          |
| Variables / Properties        | camelCase                                 | `minimumSeverity`, `rateLimitEnabled`                               |
| Constants (companion)         | UPPER_SNAKE_CASE                          | (not prominently used)                                              |
| Packages                      | lowercase, reverse domain                 | `io.dyuti.osvplugin.api`                                            |
| Test classes                  | `*Test` suffix                            | `OsVApiServiceTest`                                                 |
| Test functions                | backtick descriptive names                | `` `queryVulnerabilities returns empty list for unknown package` `` |

## Code Patterns

### Companion Object Singletons

```kotlin
// From CacheManager.kt
companion object {
    fun getInstance(): CacheManager = CacheManager()
}
```

Most services use companion objects. **Note**: Some return new instances rather than true singletons (potential bug in `CacheManager`).

### Lazy-Initialized Config

```kotlin
// From OsVApiService.kt
private val config by lazy {
    try {
        ApplicationManager.getApplication().getService(OsVConfig::class.java)
    } catch (e: Exception) {
        OsVConfig()  // Fallback defaults
    }
}
```

Graceful degradation when running outside IntelliJ (e.g., unit tests).

### Data Classes for Models

```kotlin
// From api/model/Vulnerability.kt
data class Vulnerability(
    val id: String,
    val cveIds: List<String>,
    val severity: OsVSeverity,
    val cvssScore: Double?,
    // ...
)
```

All domain models are Kotlin `data class` with immutable vals and default parameters.

### WriteCommandAction for Document Mutations

```kotlin
// From OsVQuickFix.kt
WriteCommandAction.runWriteCommandAction(
    project,
    "Upgrade ${dependency.name} to $newVersion",
    null,
    Runnable {
        document.replaceString(start, end, updatedBlock)
        FileDocumentManager.getInstance().saveDocument(document)
    }
)
```

All file mutations use `WriteCommandAction` for undo support.

### Nullable Safety

```kotlin
val id = vuln.getAsJsonPrimitive("id")?.asString ?: ""
val cvssScore = scoreStr?.toDoubleOrNull()
```

Extensive use of `?.` Elvis operator for JSON parsing and API response handling.

## Error Handling Patterns

| Pattern                     | Usage                                                 |
| --------------------------- | ----------------------------------------------------- |
| **Try-catch with fallback** | API config retrieval, JSON parsing                    |
| **Custom exception type**   | `OsVApiException` for API errors                      |
| **Silent failure**          | Inspection parser errors logged to `System.err`       |
| **User-visible messages**   | `Messages.showWarningDialog()` for quick fix failures |

### Example Error Handling

```kotlin
// From OsVInspection.kt
try {
    val dependencies = parser.parse(fileName, file.text)
    // ...
} catch (e: Exception) {
    System.err.println("OSV: Error scanning file $fileName: ${e.message}")
}
```

## Threading Model

| Context                  | Pattern                                                   | File                                        |
| ------------------------ | --------------------------------------------------------- | ------------------------------------------- |
| **Background tasks**     | `ProgressManager.run(Backgroundable(...))`                | `OsVInspection.kt`, `OsVToolWindowPanel.kt` |
| **UI updates**           | `ApplicationManager.getApplication().invokeLater { ... }` | `OsVInspection.kt`                          |
| **Async I/O**            | `httpClient.sendAsync().handle(...)`                      | `OsVApiService.kt`                          |
| **Concurrency limiting** | `Semaphore(maxConcurrent = 10)`                           | `OsVApiService.kt`                          |
| **Synchronized blocks**  | `synchronized(results) { ... }`                           | `OsVApiService.kt` (parallel batch)         |
| **Debounce**             | `Timer.schedule(timerTask, 500)`                          | `OsVInspection.kt`                          |

## Documentation Style

- KDoc for all public classes and methods
- Multi-line comments for complex logic
- ASCII section dividers in large files:

```kotlin
// ───────────────────────────────────────────────────────────────────────────
// UPGRADE — document-level version replacement with undo support
// ───────────────────────────────────────────────────────────────────────────
```

- Inline `TODO` comments for known gaps

## Feature Flags

Many features are gated via boolean flags in `OsVConfig`:

- `inspectionEnabled` — Inline highlighting
- `githubAdvisoryEnabled` — GitHub Advisory integration
- `licenseScanningEnabled` — License scanning
- `focusModeEnabled` — Branch comparison
- `privacyPreservingEnabled` — Privacy mode
- `orgManagementEnabled` — Organization/team support
- `jiraEnabled` — Jira issue creation
- `rateLimitEnabled` — API rate limiting
