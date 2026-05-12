# External Integrations

## OSV (Open Source Vulnerabilities) API

| Aspect | Detail |
|--------|--------|
| Endpoint | `https://api.osv.dev/v1/query` |
| Method | POST |
| Auth | None required (open API) |
| Rate Limiting | Client-side configurable via `OsVConfig.rateLimitEnabled` (default: 100 req/hour) |
| Files | `OsVApiService.kt`, `AggregatedVulnerabilityService.kt` |

### API Client Capabilities
- Single vulnerability query by package name, ecosystem, and version
- Batch query with parallel async execution (max 10 concurrent requests via `Semaphore`)
- Response caching with TTL via `CacheManager`
- CVSS severity parsing (V3 preferred, V2 fallback)
- Affected function signature extraction

## GitHub Advisory Database

| Aspect | Detail |
|--------|--------|
| Endpoint | `https://api.github.com/advisories` |
| Method | GET |
| Auth | Optional GitHub token (`OsVConfig.githubToken`) |
| Rate Limit | 60 req/hour unauthenticated |
| File | `GitHubAdvisoryApiService.kt` |

### Status
- Feature flag: `githubAdvisoryEnabled` (default: `false`)
- Secondary data source to augment OSV results

## NVD (National Vulnerability Database)

| Aspect | Detail |
|--------|--------|
| File | `NvdApiService.kt` |
| Purpose | Supplemental CVE scoring and metadata |

## Jira Integration

| Aspect | Detail |
|--------|--------|
| Files | `JiraConnector.kt`, `JiraIssueCreator.kt` |
| Auth | Basic (email + API token, Base64 encoded) |
| Features | Create vulnerability tickets with severity mapping |
| Configuration | Via `OsVConfig`: `jiraBaseUrl`, `jiraProjectKey`, `jiraEmail`, `jiraToken` |
| Status | Feature flag: `jiraEnabled` (default: `false`) |

### Jira Issue Fields
- Summary, description, labels, priority, assignee
- Vulnerability ID, severity, CVE references linked in description

## Local File System (IntelliJ VFS)

| Aspect | Detail |
|--------|--------|
| API | `com.intellij.openapi.vfs.LocalFileSystem` |
| Usage | Scanning for dependency manifest files (`pom.xml`, `build.gradle`, `package.json`, `requirements.txt`) |
| Recursive | Scans subdirectories for module files |

## SARIF Export

| Aspect | Detail |
|--------|--------|
| Format | SARIF v2.1.0 |
| File | `SarifExporter.kt` |
| Features | Rules, results, severity mapping, tool metadata |
| Output | JSON file written to configurable path (`OsVConfig.sarifExportPath`) |

## SBOM Generation (No External API)

| Format | Standard | File |
|--------|----------|------|
| CycloneDX JSON | v1.5 | `SbomGenerator.kt` |
| SPDX JSON | v2.3 | `SbomGenerator.kt` |
| SPDX Tag-Value | v2.3 | `SbomGenerator.kt` |

- Pure generation — no external service calls
- PURL generation for package identification

## Data Sources Used (No API Call)

| Source | Purpose | File |
|--------|---------|------|
| SPDX License List | License classification | `LicenseRegistryService.kt` |
| OSV malicious package list | Known malicious packages | `MaliciousPackageService.kt` |

## Network Client Configuration

```kotlin
// From OsVApiService.kt
HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build()
```

- Uses Java 11+ `java.net.http.HttpClient`
- Async requests via `sendAsync()` with `CompletableFuture`
- Parallel batch execution limited by `Semaphore(maxConcurrent = 10)`
