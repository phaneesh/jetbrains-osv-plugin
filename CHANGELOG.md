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
- Raised default cache TTL from 1 hour to 24 hours for reduced API load
- Raised default rate limit from 100 to 1000 requests/hour via OSV batch API

### Fixed

- **AWT Threading Error** — quick fix dialogs cannot run inside write actions; deferred with `invokeLater`
- **GHSA shown instead of CVE** — CVE IDs now displayed preferentially in all UI paths (tree, inspection, notifications, exports)
- **Fixed version always N/A** — git commit hashes filtered from version ranges; `last_affected` and `affected[].versions` parsed correctly
- **Rate limit errors** — replaced N individual parallel HTTP requests with single OSV `/v1/querybatch` call
- **Short name mismatch** — inspection subclasses created for each language to match `plugin.xml` registration (IntelliJ 2024.1+)
- **IDEA 2026.1 Compatibility** — removed `untilBuild` upper bound; plugin compatible with all versions ≥ 2023.3

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
