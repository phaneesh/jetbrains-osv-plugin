# OSV Vulnerability Scanner - Complete Plugin Documentation

A comprehensive guide to the OSV Vulnerability Scanner plugin for IntelliJ IDEA.

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Installation](#installation)
5. [Usage Guide](#usage-guide)
6. [Configuration](#configuration)
7. [API Integration](#api-integration)
8. [Parser Reference](#parser-reference)
9. [Development](#development)
10. [Contributing](#contributing)

---

## Overview

The OSV Vulnerability Scanner is a free, open-source IntelliJ IDEA plugin that provides real-time security vulnerability scanning for open-source dependencies. It integrates directly into the IDE to help developers identify and fix security issues before they reach production.

### Key Benefits

- **Real-time Scanning:** Automatic detection of vulnerabilities as you work
- **Comprehensive Coverage:** Supports Maven, Gradle, npm, and PIP dependencies
- **Actionable Insights:** Includes CVE details, severity ratings, and fix versions
- **IDE Integration:** Quick fixes, navigation, and inline highlighting
- **CI/CD Ready:** SARIF export for integration with continuous integration pipelines

---

## Architecture

### High-Level Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     IntelliJ IDEA Plugin                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐   │
│  │   Tool UI    │  │   Parsers    │  │   API Services      │   │
│  │   Layer      │  │              │  │                     │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬──────────┘   │
│         │                  │                      │             │
│         │                  │                      │             │
│         └──────────────────┴──────────────────────┘             │
│                            │                                    │
│                    ┌───────▼───────┐                            │
│                    │   Model Layer │                            │
│                    │  (Data Models)│                            │
│                    └───────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      External Services                          │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │     OSV API     │    │  GitHub Advisories │  │   Cache     │ │
│  │  (osv.dev)      │    │                     │  │  (Disk)     │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details

#### 1. Tool UI Layer (`toolwindow/`)
- **OsVToolWindowFactory:** Creates the tool window instance
- **OsVToolWindowPanel:** Main UI with tree view, filtering, and actions
- **OsVDetailsPanel:** Displays detailed vulnerability information
- **CurrentFileSummaryPanel:** Shows summary of current file
- **SummaryButton:** Quick access to scan results

#### 2. Parser Layer (`parser/`)
- **DependencyParser:** Base interface for all parsers
- **MavenParser:** Parses pom.xml files
- **GradleParser:** Parses build.gradle and build.gradle.kts files
- **NpmParser:** Parses package-lock.json files
- **PipParser:** Parses requirements.txt files

#### 3. API Services (`api/`)
- **OsVApiService:** Primary interface to OSV database
- **GitHubAdvisoryApiService:** Secondary source for security advisories
- **NvdApiService:** National Vulnerability Database integration
- **AggregatedVulnerabilityService:** Combines multiple sources

#### 4. Model Layer (`model/`)
- **Dependency:** Represents a parsed dependency
- **Vulnerability:** Represents a security vulnerability
- **OsVSeverity:** Severity levels (CRITICAL, HIGH, MEDIUM, LOW, INFO)
- **ScanResult:** Aggregated results from a scan

#### 5. Configuration Layer (`config/`)
- **OsVConfig:** Application-wide settings
- **LicensePolicyConfig:** License compliance settings

---

## Features

### Dependency File Parsing

The plugin supports parsing multiple dependency file formats:

#### Maven (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
        <version>5.3.20</version>
    </dependency>
</dependencies>
```

#### Gradle (build.gradle)
```kotlin
dependencies {
    implementation("org.springframework:spring-core:5.3.20")
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
}
```

#### npm (package-lock.json)
```json
{
  "name": "my-app",
  "version": "1.0.0",
  "dependencies": {
    "lodash": "4.17.20"
  }
}
```

#### PIP (requirements.txt)
```
requests==2.25.1
django==3.1.5
```

### Vulnerability Detection

The plugin queries multiple vulnerability databases:

1. **OSV Database** - Primary source with comprehensive vulnerability data
2. **GitHub Advisories** - Secondary source with GitHub Security Advisories
3. **NVD** - National Vulnerability Database for CVE details

### Data Flow

```
Dependency Files → Parsers → Dependency Objects → API Queries → 
Vulnerability Objects → Tree Model → UI Rendering
```

### Inline Inspection

Vulnerabilities are highlighted directly in dependency files:

- **Critical:** Red underline with warning icon
- **High:** Orange underline
- **Medium:** Yellow underline
- **Low:** Gray underline

### Quick Fixes

Context actions for each vulnerability:

- **Auto-Fix:** Update to a fixed version
- **Suppress:** Temporarily ignore the vulnerability
- **Navigate:** Jump to the dependency in the file
- **Export:** Save results to SARIF format

---

## Installation

### From JetBrains Marketplace

1. Open IntelliJ IDEA
2. Go to **Settings/Preferences** → **Plugins**
3. Search for **"OSV Vulnerability Scanner"**
4. Click **Install**
5. Restart IntelliJ IDEA

### From Source

```bash
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin
./gradlew buildPlugin
# Install from disk: Settings → Plugins → Install Plugin from Disk
```

### Prerequisites

- IntelliJ IDEA 2023.3 or later
- JDK 17 or later (for development)
- Internet connection (for API queries)

---

## Usage Guide

### Basic Workflow

1. **Open a Project**
   - The plugin automatically detects dependency files
   - No configuration required for basic usage

2. **Initiate a Scan**
   - Click **Scan Dependencies** in the tool window
   - Or use the context menu on a dependency file

3. **Review Vulnerabilities**
   - View in the tool window tree structure
   - Filter using the search box
   - Navigate to source by double-clicking

4. **Fix Issues**
   - Use auto-fix to update versions
   - Or manually update versions in your files

### Tool Window Structure

```
OSV Vulnerability Scanner
├── [Scan Dependencies] [Filter: ________]
├── Tree View
│   ├── pom.xml (3 vulnerabilities)
│   │   ├── Critical (1)
│   │   │   └── CVE-2023-1234 - Spring RCE (Fix: 5.3.21)
│   │   ├── High (1)
│   │   └── Medium (1)
│   └── build.gradle (2 vulnerabilities)
│       └── High (2)
└── [Export SARIF] [Focus Mode] [Settings]
```

### Filtering

Filter by:
- **CVE ID:** `CVE-2023-1234`
- **Package name:** `spring`, `lodash`
- **Severity:** `critical`, `high`, `medium`, `low`
- **Multiple criteria:** `spring critical`

### Focus Mode (Branch Comparison)

Compare vulnerabilities between branches:

1. Toggle **Focus Mode** in the tool window
2. Select base branch (usually `main` or `master`)
3. Only show vulnerabilities introduced in current branch
4. Useful for pull request reviews

### Exporting Results

1. Click **Export SARIF**
2. Save file with `.sarif` extension
3. Import into CI/CD pipelines or security tools

---

## Configuration

### Global Settings

**Path:** Settings/Preferences → Tools → OSV Scanner

| Setting | Description | Default |
|---------|-------------|---------|
| Minimum Severity | Only show vulnerabilities at or above this level | Medium |
| Cache TTL (minutes) | How long to cache API responses | 60 |
| OSV API Endpoint | Custom OSV API URL | https://api.osv.dev/v1/query |
| GitHub Advisory | Enable GitHub Advisory scanning | Enabled |
| NVD Integration | Enable NVD API queries | Disabled |

### License Policy

**Path:** Settings/Preferences → Tools → OSV Scanner → License Policy

| Setting | Description |
|---------|-------------|
| Allowed Licenses | Comma-separated SPDX license identifiers |
| Strict Mode | Reject all non-allowed licenses |
| Warning Licenses | Licenses that trigger warnings |

Example allowed licenses:
```
MIT, Apache-2.0, BSD-3-Clause, ISC, Unlicense
```

### Project-Specific Settings

Right-click on a project file → **OSV Scanner** → **Project Settings**

- Override global minimum severity
- Exclude specific files or directories
- Configure custom API endpoints

---

## API Integration

### OSV API

**Base URL:** https://api.osv.dev/v1/

#### Query Endpoint
```
POST /query
Content-Type: application/json

{
  "package": {
    "name": "org.springframework:spring-core",
    "ecosystem": "Maven"
  },
  "version": "5.3.20"
}
```

**Response:**
```json
{
  "id": "GHSA-x5rq-j2xg-h7cm",
  "modified": "2023-05-15T17:30:00Z",
  "published": "2023-05-15T17:30:00Z",
  "summary": "Spring Framework RCE",
  "details": "Remote code execution...",
  "severity": [
    {
      "type": "CVSS_V3",
      "score": "9.8"
    }
  ],
  "affected": [
    {
      "package": {
        "name": "org.springframework:spring-core",
        "ecosystem": "Maven"
      },
      "ranges": [
        {
          "type": "ECOSYSTEM",
          "events": [
            {"introduced": "0"},
            {"fixed": "5.3.21"}
          ]
        }
      ]
    }
  ],
  "references": [
    {"type": "ADVISORY", "url": "https://github.com/advisories/GHSA-x5rq-j2xg-h7cm"}
  ]
}
```

### GitHub Advisory API

**Base URL:** https://api.github.com/

#### List Advisories
```
GET /advisories?per_page=100
```

### NVD API

**Base URL:** https://services.nvd.nist.gov/rest/json/

#### CVE Details
```
GET /2.0/cves/cveId/CVE-2023-1234
```

### Rate Limiting

- **OSV API:** No rate limits (open API)
- **GitHub API:** 60 requests/hour (unauthenticated)
- **NVD API:** 1 request/5 seconds (enforced)

### Caching

The plugin implements a disk-based cache:

- **Cache Location:** `.idea/osv-cache/`
- **Cache TTL:** Configurable (default: 60 minutes)
- **Cache Management:** Clear cache from settings

---

## Parser Reference

### Maven Parser

**File:** `MavenParser.kt`

**Features:**
- Parses dependencies, dependency management, and plugin dependencies
- Extracts group ID, artifact ID, and version
- Handles property references (`${version.spring}`)

**Example:**
```kotlin
val pomXml = """
    <project>
        <properties>
            <version.spring>5.3.20</version.spring>
        </properties>
        <dependencies>
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-core</artifactId>
                <version>${version.spring}</version>
            </dependency>
        </dependencies>
    </project>
"""

// Parses to: org.springframework:spring-core:5.3.20
```

### Gradle Parser

**File:** `GradleParser.kt`

**Features:**
- Supports Kotlin DSL (build.gradle.kts) and Groovy DSL
- Handles both `implementation` and `compileOnly` configurations
- Extracts version from string literals and maps

**Example:**
```kotlin
dependencies {
    implementation("org.springframework:spring-core:5.3.20")
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
}
```

### npm Parser

**File:** `NpmParser.kt`

**Features:**
- Parses package-lock.json
- Extracts dependencies and devDependencies
- Handles nested dependencies

**Example:**
```json
{
  "name": "my-app",
  "version": "1.0.0",
  "lockfileVersion": 2,
  "dependencies": {
    "lodash": {
      "version": "4.17.20"
    }
  }
}
```

### PIP Parser

**File:** `PipParser.kt`

**Features:**
- Parses requirements.txt
- Handles version specifiers (`==`, `>=`, `<=`, `~=`, `!=`)
- Ignores comments and empty lines

**Example:**
```
requests==2.25.1
django>=3.1.5,<4.0.0
numpy~=1.20.0  # Compatible release
```

---

## Development

### Project Structure

```
src/
├── main/
│   ├── java/io/dyuti/osvplugin/
│   │   ├── OsVPlugin.kt                    # Plugin entry point
│   │   ├── api/                            # API services
│   │   │   ├── OsVApiService.kt
│   │   │   ├── GitHubAdvisoryApiService.kt
│   │   │   ├── NvdApiService.kt
│   │   │   └── model/                      # API data models
│   │   ├── parser/                         # Parsers
│   │   │   ├── DependencyParser.kt
│   │   │   ├── MavenParser.kt
│   │   │   ├── GradleParser.kt
│   │   │   ├── NpmParser.kt
│   │   │   └── PipParser.kt
│   │   ├── toolwindow/                     # UI components
│   │   │   ├── OsVToolWindowFactory.kt
│   │   │   ├── OsVToolWindowPanel.kt
│   │   │   └── CurrentFileSummaryPanel.kt
│   │   ├── config/                         # Configuration
│   │   │   ├── OsVConfig.java
│   │   │   └── LicensePolicyConfig.java
│   │   ├── inspection/                     # IDE inspections
│   │   │   ├── OsVInspection.kt
│   │   │   └── OsVQuickFix.kt
│   │   ├── license/                        # License scanning
│   │   │   ├── LicenseScannerService.kt
│   │   │   ├── LicenseParser.kt
│   │   │   └── model/
│   │   ├── organization/                   # Team features
│   │   │   ├── OrganizationManager.kt
│   │   │   └── TeamPermissionService.kt
│   │   ├── integration/                    # External integrations
│   │   │   ├── JiraConnector.kt
│   │   │   └── JiraIssueCreator.kt
│   │   ├── utils/                          # Utilities
│   │   │   ├── CacheManager.kt
│   │   │   ├── SeverityUtil.kt
│   │   │   └── BranchManager.kt
│   │   └── export/                         # Exporters
│   │       └── SarifExporter.kt
│   └── resources/
│       ├── META-INF/plugin.xml             # Plugin configuration
│       └── messages.properties             # Localized strings
└── test/
    └── java/io/dyuti/osvplugin/
        ├── api/                            # API tests
        ├── parser/                         # Parser tests
        ├── toolwindow/                     # UI tests
        └── config/                         # Config tests
```

### Building

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run the plugin in IDE (for development)
./gradlew runIde

# Build distribution
./gradlew buildPlugin distTar distZip

# Verify code quality
./gradlew check
```

### Debugging

1. Run `./gradlew runIde` to start a development IDE
2. Set breakpoints in your code
3. Use the plugin in the development IDE
4. Debug console will show logs

### Logging

The plugin uses IntelliJ's logging system:

```kotlin
import com.intellij.openapi.diagnostic.thisLogger

thisLogger().info("Starting scan")
thisLogger().warn("Cache miss for: $key")
thisLogger().error("Failed to parse: $file", exception)
```

---

## Contributing

### Code Style

- **Kotlin:** Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **IntelliJ:** Follow [IntelliJ Platform Code Style](https://plugins.jetbrains.com/docs/intellij/code-style.html)

### Contribution Guidelines

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
4. **Run tests**
   ```bash
   ./gradlew test
   ```
5. **Commit your changes**
   ```bash
   git commit -m 'Add amazing feature'
   ```
6. **Push to your branch**
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open a Pull Request**

### Types of Contributions

We welcome contributions in the following areas:

- **New Parsers:** Support for additional dependency formats
- **API Integrations:** Add new vulnerability sources
- **UI Improvements:** Enhancements to the tool window
- **Documentation:** Improvements to this documentation
- **Tests:** Adding test coverage
- **Bug Fixes:** Fixing issues reported in GitHub issues

### pull Request Process

1. Update README.md and CHANGELOG.md if applicable
2. Ensure all tests pass
3. Update documentation for any API changes
4. Follow the code style guide
5. Squash related commits

### Code Review

All PRs are reviewed by the maintainers. Reviewers will:

- Check for code quality
- Ensure tests are adequate
- Verify the solution aligns with project goals
- Suggest improvements

### Release Process

1. Update version in `build.gradle.kts`
2. Create release notes
3. Tag the release
4. Publish to JetBrains Marketplace

---

## API Reference

### OsVApiService

```kotlin
interface OsVApiService {
    suspend fun queryVulnerabilities(
        packageId: String,
        version: String
    ): List<Vulnerability>
    
    suspend fun queryBatch(
        queries: List<QueryRequest>
    ): List<List<Vulnerability>>
    
    fun getCachedVulnerabilities(
        packageId: String,
        version: String
    ): List<Vulnerability>?
}
```

### DependencyParser

```kotlin
interface DependencyParser {
    fun supports(file: VirtualFile): Boolean
    fun parse(file: VirtualFile): List<Dependency>
}
```

### Vulnerability

```kotlin
data class Vulnerability(
    val id: String,
    val packageId: String,
    val versions: List<String>,
    val severity: OsVSeverity,
    val cveIds: List<String>,
    val summary: String,
    val details: String,
    val fixVersions: List<String>,
    val published: Instant,
    val modified: Instant
)
```

---

## Troubleshooting

### Common Issues

#### No Vulnerabilities Detected

**Symptoms:** Scan completes but shows zero vulnerabilities

**Possible Causes:**
- No dependency files found in project
- Network connectivity issues
- OSV API is down

**Solutions:**
- Verify dependency files exist
- Check internet connection
- Try scanning again

#### Slow Scans

**Symptoms:** Scanning takes a long time

**Possible Causes:**
- Large project with many dependencies
- Network latency to OSV API
- Cache is disabled

**Solutions:**
- Increase cache TTL in settings
- Use Focus Mode to scan fewer files
- Consider upgrading internet connection

#### API Rate Limiting

**Symptoms:** Some API calls fail with rate limit errors

**Possible Causes:**
- Too many API requests in short time
- OSV API rate limiting

**Solutions:**
- Wait for rate limit to reset
- Enable caching
- Reduce concurrent scans

### Debug Mode

Enable debug logging:

1. Go to **Help** → **Debug Log Settings**
2. Add: `#io.dyuti.osvplugin`
3. Restart IntelliJ IDEA
4. Logs appear in **Help** → **Show Log in Explorer**

---

## Changelog

### Version 1.0.0

- Initial release
- Maven, Gradle, npm, and PIP support
- OSV API integration
- GitHub Advisory integration
- SARIF export
- License scanning
- Focus Mode (branch comparison)

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [OSV Database](https://osv.dev/) for vulnerability data
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) for plugin framework
- All contributors and users of the plugin

## Support

- [GitHub Issues](https://github.com/dyuti/jetbrains-osv-plugin/issues)
- [Documentation](https://github.com/dyuti/jetbrains-osv-plugin/wiki)
- [Discussions](https://github.com/dyuti/jetbrains-osv-plugin/discussions)

## Roadmap

- [ ] Additional dependency formats (Cargo, Go modules)
- [ ] Vulnerability remediation suggestions
- [ ] Integration with Jira and other issue trackers
- [ ] Team collaboration features
- [ ] Advanced filtering and sorting
- [ ] Custom rule definitions
