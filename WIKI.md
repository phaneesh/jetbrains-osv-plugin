# OSV Vulnerability Scanner — Wiki

> Navigation hub for all plugin documentation. Last updated for **v1.1.2**.

---

## 📚 Documentation

| Document | What You'll Find |
| --- | --- |
| **[README.md](README.md)** | One-pager: quick start, features, usage guide, FAQ |
| **[GETTING_STARTED.md](GETTING_STARTED.md)** | Step-by-step walkthrough for first-time users |
| **[PLUGIN_DOCUMENTATION.md](PLUGIN_DOCUMENTATION.md)** | Complete technical reference: architecture, API, parsers, development |
| **[FAQ.md](FAQ.md)** | Frequently asked questions and troubleshooting |
| **[CHANGELOG.md](CHANGELOG.md)** | Version-by-version release history |
| **[CONTRIBUTING.md](CONTRIBUTING.md)** | Development setup, code style, PR process |

---

## 🚀 Quick Start

1. **Install** — Search "OSV Vulnerability Scanner" in **Settings → Plugins → Marketplace**
2. **Open** a project with lockfiles — the plugin auto-detects
3. **Scan** — Click **Scan Dependencies** in the tool window (bottom)

See [GETTING_STARTED.md](GETTING_STARTED.md) for the full walkthrough.

---

## 🗂️ Features

### Core (All Versions)
- Auto-detection of 21 lockfile formats across 12 ecosystems
- OSV API batch queries with TTL caching
- Severity-grouped tree view with inline navigation
- `Alt+Enter` quick fixes for version upgrades
- SARIF export for CI/CD

### Advanced (v1.1.0+)
- Risk scoring (EPSS + CISA KEV + CVSS)
- Malicious package detection
- Basic SAST (SQL injection, XSS, path traversal)
- Privacy mode (SHA-256 hashing)
- Policy enforcement
- Team config sharing
- Differential analysis
- Config audit

### BOM Generation (v1.1.1+)
- **SBOM** — CycloneDX 1.5 / SPDX 2.3
- **CBOM** — Cryptographic assets
- **QBOM** — Post-quantum cryptography
- **AIBOM** — AI/ML assets

### IDE Compatibility (v1.1.2+)
- Works on **all JetBrains IDEs**: IntelliJ IDEA, PyCharm, WebStorm, GoLand, PhpStorm, Rider, CLion, RubyMine, DataGrip
- Java-only features (SAST, reachability) auto-skip on non-Java IDEs

---

## 🖥️ Platform Support

| IDE | Status | Notes |
| --- | --- | --- |
| IntelliJ IDEA | ✅ Full | All features available |
| PyCharm | ✅ Full | All features available |
| WebStorm | ✅ Full | All features available |
| GoLand | ✅ Full | All features available |
| PhpStorm | ✅ Full | All features available |
| Rider | ✅ Full | All features available |
| CLion | ✅ Full | All features available |
| RubyMine | ✅ Full | All features available |
| DataGrip | ✅ Full | All features available |

---

## 🔧 Development

```bash
# Clone and build
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin
./gradlew buildPlugin

# Run in sandbox IDE
./gradlew runIde

# Run all tests
./gradlew test
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full developer guide.

---

## 📊 Version Matrix

| Version | Date | Highlights |
| --- | --- | --- |
| **1.1.2** | 2026-05-13 | Cross-IDE support, OSV-Scanner format parity, settings UI redesign |
| **1.1.1** | 2026-05-13 | BOM suite (SBOM/CBOM/QBOM/AIBOM), rendered charts, SARIF export |
| **1.1.0** | 2026-05-12 | Risk scoring, SAST, config audit, policy enforcement, privacy mode |
| **1.0.0** | 2024-04-24 | Initial release — Maven, Gradle, npm, PIP |

See [CHANGELOG.md](CHANGELOG.md) for full details.

---

## 🆘 Support

- [GitHub Issues](https://github.com/dyuti/jetbrains-osv-plugin/issues)
- [GitHub Discussions](https://github.com/dyuti/jetbrains-osv-plugin/discussions)

---

*Last updated: 2026-05-13*
