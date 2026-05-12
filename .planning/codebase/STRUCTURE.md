# Directory Structure

## Top-Level Layout

```
.
├── build.gradle.kts              # Build configuration with shadow JAR
├── gradlew / gradlew.bat         # Gradle wrapper scripts
├── settings.gradle.kts           # Gradle settings
├── README.md                     # User-facing documentation
├── PLUGIN_DOCUMENTATION.md       # Detailed plugin docs
├── GETTING_STARTED.md            # Developer onboarding
├── COMPETITIVE_RESEARCH.md       # Market analysis
├── ENTERPRISE_GAP_ANALYSIS.md    # Enterprise feature gaps
├── ENTERPRISE_TASK_LIST.md       # Enterprise roadmap
├── META-INF/plugin.xml           # Alternative plugin manifest location
├── src/
│   ├── main/
│   │   ├── java/io/dyuti/osvplugin/      # Kotlin source (despite java/ path)
│   │   └── resources/
│   │       ├── META-INF/plugin.xml       # Primary plugin manifest
│   │       ├── OsVBundle.properties      # I18n strings
│   │       └── icons/                    # Plugin icons (16/24/32/48/64 px)
│   └── test/
│       └── java/io/dyuti/osvplugin/      # Test source mirrors main
├── build/                        # Gradle build outputs (generated)
├── .github/                      # GitHub workflows
├── docs/                         # Additional documentation
├── icons/                        # Source icon assets
└── .planning/                    # GSD planning artifacts (this dir)
```

## Source Package Structure (`src/main/java/io/dyuti/osvplugin/`)

| Package             | Files                                                                                                                                     | Purpose                             |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |
| **Root**            | `OsVPlugin.kt`                                                                                                                            | Plugin entry point, state component |
| **`api/`**          | `OsVApiService.kt`, `GitHubAdvisoryApiService.kt`, `NvdApiService.kt`, `AggregatedVulnerabilityService.kt`                                | External API clients                |
| **`api/model/`**    | `Vulnerability.kt`, `AffectedFunction.kt`                                                                                                 | API response data models            |
| **`parser/`**       | `DependencyParser.kt`, `MavenParser.kt`, `GradleParser.kt`, `NpmParser.kt`, `PipParser.kt`                                                | Dependency manifest parsers         |
| **`inspection/`**   | `OsVInspection.kt`, `OsVQuickFix.kt`                                                                                                      | PSI inspection + quick fixes        |
| **`toolwindow/`**   | `OsVToolWindowFactory.kt`, `OsVToolWindowPanel.kt`, `CurrentFileSummaryPanel.kt`, `SbomExportPanel.kt`, `VulnerableApiService.kt`, etc.   | Tool window UI                      |
| **`config/`**       | `OsVConfig.kt`                                                                                                                            | Persistent plugin configuration     |
| **`settings/`**     | `OsVConfigurable.kt`                                                                                                                      | Settings UI configurable            |
| **`license/`**      | `LicenseScannerService.kt`, `LicenseInspection.kt`, `LicensePolicyConfigurable.kt`, `LicenseRegistryService.kt`, `LicenseParser.kt`, etc. | License scanning & policy           |
| **`github/`**       | `GitHubAdvisoryApiService.kt`                                                                                                             | GitHub Advisory integration         |
| **`integration/`**  | `JiraConnector.kt`, `JiraIssueCreator.kt`                                                                                                 | Jira ticket creation                |
| **`export/`**       | `SarifExporter.kt`                                                                                                                        | SARIF report generation             |
| **`sbom/`**         | `SbomGenerator.kt`, `SbomExporter.kt`, `SbomModels.kt`                                                                                    | SBOM generation (CycloneDX/SPDX)    |
| **`sast/`**         | `SastAnalyzer.kt`, `SqlInjectionDetector.kt`, `PathTraversalDetector.kt`, `XssDetector.kt`                                                | Lightweight static analysis         |
| **`diff/`**         | `DiffAnalyzer.kt`, `DiffModels.kt`                                                                                                        | Branch comparison for focus mode    |
| **`fix/`**          | `AutoFixService.kt`                                                                                                                       | Automatic version upgrade service   |
| **`focus/`**        | `OsVFocusModeService.kt`, `OsVBranchManager.kt`                                                                                           | Branch comparison focus mode        |
| **`historical/`**   | `HistoricalScanRepository.kt`, `HistoricalTrendPanel.kt`, `HistoricalJsonSerializer.kt`, `ScanCompletionListener.kt`                      | Historical scan tracking            |
| **`notification/`** | `NotificationService.kt`, `TeamConfigManager.kt`, `NotificationModels.kt`                                                                 | Team notifications                  |
| **`organization/`** | `OrganizationManager.kt`, `TeamPermissionService.kt`                                                                                      | Multi-org/multi-team support        |
| **`policy/`**       | `PolicyConfig.kt`, `PolicyEvaluator.kt`                                                                                                   | Security policy evaluation          |
| **`privacy/`**      | `PrivacyService.kt`, `PrivacyHasher.kt`, `PrivacyConfigurable.kt`                                                                         | Privacy-preserving mode             |
| **`risk/`**         | `RiskScoringService.kt`, `RiskModels.kt`                                                                                                  | Risk scoring algorithms             |
| **`malicious/`**    | `MaliciousPackageService.kt`                                                                                                              | Known malicious package detection   |
| **`configaudit/`**  | `ConfigAuditService.kt`, `ConfigAuditModels.kt`                                                                                           | Configuration file auditing         |
| **`utils/`**        | `CacheManager.kt`, `SeverityUtil.kt`, `BranchManager.kt`, `HelpLinkProvider.kt`                                                           | Utilities                           |

