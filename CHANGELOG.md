# Changelog

All notable changes to the OSV IntelliJ Plugin are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.1.2] - 2026-05-13

### Fixed

- **Deprecated API Usage** — replaced `NotificationGroup` constructor with `NotificationGroupManager` API (IntelliJ 2023.3+)
- **Deprecated API Usage** — replaced `Project.getBaseDir()` with `LocalFileSystem.findFileByPath(project.basePath)` in SAST analyzer and Vulnerable API service (IntelliJ 2023.3+)
- **Gson Internal Deprecation** — `JsonElement.getAsCharacter()` warning comes from bundled Gson 2.11.0; no user-code references exist

## [1.1.1] - 2026-05-13

### Added

- **SBOM Export Panel Redesign** — dependency tree view grouped by source file, format-aware file save dialog (CycloneDX JSON, SPDX JSON, SPDX Tag-Value)
- **Professional Trends Panel** — card-based layout with rendered charts: area line chart, severity donut chart, rolling statistics table, and delta change badges
- **SARIF Export** — functional export from tool-window toolbar with dependency matching and `JFileChooser` save dialog
- **CBOM Generation** — Cryptographic Bill of Materials exporting CycloneDX 1.6 CBOM with algorithm/protocol/certificate/related-material discovery
- **QBOM Generation** — Post-Quantum Cryptography BOM detecting ML-KEM, ML-DSA, SLH-DSA, Falcon, hybrid key exchange, and quantum-vulnerable crypto
- **AIBOM Generation** — AI/ML Bill of Materials detecting LLM APIs, ML frameworks, vector databases, MLOps platforms, and AI-generated code markers
- **Multi-format BOM Tabs** — six tool-window tabs: Vulnerabilities, Trends, SBOM, CBOM, QBOM, AIBOM

### Fixed

- **AIBOM False Positives** — tightened regex detectors for OpenAI, TensorFlow, PyTorch, scikit-learn, etc. to prevent matching generic Java/Kotlin constructs `.build()`, `Completion`, `@Generated`, `.fit()`, `.predict()`
- **PluginException on Short Name Mismatch** — created inspection subclasses for XML, Groovy, Kotlin, JSON, and PlainText to satisfy IntelliJ 2024.1+ enforcement
- **Rate Limit Errors** — adopted OSV `/v1/querybatch` API replacing N individual parallel requests; then added per-vulnerability `/v1/vulns/{id}` detail fetch for full alias/affected data
- **GHSA Shown Instead of CVE** — CVE IDs now displayed preferentially across all UI paths via `/v1/vulns/{id}` detail fetch
- **Fixed Version Always N/A** — removed git commit hashes from version ranges; corrected `affected[].versions` parsing
- **AWT Threading Error in Quick Fix** — deferred dialog creation with `ApplicationManager.getApplication().invokeLater` to exit write action before UI
- **SBOM Export Threading Error** — same `invokeLater` fix for `LocalFileSystem.refreshAndFindFileByIoFile()` called from background thread
- **IntelliJ 2026.1 Compatibility** — removed `untilBuild` upper bound for forward compatibility

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
