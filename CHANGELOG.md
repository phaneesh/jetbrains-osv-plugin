# Changelog

All notable changes to the OSV IntelliJ Plugin are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.1.0] - 2026-05-12

### Added

- **Vulnerable API Detection** — cross-references vulnerable function signatures with call sites in source code
- **Malicious Package Detection** — typosquatting, homoglyph, and known-malware detection
- **Basic SAST / Taint Analysis** — pattern-based SQL injection, XSS, and path-traversal detection
- **Privacy-Preserving Queries** — SHA-256 hashing of package names in UI, logs, and exports
- **Risk Scoring** — composite scoring using EPSS + CISA KEV + CVSS for exploitability-based prioritization
- **Policy Enforcement** — organization-wide dependency compliance policies with severity / CVSS / KEV / license rules
- **Team Collaboration** — project-level config sharing via `.idea/osv-plugin-config.json`
- **Differential Analysis** — compare two scans to detect NEW, RESOLVED, and severity-changed vulnerabilities
- **Historical Trending** — track vulnerability counts over time with ASCII sparkline display
- **SBOM Generation** — export CycloneDX 1.5 and SPDX 2.3 in JSON and Tag-Value formats
- **Configuration Audit** — scan `application.properties` / `application.yml` for 20 insecure patterns
- **IDE Notification Service** — severity-based balloon notifications for discovered vulnerabilities
- **Status Bar Widget** — persistent scan status and vulnerability count in the IDE status bar
- **Dark Mode Support** — all UI colors use JBColor for automatic Darcula / light theme adaptation
- **MockWebServer Testing** — all API tests use mocked HTTP instead of live OSV API calls
- **Toolbar Actions** — Scan, Clear, Export buttons in the tool window title bar
- **Animated Scan Indicator** — spinner during scan via `AnimatedIcon`

### Changed

- Replaced OkHttp with `java.net.http.HttpClient` for better platform compatibility
- Converted inspections to async with per-file caching and debounce to eliminate UI freezing
- Replaced string-based auto-fix with PSI-based refactoring supporting transitive dependencies

### Fixed

- **CacheManager singleton** — `getInstance()` now returns a true singleton via `ApplicationManager.getService()`
- **OsVApiService singleton** — same fix, restoring correct rate limiting behavior
- **Hardcoded OSV API URL** — now configurable in Settings → OSV Scanner
- **Exception swallowing** — all `System.err.println` replaced with `com.intellij.openapi.diagnostic.Logger`
- **Vulnerability `packageName`** — now populated from query context instead of empty string
- **CacheManager thread safety** — `@Synchronized` annotations added to prevent data corruption under concurrent access

## [1.0.0] - 2024-04-24

### Added

- Core dependency parsing for Maven (`pom.xml`), Gradle (`build.gradle`), npm (`package-lock.json`), pip (`requirements.txt`)
- OSV API integration with batch queries and TTL caching
- Tool window with vulnerability tree, filtering, and sorting
- Multi-language local inspections (XML, Groovy, Kotlin, JSON, plain text)
- Alt+Enter quick fixes for upgrading vulnerable dependencies
- SARIF export for CI/CD integration
- Settings configuration panel (severity threshold, cache TTL, scan scope)
- License scanning with SPDX identifier support
