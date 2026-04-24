# OSV IntelliJ Plugin

A free, open-source IntelliJ IDEA plugin that provides security vulnerability scanning for open-source dependencies using the OSV (Open Source Vulnerabilities) database.

## Features

### Core Features
- **Dependency File Parsing:** Parse Maven (pom.xml), Gradle (build.gradle), npm (package-lock.json), and pip (requirements.txt) files
- **OSV API Integration:** Query the OSV database for vulnerabilities by package name and version
- **Vulnerability Data Model:** Store and manage vulnerability information (severity, CVE IDs, descriptions)
- **Tool Window:** Display all vulnerabilities with filtering and sorting options
- **Local Inspection:** Highlight vulnerabilities inline in dependency files

### IntelliJ Integration
- **Configuration UI:** Global and project-level configuration in IntelliJ settings
- **Quick Fixes:** Suggest upgrading to fixed versions or suppress vulnerabilities
- **Branch Comparison (Focus Mode):** Compare vulnerabilities between branches
- **SARIF Export/Import:** Export scan results in SARIF format for CI/CD integration

### Advanced Features
- **GitHub Advisory Integration:** Query GitHub Advisory Database as a secondary source
- **License Scanning:** Check licenses against allowed lists with SPDX support
- **Data Flow Analysis:** Show data flow to vulnerable sinks for critical vulnerabilities

## Installation

### From JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to Settings/Preferences → Plugins
3. Search for "OSV Vulnerability Scanner"
4. Click Install

### From Source
1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Build the plugin: `./gradlew buildPlugin`
4. Install the plugin: Settings/Preferences → Plugins → Install Plugin from Disk
5. Select the JAR file from `build/distributions/`

## Usage

### Basic Usage
1. Open a project with dependency files (pom.xml, build.gradle, etc.)
2. The plugin automatically scans for vulnerabilities
3. Vulnerabilities appear as inline highlights in dependency files
4. Open the OSV Vulnerability Scanner tool window to see all vulnerabilities

### Focus Mode (Branch Comparison)
1. Open the OSV Vulnerability Scanner tool window
2. Click the Focus Mode toggle
3. Select a base branch (usually main or master)
4. Only vulnerabilities new in the current branch are shown

### SARIF Export
1. Open the OSV Vulnerability Scanner tool window
2. Click the Export button
3. Select SARIF format
4. Save the file

## Configuration

### Global Settings
1. Go to Settings/Preferences → Tools → OSV Vulnerability Scanner
2. Configure:
   - Minimum severity threshold
   - Cache TTL
   - OSV API endpoint

### Project Settings
1. Right-click on the project root
2. Select OSV Vulnerability Scanner → Project Settings
3. Configure project-specific settings

## Development

### Prerequisites
- IntelliJ IDEA 2023.3+
- JDK 17+
- Gradle 8+

### Building
```bash
./gradlew buildPlugin
```

### Running Tests
```bash
./gradlew test
```

### Debugging
```bash
./gradlew runIde
```

## Project Structure

```
src/
├── main/
│   └── java/io/dyuti/osvplugin/
│       ├── api/               # OSV API client
│       │   ├── OsVApiService.kt
│       │   └── model/         # API models
│       ├── parser/            # Dependency parsers
│       │   ├── DependencyParser.kt
│       │   ├── MavenParser.kt
│       │   ├── GradleParser.kt
│       │   ├── NpmParser.kt
│       │   └── PipParser.kt
│       ├── inspection/        # IntelliJ inspection
│       │   ├── OsVInspection.kt
│       │   └── OsVQuickFix.kt
│       ├── toolwindow/        # Tool window UI
│       │   ├── OsVToolWindowFactory.kt
│       │   ├── OsVToolWindowPanel.kt
│       │   └── OsVDetailsPanel.kt
│       ├── config/            # Configuration
│       │   ├── OsVConfig.java
│       │   └── OsVConfigState.java
│       ├── focus/             # Focus mode
│       │   ├── OsVFocusModeService.kt
│       │   └── OsVBranchManager.kt
│       ├── github/            # GitHub integration
│       │   └── OsVGithubAdvisor.kt
│       ├── license/           # License scanning
│       │   ├── OsVLicensedDatabase.kt
│       │   └── OsVLicensedChecker.kt
│       ├── model/             # Vulnerability models
│       │   ├── Vulnerability.kt
│       │   ├── Dependency.kt
│       │   └── ScanResult.kt
│       ├── utils/             # Utility classes
│       │   ├── CacheManager.kt
│       │   └── SeverityUtil.kt
│       └── OsVPlugin.kt
└── test/
    └── java/io/dyuti/osvplugin/
        ├── api/
        ├── parser/
        ├── inspection/
        ├── toolwindow/
        ├── config/
        ├── focus/
        ├── github/
        ├── license/
        └── utils/
```

## API Reference

### OSV API
- **Endpoint:** https://api.osv.dev/v1/query
- **Method:** POST
- **Rate Limit:** None (open API)

### GitHub Advisory API
- **Endpoint:** https://api.github.com/advisories
- **Method:** GET
- **Rate Limit:** 60 requests/hour (unauthenticated)

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Acknowledgments

- Uses data from the [OSV Database](https://osv.dev/)
- Built on the [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- Inspired by [Mend Advise](https://docs.mend.io/renovate/latest/mend-advise-for-intellij-idea)

## Contributing

Contributions are welcome! Please read the [CONTRIBUTING.md](CONTRIBUTING.md) for details.
