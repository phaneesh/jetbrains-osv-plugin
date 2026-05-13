# OSV IntelliJ Plugin

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/jetbrains%20marketplace-v1.1.2-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-yellow)

> A free, open-source IntelliJ IDEA plugin that provides security vulnerability scanning for open-source dependencies using the [OSV Database](https://osv.dev/).

## Features

### Core

- **Dependency Parsing** — 20+ lockfile formats across 12 ecosystems:
  - **Java** — `pom.xml`, `build.gradle`, `build.gradle.kts`, `gradle.lockfile`, `verification-metadata.xml`
  - **JavaScript/Node** — `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `bun.lock`
  - **Python** — `requirements.txt`, `pyproject.toml`, `poetry.lock`, `Pipfile.lock`, `pdm.lock`, `uv.lock`, `pylock.toml`
  - **Go** — `go.mod`
  - **Rust** — `Cargo.lock`
  - **PHP** — `composer.lock`
  - **Ruby** — `Gemfile.lock`, `gems.locked`
  - **Dart/Flutter** — `pubspec.lock`
  - **.NET** — `packages.lock.json`, `packages.config`, `*.deps.json`
  - **Haskell** — `stack.yaml.lock`, `cabal.project.freeze`
  - **Elixir** — `mix.lock`
  - **R** — `renv.lock`
  - **C/C++** — `conan.lock`
- **OSV API Integration** — Real-time batch queries with TTL caching, rate limiting, and offline resilience
- **Tool Window** — Tree-based vulnerability explorer with filtering, severity grouping, and hyperlinks
- **Inline Inspections** — Highlight vulnerable dependencies directly in dependency files
- **Quick Fixes** — Alt+Enter to upgrade to fixed versions or suppress false positives
- **SARIF Export** — Export scan results for CI/CD ingestion (GitHub Advanced Security, Azure DevOps)

### Advanced (v1.1.1)

- **Multi-Format BOM Generation** — Export four bill-of-materials types from the tool window:
  - **SBOM** — CycloneDX 1.5 and SPDX 2.3 (JSON and Tag-Value)
  - **CBOM** — Cryptographic assets: algorithms, protocols, certificates, key material
  - **QBOM** — Post-quantum cryptography: ML-KEM, ML-DSA, SLH-DSA, Falcon, hybrid key exchange
  - **AIBOM** — AI/ML assets: LLM APIs, TensorFlow, PyTorch, vector DBs, MLOps, AI-generated code markers
- **Professional Trends Charts** — Rendered `Graphics2D` area + donut charts, rolling statistics table, delta change badges (replaced ASCII sparklines)
- **SARIF Export** — Full CI/CD-ready export for GitHub Advanced Security, Azure DevOps

### Advanced (v1.1.0)

- **Vulnerable API Detection** — Cross-references vulnerable function signatures with call sites
- **Malicious Package Detection** — Typosquatting, homoglyph, and known-malware checks
- **Basic SAST / Taint Analysis** — Pattern-based SQL injection, XSS, and path-traversal detection
- **Privacy-Preserving Queries** — SHA-256 hash package names in UI, logs, and exports
- **Risk Scoring** — Composite EPSS + CISA KEV + CVSS scoring for exploitability-based prioritization
- **Policy Enforcement** — Organization-wide compliance rules (severity, CVSS, KEV, license)
- **Team Collaboration** — Project-level config sharing via `.idea/osv-plugin-config.json`
- **Differential Analysis** — Compare two scans to identify NEW, RESOLVED, and CHANGED vulnerabilities
- **Configuration Audit** — Scan `application.properties` / `application.yml` for 20 insecure patterns
- **IDE Notifications** — Severity-based balloon notifications for discovered vulnerabilities
- **Status Bar Widget** — Persistent scan status and vulnerability count in the IDE status bar
- **Dark Mode Support** — All UI colors use `JBColor` for Darcula / light theme adaptation

## Quick Start

1. **Install** — Search "OSV Vulnerability Scanner" in **Settings → Plugins → Marketplace**
2. **Open** a project with dependency files — the plugin auto-detects
3. **Scan** — Click "Scan Dependencies" in the tool window (bottom)

> Vulnerabilities appear grouped by severity. Double-click to navigate to the source line. Alt+Enter for quick fixes.

## Screenshots

> 📸 Screenshots will be captured during Wave 5 (Marketplace Packaging):
>
> - Tool window tree view
> - Inline vulnerability highlight
> - Alt+Enter quick-fix popup

## Feature Comparison

| Feature                        | OSV Plugin    | Snyk       | Mend.io    | Qodana |
| ------------------------------ | ------------- | ---------- | ---------- | ------ |
| Free SCA                       | ✅            | Free tier  | Paid       | Paid   |
| Problems tab integration       | ✅            | ✅         | ✅         | ✅     |
| Auto-fix via quick fix         | ✅            | ✅         | ✅         | ❌     |
| License scanning               | ✅            | ✅         | ✅         | ❌     |
| Reachability analysis          | ✅ Basic      | ✅ Premium | ✅ Premium | ✅     |
| Malicious packages             | ✅ **Unique** | ❌         | ✅ Premium | ❌     |
| Basic SAST                     | ✅ **Unique** | ✅         | ✅         | ✅     |
| Privacy exports                | ✅ **Unique** | ❌         | ❌         | ❌     |
| Risk scoring (EPSS+KEV)        | ✅ **Unique** | ✅ Premium | ✅ Premium | ❌     |
| Policy enforcement             | ✅ **Unique** | ✅ Premium | ✅ Premium | ❌     |
| Team config sharing            | ✅ **Unique** | ✅ Premium | ✅ Premium | ❌     |
| Differential analysis          | ✅ **Unique** | ✅         | ✅         | ❌     |
| Historical trends              | ✅ **Unique** | ✅         | ✅ Premium | ❌     |
| Config audit (properties/yml)  | ✅ **Unique** | ❌         | ❌         | ❌     |
| **SBOM / CBOM / QBOM / AIBOM** | ✅ **Unique** | ✅ SBOM    | ✅ SBOM    | ❌     |
| Tree-based UI                  | ✅            | ✅         | ✅         | ✅     |

## Installation

### From JetBrains Marketplace (Recommended)

1. Open IntelliJ IDEA
2. Go to **Settings → Plugins → Marketplace**
3. Search **"OSV Vulnerability Scanner"**
4. Click **Install** and restart

### From Source

```bash
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin
./gradlew buildPlugin
# Install from: build/distributions/*.zip
```

## Requirements

- IntelliJ IDEA 2023.3 or later
- JDK 17+ (for development)
- Internet connection (for OSV API queries)

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
</content>
