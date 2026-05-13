# Getting Started with OSV Vulnerability Scanner

A step-by-step guide to help you install, configure, and use the OSV Vulnerability Scanner plugin for IntelliJ IDEA.

## Table of Contents
- [Installation](#installation)
- [First Scans](#first-scans)
- [Understanding the Tool Window](#understanding-the-tool-window)
- [Navigating Vulnerabilities](#navigating-vulnerabilities)
- [Auto-Fix Vulnerabilities](#auto-fix-vulnerabilities)
- [Configuration](#configuration)
- [Tips & Best Practices](#tips--best-practices)

## Installation

### From JetBrains Marketplace (Recommended)

1. Open IntelliJ IDEA
2. Go to **Settings/Preferences** → **Plugins**
3. Search for **"OSV Vulnerability Scanner"**
4. Click **Install**
5. Restart IntelliJ IDEA if prompted

### From Source (For Developers)

```bash
# Clone the repository
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin

# Build the plugin
./gradlew buildPlugin

# Install from disk
# Settings/Preferences → Plugins → Install Plugin from Disk
# Select the JAR from build/distributions/
```

## What's New in v1.1.1

The latest release focuses on **BOM (Bill of Materials) generation** and **IDE stability**:

1. **Multi-Format BOM Generation** — discover and export four BOM types:
   - **SBOM** — CycloneDX 1.5 / SPDX 2.3 with dependency tree view
   - **CBOM** — cryptographic assets (algorithms, protocols, certificates, key material)
   - **QBOM** — post-quantum cryptography inventory (ML-KEM, ML-DSA, SLH-DSA, Falcon)
   - **AIBOM** — AI/ML assets (LLM APIs, TensorFlow, PyTorch, vector DBs, MLOps)
2. **Professional Trends Panel** — no more ASCII sparklines; rich `Graphics2D` charts with area fill, donut severity distribution, rolling statistics table, and delta change badges.
3. **Full SARIF Export** — export scan results for CI/CD ingestion (GitHub Advanced Security, Azure DevOps).

Plus: automated AIBOM false-positive fixes (`.build()` and `Completion` no longer trigger incorrectly), and expanded IDE compatibility.

## What's New in v1.1.0

1. **Risk Scoring** — composite EPSS + CISA KEV + CVSS for exploitability-based prioritization.
2. **Policy Enforcement** — organization-wide rules for severity, CVSS, KEV, and license compliance.
3. **Differential Analysis** — compare two scans to detect NEW, RESOLVED, and severity-changed vulnerabilities.

Other highlights: Malicious Package Detection, Basic SAST, Privacy-Preserving Queries, Historical Trending, Configuration Audit, IDE Notifications, and Dark Mode support.

## First Scans

### Opening the Tool Window

1. Open a project with dependency files (pom.xml, build.gradle, etc.)
2. The OSV Vulnerability Scanner tool window appears at the bottom of the IDE
3. If not visible, open it via: **View** → **Tool Windows** → **OSV Vulnerability Scanner**

### Running Your First Scan

1. Click the **"Scan Dependencies"** button (or press the scan button in the tool window toolbar)
2. Wait for the scan to complete (status shows "Scanning dependencies...")
3. Vulnerabilities will appear in the tree view

### What Gets Scanned

The plugin automatically detects and scans:
- **Maven:** `pom.xml` files
- **Gradle:** `build.gradle` and `build.gradle.kts` files
- **npm:** `package-lock.json` files
- **PIP:** `requirements.txt` files

## Understanding the Tool Window

The tool window provides **six tabs** for different views of your project security:

- **Vulnerabilities** — real-time scan results grouped by module and severity  
- **Trends** — historical vulnerability tracking with rendered charts and statistics
- **SBOM** — CycloneDX / SPDX export for your current dependencies
- **CBOM** — cryptographic asset inventory (algorithms, protocols, certificates)
- **QBOM** — post-quantum cryptography asset inventory
- **AIBOM** — AI/ML asset inventory (LLM APIs, frameworks, vector DBs)

### Tree Structure

The **Vulnerabilities** tab tree follows this hierarchy:

```
OSV Vulnerability Scanner (root)
├── pom.xml (module file)
│   ├── Critical (severity group)
│   │   ├── CVE-2023-1234 - Spring Framework RCE (Fix: 5.3.21)
│   │   └── CVE-2023-5678 - SQL Injection (Fix: 4.1.10)
│   ├── High
│   │   └── CVE-2023-9999 - XSS Vulnerability (Fix: 3.2.5)
│   └── ...
└── build.gradle (another module)
    └── ...
```

### Node Types

| Node | Description |
|------|-------------|
| **Module File** | A dependency file (pom.xml, build.gradle, etc.) with vulnerability count |
| **Severity Group** | Vulnerabilities grouped by severity (Critical, High, Medium, Low) |
| **Vulnerability** | Individual CVE with CVE ID, summary, and fix version |

### Status Bar Messages

- **"Scanning dependencies..."** - Initial scan in progress
- **"Scanning pom.xml..."** - Currently scanning a specific file
- **"Scan complete: X vulnerabilities found"** - Scan finished with results
- **"No dependency files found..."** - No dependency files detected

## Navigating Vulnerabilities

### Opening the Source Code

- **Double-click** any vulnerability node
- Or **right-click** → **Navigate to Line**
- IntelliJ opens the dependency file and highlights the vulnerable dependency

### Viewing Details

- Hover over a vulnerability for tooltip details
- Or select and check the details panel (if enabled)

### Exporting Results

1. Click the **Export** button in the tool window
2. Choose format: **SARIF** (for CI/CD integration)
3. Save the file

### Filtering Vulnerabilities

Type in the **Filter** field to search by:
- CVE ID (e.g., `CVE-2023-1234`)
- Package name (e.g., `spring`)
- Severity (e.g., `critical`)

## Auto-Fix Vulnerabilities

### Quick Fix via Context Menu

1. Right-click a vulnerability node
2. Select **"Auto-Fix Dependency Version"**
3. The plugin updates the version in your dependency file

### What Gets Fixed

- **Maven:** Updates `<version>` tags in pom.xml
- **Gradle:** Updates version in dependency declarations
- **npm:** Updates version in package-lock.json
- **PIP:** Updates version in requirements.txt

### Version Reference

- If a fix version is available, it's used
- If multiple fix versions exist, the lowest compatible version is selected
- If no fix exists, shows "N/A" in the vulnerability details

## Configuration

### Accessing Settings

1. Go to **Settings/Preferences** → **Tools** → **OSV Scanner**
2. Or open **OSV Vulnerability Scanner** tool window → click gear icon

### Available Settings

| Setting | Description | Default |
|---------|-------------|---------|
| **Minimum Severity** | Only show vulnerabilities at or above this level | Medium |
| **Cache TTL** | How long to cache API responses (minutes) | 60 |
| **OSV API Endpoint** | Custom OSV API URL | https://api.osv.dev/v1/query |
| **GitHub Advisory** | Enable GitHub Advisory scanning | Enabled |
| **Privacy Mode** | Hash package names in exports and UI | Disabled |

### License Policy Configuration

1. Go to **Settings/Preferences** → **Tools** → **OSV Scanner** → **License Policy**
2. Configure:
   - **Allowed Licenses:** Comma-separated list of SPDX license identifiers
   - **Strict Mode:** Reject all licenses not explicitly allowed
   - **Warning Licenses:** Licenses that trigger warnings but don't block

### Privacy Mode

Enable **Privacy Mode** to replace human-readable package names with SHA-256 hashes in UI, logs, and all exports:

1. Go to **Settings/Preferences** → **Tools** → **OSV Scanner**
2. Check **Privacy Mode**
3. Restart the IDE for full effect

This is useful when sharing screenshots, SARIF exports, or SBOMs in public or customer-facing contexts.

## Tips & Best Practices

### 1. Set Up Automatic Scans

- The plugin automatically rescans when dependency files change
- For CI/CD, use the SARIF export feature

### 2. Prioritize Critical Issues

- Use the **Severity** tree groups to focus on Critical and High vulnerabilities first
- Consider setting minimum severity to "High" for production projects

### 3. Use Filters for Large Projects

- With many vulnerabilities, use the filter field to find specific CVEs or packages
- Filter is case-insensitive

### 4. Manage False Positives

- Right-click vulnerabilities to suppress or mark as false positive
- Use inline inspection quick fixes for quick resolution

### 5. Keep OSV Database Updated

- The plugin caches results for performance
- Clear cache if you suspect stale data: **Settings** → **OSV Scanner** → **Clear Cache**

### 6. Branch Comparison (Focus Mode)

- Toggle **Focus Mode** to see only vulnerabilities introduced in your current branch
- Useful for pull request reviews

## Troubleshooting

### No Vulnerabilities Detected

- Check that dependency files exist (pom.xml, build.gradle, etc.)
- Verify network connectivity (OSV API requires internet access)
- Check the status bar for error messages

### Scan Takes Too Long

- Increase cache TTL to reduce API calls
- Scan smaller projects or specific directories first
- Consider increasing minimum severity threshold

### API Errors

- Check OSV API status: https://status.osv.dev/
- Try again later if rate limiting is in effect
- Verify custom API endpoint if configured

## Next Steps

- Explore the [OSV Documentation](https://osv.dev/docs/)
- Learn about [SARIF format](https://sarifweb.azurewebsites.net/) for CI/CD integration
- Check out [SPDX License List](https://spdx.org/licenses/) for license management
- Contribute to the plugin on [GitHub](https://github.com/dyuti/jetbrains-osv-plugin)
