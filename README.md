# OSV IntelliJ Plugin

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/jetbrains%20marketplace-v1.1.3-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-yellow)

> Find and fix security vulnerabilities in your dependencies — without leaving your IDE.

The **OSV Vulnerability Scanner** is a free, open-source JetBrains plugin that scans your project's lockfiles and dependency manifests against the [OSV Database](https://osv.dev/), then presents results in an interactive tree view with one-click fixes.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Supported Languages & Formats](#supported-languages--formats)
- [Features](#features)
- [Usage Guide](#usage-guide)
- [BOM Exports](#bom-exports)
- [Screenshots](#screenshots)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)

---

## Quick Start

### Install from JetBrains Marketplace (Recommended)

1. Open **IntelliJ IDEA** (or any JetBrains IDE)
2. Go to **Settings → Plugins → Marketplace**
3. Search **"OSV Vulnerability Scanner"**
4. Click **Install** — restart if prompted

### Install from Disk

```bash
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin
./gradlew buildPlugin
# Install via Settings → Plugins → ⚙️ → Install from Disk → build/distributions/*.zip
```

### Run Your First Scan

1. Open any project that has dependency files
2. Look for the **"OSV Vulnerability Scanner"** tool window at the bottom
3. Click **Scan Dependencies** — or press the toolbar button
4. Vulnerabilities appear grouped by severity; double-click to jump to source

---

## Supported Languages & Formats

| Language / Ecosystem  | Supported Lockfiles & Manifests                                                                           |
| --------------------- | --------------------------------------------------------------------------------------------------------- |
| **Java / Kotlin**     | `pom.xml`, `build.gradle`, `build.gradle.kts`, `gradle.lockfile`, `verification-metadata.xml`             |
| **JavaScript / Node** | `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `bun.lock`                                            |
| **Python**            | `requirements.txt`, `pyproject.toml`, `poetry.lock`, `Pipfile.lock`, `pdm.lock`, `uv.lock`, `pylock.toml` |
| **Go**                | `go.mod`                                                                                                  |
| **Rust**              | `Cargo.lock`                                                                                              |
| **PHP**               | `composer.lock`                                                                                           |
| **Ruby**              | `Gemfile.lock`, `gems.locked`                                                                             |
| **Dart / Flutter**    | `pubspec.lock`                                                                                            |
| **.NET**              | `packages.lock.json`, `packages.config`, `*.deps.json`                                                    |
| **Haskell**           | `stack.yaml.lock`, `cabal.project.freeze`                                                                 |
| **Elixir**            | `mix.lock`                                                                                                |
| **R**                 | `renv.lock`                                                                                               |
| **C / C++**           | `conan.lock`                                                                                              |

> Detection is automatic — open a project and the plugin finds the files for you.

---

## Features

### Core

| Feature                 | What It Does                                                                             |
| ----------------------- | ---------------------------------------------------------------------------------------- |
| **Auto-Detection**      | Scans all supported lockfiles across the project tree automatically                      |
| **OSV API Integration** | Real-time batch queries with TTL caching, automatic retries, and offline fallback        |
| **Severity Grouping**   | Vulnerabilities sorted by Critical / High / Medium / Low with color-coded icons          |
| **Inline Inspections**  | Red/orange/yellow underlines in `pom.xml`, `build.gradle`, etc. — see issues as you type |
| **Quick Fixes**         | `Alt+Enter` on any vulnerable dependency to update to the fixed version                  |
| **Navigate to Source**  | Double-click a vulnerability to jump directly to the offending line                      |
| **SARIF Export**        | One-click export for GitHub Advanced Security, Azure DevOps, or any SARIF consumer       |

### Advanced (v1.1+)

| Feature                         | What It Does                                                                    |
| ------------------------------- | ------------------------------------------------------------------------------- |
| **Risk Scoring**                | Combines EPSS, CISA KEV, and CVSS into a single exploitability score            |
| **Malicious Package Detection** | Flags typosquatting, homoglyphs, and known-malware packages                     |
| **Basic SAST**                  | Pattern-based SQL injection, XSS, and path-traversal detection in project code  |
| **Privacy Mode**                | SHA-256 hashes package names in UI, logs, and exports before sharing            |
| **Policy Enforcement**          | Org-wide rules: block licenses, set severity thresholds, enforce KEV compliance |
| **Team Config**                 | Project-level policy via `.idea/osv-plugin-config.json` — version controlled    |
| **Differential Analysis**       | Compare two scans to see NEW, RESOLVED, and CHANGED vulnerabilities             |
| **Config Audit**                | Scans `application.properties` / `.yml` for 20+ insecure patterns               |

### BOM Generation (v1.1.1+)

Four exportable bill-of-materials types from the tool window:

| BOM       | Purpose                                                                                     | Format                                                |
| --------- | ------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| **SBOM**  | Software Bill of Materials — all dependencies with PURLs and hashes                         | CycloneDX 1.5 JSON, SPDX 2.3 JSON, SPDX 2.3 Tag-Value |
| **CBOM**  | Cryptographic assets — algorithms, protocols, certificates, key material detected in source | CycloneDX 1.6 aligned JSON                            |
| **QBOM**  | Post-quantum cryptography inventory — ML-KEM, ML-DSA, Falcon, X25519Kyber768                | Custom JSON                                           |
| **AIBOM** | AI/ML asset inventory — LLM APIs, TensorFlow, PyTorch, vector DBs, MLOps                    | Custom JSON                                           |

---

## Usage Guide

### The Tool Window

After opening a project, the tool window appears at the bottom with **6 tabs**:

| Tab                 | Content                                                                        |
| ------------------- | ------------------------------------------------------------------------------ |
| **Vulnerabilities** | Tree view of all findings, grouped by lockfile → severity → issue              |
| **Trends**          | Rolling scan history with rendered area charts, donut charts, and delta badges |
| **SBOM**            | Interactive dependency tree with CycloneDX / SPDX export controls              |
| **CBOM**            | Cryptographic asset tree with automated source-code scanning                   |
| **QBOM**            | Post-quantum crypto asset tree with hybrid-key-exchange detection              |
| **AIBOM**           | AI/ML asset tree detecting LLM providers, model frameworks, and vector DBs     |

### Navigating Results

1. **Expand a lockfile** (e.g., `pom.xml`)
2. **Expand a severity group** (Critical → High → Medium → Low)
3. **Select a vulnerability** to see:
   - CVE or GHSA ID with clickable link
   - Affected and fixed version range
   - EPSS / KEV / CVSS scores
   - Affected function signatures (when available)
4. **Double-click** to open the source file at the vulnerable line

### Quick Fixes (`Alt+Enter`)

Place the cursor on a highlighted dependency and press **Alt+Enter**:

- **Update to fixed version** — rewrites the version in `pom.xml`, `build.gradle`, etc.
- **Suppress / Mark False Positive** — excludes from future scans
- **View Advisory** — opens OSV.dev page in browser

### Filtering

Type in the filter box to search by:

- `CVE-2023-1234` — specific CVE
- `spring` — package name
- `critical` — severity
- `kev` — CISA KEV entries only

### Exporting

- **SARIF** — click **Export** in the toolbar, select **SARIF**, save → upload to GitHub Security / Azure DevOps
- **BOM** — open the SBOM/CBOM/QBOM/AIBOM tab → click **Export** → choose format and save

### Configuring the Plugin

Open **Settings → Tools → OSV Scanner**:

| Setting              | What It Controls                               | Default                        |
| -------------------- | ---------------------------------------------- | ------------------------------ |
| **Minimum Severity** | Filter out low-severity noise                  | Medium                         |
| **Cache TTL**        | How long API responses are cached locally      | 60 min                         |
| **OSV API Endpoint** | Point to a self-hosted OSV instance            | `https://api.osv.dev/v1/query` |
| **Privacy Mode**     | Hash all package names in UI and exports       | Off                            |
| **GitHub Advisory**  | Enrich results with GitHub Security Advisories | On                             |
| **License Policy**   | Block or warn on specific SPDX licenses        | —                              |

---

## BOM Exports

### When to Use Each

- **SBOM** — Sharing with customers, compliance (EO 14028), vulnerability disclosure
- **CBOM** — OT / government / regulated environments where crypto inventory is required (e.g., NSA CNSA 2.0)
- **QBOM** — Preparing for post-quantum migration; identifies algorithms vulnerable to quantum attacks (RSA < 3072, ECC) and quantum-safe alternatives (ML-KEM, ML-DSA)
- **AIBOM** — AI governance, model provenance, LLM supply-chain audit

### Export Workflow

1. Open the relevant BOM tab in the tool window
2. Click **Scan Project** — the plugin scans source files for the relevant assets
3. Review the tree (grouped by type / subtype)
4. Click **Export** → choose format → save to disk
5. A notification with **Open Folder** appears on completion

---

## Screenshots

| Feature                                        | Screenshot |
| ---------------------------------------------- | ---------- |
| Tool Window — Vulnerability Tree               | _TBD_      |
| Inline Inspection — Red underline in `pom.xml` | _TBD_      |
| Quick Fix — Alt+Enter popup                    | _TBD_      |
| Trends Panel — Area + donut charts             | _TBD_      |
| SBOM Panel — Dependency tree with export       | _TBD_      |
| Settings — Plugin configuration                | _TBD_      |

> 📸 Screenshots will be captured before marketplace submission and embedded here.

---

## FAQ

**Q: Does it work in PyCharm, WebStorm, GoLand, etc.?**

> Yes. The plugin installs on any JetBrains IDE. Java-specific features (SAST, reachability) are skipped on non-Java IDEs automatically.

**Q: Do I need an API key?**

> No. The OSV API is free and open. GitHub Advisory enrichment uses unauthenticated requests (60/hour).

**Q: Does it modify my files automatically?**

> No. Quick fixes only update versions when you explicitly trigger them (`Alt+Enter`).

**Q: Can I use it offline?**

> Partially. Cached results are available; new queries require internet.

**Q: How often should I scan?**

> The plugin auto-rescans when lockfiles change. For CI/CD, export SARIF on every build.

**Q: What's in the plugin bundle?**

> The distribution is self-contained (~4 MB). No additional downloads or JVMs required.

---

## Contributing

Bug reports, feature requests, and pull requests are welcome.

- **[Contributing Guide](CONTRIBUTING.md)** — setup, code style, PR process
- **[Plugin Documentation](PLUGIN_DOCUMENTATION.md)** — architecture, API reference, parser details
- **[Getting Started](GETTING_STARTED.md)** — step-by-step walkthrough for new users

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Built with ❤️ by Dyuti for the open-source security community.
</p>