## Test Package Structure (`src/test/java/io/dyuti/osvplugin/`)

Tests mirror the main source structure:

- `api/OsVApiServiceTest.kt`, `api/model/VulnerabilityTest.kt`, `api/model/ReachabilityResultTest.kt`
- `parser/MavenParserTest.kt`, `parser/GradleParserTest.kt`
- `inspection/OsVInspectionTest.kt`, `inspection/OsVQuickFixTest.kt`
- `config/OsVConfigTest.kt`
- `utils/CacheManagerTest.kt`, `utils/SeverityUtilTest.kt`
- `license/LicenseRegistryServiceTest.kt`
- And more — see `src/test/` for full list

### Test Resources

- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` — Mockito inline mock maker config

## Naming Conventions

| Pattern          | Example                                       | Usage                       |
| ---------------- | --------------------------------------------- | --------------------------- |
| `OsV` prefix     | `OsVPlugin`, `OsVApiService`, `OsVInspection` | All plugin-specific types   |
| `Service` suffix | `CacheManager`, `AutoFixService`              | Singleton service classes   |
| `Parser` suffix  | `MavenParser`, `GradleParser`                 | Dependency parsers          |
| `*Test` suffix   | `OsVApiServiceTest`                           | Test classes                |
| - Kt\*\* suffix  | `OsVPlugin.kt`                                | Kotlin files (all)          |
| `*Configurable`  | `OsVConfigurable`                             | IntelliJ settings UI panels |
| `*Panel`         | `OsVToolWindowPanel`                          | Swing UI panels             |

## Key File Locations

- **Plugin manifest**: `src/main/resources/META-INF/plugin.xml`
- **Build script**: `build.gradle.kts`
- **Main entry point**: `src/main/java/io/dyuti/osvplugin/OsVPlugin.kt`
- **API client**: `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`
- **Inspection**: `src/main/java/io/dyuti/osvplugin/inspection/OsVInspection.kt`
- **Tool window**: `src/main/java/io/dyuti/osvplugin/toolwindow/OsVToolWindowFactory.kt`
- **Config**: `src/main/java/io/dyuti/osvplugin/config/OsVConfig.kt`
