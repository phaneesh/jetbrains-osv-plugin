# Architecture

## Architectural Pattern

The plugin follows the **IntelliJ Platform Plugin Architecture** with a layered service-oriented design:

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer                                  │
│  Tool Windows  │  Settings Configurables  │  Notifications      │
│  Inspections   │  Quick Fixes             │  Pop-up Menus       │
├─────────────────────────────────────────────────────────────────┤
│                      Service Layer                               │
│  API Services  │  Parsers  │  Cache  │  Config  │  Exporters   │
├─────────────────────────────────────────────────────────────────┤
│                      Domain Layer                                │
│  Models (Vulnerability, Dependency, Severity)                    │
├─────────────────────────────────────────────────────────────────┤
│                  IntelliJ Platform SDK                           │
│  PSI (Program Structure Interface)  │  VFS  │  Editor API       │
└─────────────────────────────────────────────────────────────────┘
```

## Entry Points

### 1. Plugin Entry Point

- **`OsVPlugin.kt`** — `PersistentStateComponent<OsVConfig>` for plugin-level state management

### 2. Inspection Entry Point

- **`OsVInspection.kt`** — `LocalInspectionTool` registered for multiple languages:
  - JAVA, XML, Groovy, Kotlin, JSON, PlainText
  - Asynchronous non-blocking scanning with 500ms debounce
  - Per-file vulnerability cache with modification stamp invalidation

### 3. Tool Window Entry Point

- **`OsVToolWindowFactory.kt`** — `ToolWindowFactory` creating 3-tab panel:
  1. Vulnerabilities — real-time scan results tree
  2. Trends — historical vulnerability tracking
  3. SBOM — CycloneDX/SPDX export

### 4. Settings Entry Points

- **`OsVConfigurable.kt`** — Main settings under Tools → OSV Scanner
- **`LicensePolicyConfigurable.kt`** — License policy sub-page
- **`PrivacyConfigurable.kt`** — Privacy settings sub-page

## Data Flow

### Inspection Flow (Real-time)

```
PSI File Change → OsVInspection.buildVisitor()
    → Check file cache (modification stamp)
    → Show "Scanning..." placeholder
    → Debounce 500ms
    → Backgroundable Task:
        → Parse dependencies (Maven/Gradle/Npm/Pip)
        → Query OSV API (batch + cache)
        → Filter by severity threshold
        → Register problems in ProblemsHolder
        → Trigger daemon re-highlight
```

### Tool Window Scan Flow

```
Click "Scan Dependencies" → Backgroundable Task:
    → Recursively find manifest files (VFS)
    → Parse each file → collect dependencies
    → Batch query OSV API for vulnerabilities
    → Build tree model (by module → severity → vulnerability)
    → Update UI tree + notify Trend/SBOM panels
```

### Quick Fix Flow

```
User clicks quick fix → OsVQuickFix.applyFix()
    → Determine fix type (Upgrade/Suppress/Ignore)
    → For Upgrade: Document-level text replacement
        → WriteCommandAction (undo support)
        → FileDocumentManager.saveDocument()
    → For Suppress: Insert comment at file start
    → For Ignore: Update OsVConfig.ignoredPackages
```

## Core Abstractions

### Dependency Parsing

- **`DependencyParser`** — Abstract base with `canHandle()`, `parse()`, `detectEcosystem()`
- Concrete parsers:
  - `MavenParser` — Regex-based `pom.xml` parsing (property resolution, scope extraction, line calculation)
  - `GradleParser` — Regex-based `build.gradle` / `build.gradle.kts` parsing
  - `NpmParser` — `package-lock.json` / `package.json` parsing
  - `PipParser` — `requirements.txt` / `pyproject.toml` parsing

### API Service Layer

- **`OsVApiService`** — OSV API client (single + batch queries, caching, rate limiting)
- **`GitHubAdvisoryApiService`** — GitHub Advisory Database client
- **`NvdApiService`** — NVD/CVE scoring client
- **`AggregatedVulnerabilityService`** — Aggregator across multiple sources

### Configuration System

- **`OsVConfig`** — `PersistentStateComponent` with XML persistence (`osv-config.xml`)
- 20+ configurable fields: severity threshold, caching, rate limits, integrations, privacy mode

### Cache System

- **`CacheManager`** — `PersistentStateComponent` with in-memory + disk cache
- Vulnerability cache with TTL (hours)
- String cache with TTL (milliseconds)
- Manual invalidation support

## Plugin Extension Points (from plugin.xml)

| Extension Point                  | Implementation                                 | Purpose                           |
| -------------------------------- | ---------------------------------------------- | --------------------------------- |
| `localInspection` (×6 languages) | `OsVInspection`                                | Inline vulnerability highlighting |
| `localInspection`                | `LicenseInspection`                            | License compliance checking       |
| `applicationConfigurable`        | `OsVConfigurable`                              | Settings UI                       |
| `applicationConfigurable`        | `LicensePolicyConfigurable`                    | License policy UI                 |
| `applicationConfigurable`        | `PrivacyConfigurable`                          | Privacy settings UI               |
| `toolWindow`                     | `OsVToolWindowFactory`                         | Bottom tool window                |
| `notificationGroup`              | —                                              | Alert notifications               |
| `applicationService`             | `OsVConfig`, `LicensePolicyConfig`             | App-level services                |
| `projectService`                 | `OrganizationManager`, `TeamPermissionService` | Project-level services            |
| `projectService`                 | `JiraConnector`, `JiraIssueCreator`            | Jira integration                  |

## Key Design Patterns

1. **Service Locator** — `ApplicationManager.getService()` / `Project.getService()`
2. **Visitor Pattern** — `PsiElementVisitor` for file inspection
3. **Strategy Pattern** — Multiple `DependencyParser` implementations
4. **Observer Pattern** — Scan completion callbacks (`setOnScanCompleted`)
5. **Command Pattern** — `WriteCommandAction` for document mutations
6. **Singleton (via Platform)** — `CacheManager.getInstance()`, `OsVApiService.getInstance()`
