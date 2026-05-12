# Graph Report - .  (2026-05-12)

## Corpus Check
- 136 files · ~70,863 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1166 nodes · 1688 edges · 75 communities (37 shown, 38 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 239 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Notification Models|Notification Models]]
- [[_COMMUNITY_OSV Tool Window Panel|OSV Tool Window Panel]]
- [[_COMMUNITY_Risk Scoring Service|Risk Scoring Service]]
- [[_COMMUNITY_Config Audit Service|Config Audit Service]]
- [[_COMMUNITY_Auto Fix Service|Auto Fix Service]]
- [[_COMMUNITY_Cache Manager|Cache Manager]]
- [[_COMMUNITY_Aggregated Vulnerability Service|Aggregated Vulnerability Service]]
- [[_COMMUNITY_Toolbar Actions|Toolbar Actions]]
- [[_COMMUNITY_Policy Config|Policy Config]]
- [[_COMMUNITY_SAST Analyzers|SAST Analyzers]]
- [[_COMMUNITY_Affected Function  Reachability|Affected Function / Reachability]]
- [[_COMMUNITY_Jira Integration|Jira Integration]]
- [[_COMMUNITY_OSV Config  PasswordSafe|OSV Config / PasswordSafe]]
- [[_COMMUNITY_Privacy Hasher|Privacy Hasher]]
- [[_COMMUNITY_License Models|License Models]]
- [[_COMMUNITY_Historical Scan Models|Historical Scan Models]]
- [[_COMMUNITY_SBOM Generator Tests|SBOM Generator Tests]]
- [[_COMMUNITY_Organization Manager|Organization Manager]]
- [[_COMMUNITY_Dependency Parser Interface|Dependency Parser Interface]]
- [[_COMMUNITY_Historical Scan Repository Tests|Historical Scan Repository Tests]]
- [[_COMMUNITY_License Registry Service|License Registry Service]]
- [[_COMMUNITY_Gradle Parser|Gradle Parser]]
- [[_COMMUNITY_Quick Fix & Auto Fix Tests|Quick Fix & Auto Fix Tests]]
- [[_COMMUNITY_OSV API Service Tests|OSV API Service Tests]]
- [[_COMMUNITY_SARIF Exporter|SARIF Exporter]]
- [[_COMMUNITY_Diff Analyzer|Diff Analyzer]]
- [[_COMMUNITY_OSV Quick Fix|OSV Quick Fix]]
- [[_COMMUNITY_Summary Button UI|Summary Button UI]]
- [[_COMMUNITY_SBOM Generator|SBOM Generator]]
- [[_COMMUNITY_Auto Fix Service Tests|Auto Fix Service Tests]]
- [[_COMMUNITY_Diff Analyzer Tests|Diff Analyzer Tests]]
- [[_COMMUNITY_Maven Parser Tests|Maven Parser Tests]]
- [[_COMMUNITY_License Scanner Service|License Scanner Service]]
- [[_COMMUNITY_OSV Inspection|OSV Inspection]]
- [[_COMMUNITY_Notification Service Tests|Notification Service Tests]]
- [[_COMMUNITY_GitHub Advisory API Service|GitHub Advisory API Service]]
- [[_COMMUNITY_Maven Parser|Maven Parser]]
- [[_COMMUNITY_Team Config Manager|Team Config Manager]]
- [[_COMMUNITY_GitHub Advisory API Tests|GitHub Advisory API Tests]]
- [[_COMMUNITY_Privacy Service Tests|Privacy Service Tests]]
- [[_COMMUNITY_Vulnerable API Service Tests|Vulnerable API Service Tests]]
- [[_COMMUNITY_Status Bar Widget|Status Bar Widget]]
- [[_COMMUNITY_NVD API Service|NVD API Service]]
- [[_COMMUNITY_Malicious Package Service Tests|Malicious Package Service Tests]]
- [[_COMMUNITY_Privacy Service  Configurable|Privacy Service / Configurable]]
- [[_COMMUNITY_SAST Analyzer Tests|SAST Analyzer Tests]]
- [[_COMMUNITY_Policy Evaluator Tests|Policy Evaluator Tests]]
- [[_COMMUNITY_Path Traversal Detector|Path Traversal Detector]]
- [[_COMMUNITY_NPM Parser|NPM Parser]]
- [[_COMMUNITY_Config Audit Service Tests|Config Audit Service Tests]]
- [[_COMMUNITY_License Registry Service Tests|License Registry Service Tests]]
- [[_COMMUNITY_Vulnerability Models|Vulnerability Models]]
- [[_COMMUNITY_PIP Parser|PIP Parser]]
- [[_COMMUNITY_Risk Scoring Tests|Risk Scoring Tests]]
- [[_COMMUNITY_Build Gradle|Build Gradle]]
- [[_COMMUNITY_Plugin Entry Point|Plugin Entry Point]]
- [[_COMMUNITY_Severity Util|Severity Util]]
- [[_COMMUNITY_SARIF Exporter Tests|SARIF Exporter Tests]]
- [[_COMMUNITY_Help Link Provider|Help Link Provider]]
- [[_COMMUNITY_New Issues Filter|New Issues Filter]]
- [[_COMMUNITY_Branch Manager|Branch Manager]]
- [[_COMMUNITY_License Policy Config|License Policy Config]]
- [[_COMMUNITY_Jira Issue Creator|Jira Issue Creator]]
- [[_COMMUNITY_License Inspection|License Inspection]]
- [[_COMMUNITY_Historical Scan Repository|Historical Scan Repository]]
- [[_COMMUNITY_Historical Trend Panel|Historical Trend Panel]]
- [[_COMMUNITY_Summary UI Model|Summary UI Model]]
- [[_COMMUNITY_Historical JSON Serializer|Historical JSON Serializer]]
- [[_COMMUNITY_Scan Completion Listener|Scan Completion Listener]]
- [[_COMMUNITY_SBOM Exporter Panel|SBOM Exporter Panel]]
- [[_COMMUNITY_License Parser|License Parser]]
- [[_COMMUNITY_Documentation  Wiki|Documentation / Wiki]]
- [[_COMMUNITY_Plugin XML  Resources|Plugin XML / Resources]]
- [[_COMMUNITY_Team Permission Service|Team Permission Service]]

## God Nodes (most connected - your core abstractions)
1. `Dependency` - 47 edges
2. `Vulnerability` - 32 edges
3. `ConfigAuditServiceTest` - 27 edges
4. `HistoricalScanRepositoryTest` - 24 edges
5. `SbomGeneratorTest` - 23 edges
6. `PolicyConfig` - 21 edges
7. `OsVToolWindowPanel` - 21 edges
8. `OsVApiServiceTest` - 20 edges
9. `LicenseRegistryService` - 20 edges
10. `AutoFixServiceTest` - 19 edges

## Surprising Connections (you probably didn't know these)
- `Core Features (README)` --semantically_similar_to--> `Parser Layer Documentation`  [INFERRED] [semantically similar]
  README.md → PLUGIN_DOCUMENTATION.md
- `OSV IntelliJ Plugin` --conceptually_related_to--> `Getting Started Guide`  [INFERRED]
  README.md → GETTING_STARTED.md
- `Changelog v1.1.0` --conceptually_related_to--> `Advanced Features v1.1.0`  [INFERRED]
  CHANGELOG.md → README.md
- `SARIF Export` --conceptually_related_to--> `API Services Documentation`  [INFERRED]
  README.md → PLUGIN_DOCUMENTATION.md
- `SBOM Generation` --conceptually_related_to--> `API Services Documentation`  [INFERRED]
  README.md → PLUGIN_DOCUMENTATION.md

## Hyperedges (group relationships)
- **v1.1.0 Feature Documentation** — changelog_v1_1_0, readme_advanced_features, plugin_doc_architecture [INFERRED 0.85]
- **Duplicate Documentation Content** — docs_getting_started, getting_started_guide, docs_plugin_doc [EXTRACTED 1.00]
- **Enterprise Planning Trilogy** — enterprise_gap_analysis, enterprise_task_list, research_summary [INFERRED 0.80]

## Communities (75 total, 38 thin omitted)

### Community 0 - "Notification Models"
Cohesion: 0.06
Nodes (8): ScanMetrics, TeamConfig, TeamPolicyOverrides, VulnerabilityNotification, NotificationModelsTest, TeamConfigManagerTest, NotificationService, NotificationServiceTest

### Community 1 - "OSV Tool Window Panel"
Cohesion: 0.09
Nodes (7): ModuleTreeNode, OsVToolWindowPanel, OsVTreeModelBuilder, SeverityGroupTreeNode, SeverityTreeCellRenderer, VulnerabilityTreeNode, TreeModelRegressionTest

### Community 2 - "Risk Scoring Service"
Cohesion: 0.06
Nodes (9): CisaKevCatalog, CisaKevEntry, EpssEntry, EpssResponse, RiskAssessment, RiskLevel, CisaKevCatalogWrapper, RiskScoringService (+1 more)

### Community 3 - "Config Audit Service"
Cohesion: 0.09
Nodes (5): ConfigAuditFinding, ConfigAuditResult, ConfigSeverity, ConfigAuditService, ConfigAuditServiceTest

### Community 4 - "Auto Fix Service"
Cohesion: 0.11
Nodes (7): AutoFixService, getInstance(), GradleFixer, MavenFixer, NpmFixer, PipFixer, SemVerComparator

### Community 5 - "Cache Manager"
Cohesion: 0.07
Nodes (6): CacheEntry, CacheManager, CacheState, StringCacheEntry, CacheManagerConcurrencyTest, CacheManagerTest

### Community 6 - "Aggregated Vulnerability Service"
Cohesion: 0.09
Nodes (7): AggregatedVulnerabilityService, VulnerabilityQuery, VulnerabilityWithSource, MockOsVApiServiceTest, getAggregatedService(), OsVApiException, OsVApiService

### Community 7 - "Toolbar Actions"
Cohesion: 0.07
Nodes (7): ClearResultsAction, ExportAction, ScanAction, HistoricalTrendPanel, SbomExporter, OsVToolWindowFactory, SbomExportPanel

### Community 8 - "Policy Config"
Cohesion: 0.11
Nodes (13): EnforcementMode, Fail, globMatch(), Pass, PolicyConfig, PolicyResult, PolicyViolation, Warning (+5 more)

### Community 9 - "SAST Analyzers"
Cohesion: 0.07
Nodes (7): PathTraversalDetector, SastAnalyzer, SastFinding, SastSeverity, SastAnalyzerTest, SqlInjectionDetector, XssDetector

### Community 10 - "Affected Function / Reachability"
Cohesion: 0.09
Nodes (6): AffectedFunction, ReachabilityResult, VulnerableCallSite, ReachabilityResultTest, VulnerableApiService, VulnerableApiServiceTest

### Community 11 - "Jira Integration"
Cohesion: 0.12
Nodes (13): getInstance(), JiraCommentBody, JiraCommentContent, JiraCommentText, JiraConnector, JiraCreateResponse, JiraIssue, JiraIssueFields (+5 more)

### Community 12 - "OSV Config / PasswordSafe"
Cohesion: 0.1
Nodes (11): getGithubToken(), getJiraToken(), getPrivacySalt(), getSecurePassword(), OsVConfig, setGithubToken(), setJiraToken(), setPrivacySalt() (+3 more)

### Community 14 - "License Models"
Cohesion: 0.12
Nodes (9): DependencyWithLicense, isCopyleft(), isPermissive(), License, LicenseConflict, LicenseConflictDetector, Severity, LicenseScanner (+1 more)

### Community 15 - "Historical Scan Models"
Cohesion: 0.12
Nodes (9): fromScanResult(), HistoricalScanRecord, TrendDelta, TrendDirection, TrendSummary, TrendWindow, computeDelta(), computeOverallDirection() (+1 more)

### Community 17 - "Organization Manager"
Cohesion: 0.11
Nodes (9): getInstance(), LicensePolicy, Member, Organization, OrganizationManager, Permission, Role, Scope (+1 more)

### Community 18 - "Dependency Parser Interface"
Cohesion: 0.12
Nodes (3): DependencyParser, MavenParser, MavenParserTest

### Community 20 - "License Registry Service"
Cohesion: 0.16
Nodes (3): getInstance(), LicenseRegistryService, LicenseRegistryServiceTest

### Community 21 - "Gradle Parser"
Cohesion: 0.12
Nodes (4): GradleParser, GradleParserTest, NpmParser, PipParser

### Community 22 - "Quick Fix & Auto Fix Tests"
Cohesion: 0.15
Nodes (3): OsVQuickFixTest, Vulnerability, VulnerabilityTest

### Community 24 - "SARIF Exporter"
Cohesion: 0.19
Nodes (14): SarifArtifactLocation, SarifDriver, SarifExporter, SarifLocation, SarifMessage, SarifPhysicalLocation, SarifProperties, SarifReport (+6 more)

### Community 25 - "Diff Analyzer"
Cohesion: 0.13
Nodes (12): compare(), createSnapshot(), DiffAnalyzer, toModel(), ChangeType, DiffResult, PackageChange, ScanSnapshot (+4 more)

### Community 26 - "OSV Quick Fix"
Cohesion: 0.22
Nodes (5): createIgnoreFix(), createSuppressFix(), createUpgradeFix(), FixType, OsVQuickFix

### Community 27 - "Summary Button UI"
Cohesion: 0.19
Nodes (5): mouseClicked(), mouseEntered(), mouseExited(), RoundedPanelWithBackgroundColor, SummaryButton

### Community 28 - "SBOM Generator"
Cohesion: 0.2
Nodes (11): SbomGenerator, CycloneDxBom, CycloneDxComponent, CycloneDxMetadata, CycloneDxTool, SbomFormat, SpdxCreationInfo, SpdxDocument (+3 more)

### Community 31 - "Maven Parser Tests"
Cohesion: 0.15
Nodes (11): NvdApiResponse, NvdApiService, NvdConfiguration, NvdCpeMatch, NvdCve, NvdCvss, NvdCvssData, NvdDescription (+3 more)

### Community 33 - "OSV Inspection"
Cohesion: 0.17
Nodes (9): GithubAdvisory, GitHubAdvisoryApiService, GithubAffected, GithubCVSS, GithubCwe, GithubPackage, GithubPatched, GithubReference (+1 more)

### Community 36 - "Maven Parser"
Cohesion: 0.28
Nodes (4): MaliciousCheckResult, MaliciousPackageService, MaliciousType, TyposquatResult

### Community 37 - "Team Config Manager"
Cohesion: 0.15
Nodes (13): Getting Started (docs/), Plugin Documentation (docs/), FAQ Troubleshooting, Getting Started Guide, API Services Documentation, Plugin Architecture, Parser Layer Documentation, Core Features (README) (+5 more)

### Community 46 - "Policy Evaluator Tests"
Cohesion: 0.32
Nodes (5): load(), parseConfig(), save(), TeamConfigManager, toJson()

### Community 52 - "PIP Parser"
Cohesion: 0.33
Nodes (3): DependencyWithVulnerabilities, VulnerabilityCacheEntry, VulnerabilityResult

### Community 57 - "SARIF Exporter Tests"
Cohesion: 0.33
Nodes (7): Competitive Research Report, Enterprise Gap Analysis vs Snyk/GitHub, Enterprise Feature Task List, Rationale: Minimum Viable Enterprise Grade, Rationale: Align with JetBrains-native Patterns, Rationale: Malicious Package Detection as Unique Differentiator, Competitive Research Summary

### Community 59 - "New Issues Filter"
Cohesion: 0.4
Nodes (5): Contributing Guidelines, Implementation Summary v1.0, JetBrains Compliance Report, Production Readiness Report v1.0, Manual Testing Checklist

### Community 61 - "License Policy Config"
Cohesion: 0.5
Nodes (3): OsVSeverity, Package, Version

### Community 63 - "License Inspection"
Cohesion: 0.67
Nodes (3): Changelog v1.1.0, Known Limitations (FAQ), Advanced Features v1.1.0

### Community 64 - "Historical Scan Repository"
Cohesion: 0.67
Nodes (3): Fix Implementation Summary, Navigation Fix (EDT Threading), Rationale: Prevent EDT Deadlock

## Knowledge Gaps
- **89 isolated node(s):** `TeamConfigManager`, `Team`, `Member`, `Permission`, `Role` (+84 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Vulnerability` connect `Quick Fix & Auto Fix Tests` to `Notification Models`, `OSV Tool Window Panel`, `OSV Inspection`, `Cache Manager`, `Aggregated Vulnerability Service`, `Policy Config`, `Vulnerable API Service Tests`, `Affected Function / Reachability`, `Historical Scan Repository Tests`, `PIP Parser`, `Diff Analyzer`, `License Policy Config`, `Diff Analyzer Tests`, `Maven Parser Tests`?**
  _High betweenness centrality (0.242) - this node is a cross-community bridge._
- **Why does `Dependency` connect `Auto Fix Service Tests` to `License Scanner Service`, `Aggregated Vulnerability Service`, `Vulnerable API Service Tests`, `Affected Function / Reachability`, `SBOM Generator Tests`, `Dependency Parser Interface`, `Historical Scan Repository Tests`, `License Registry Service`, `PIP Parser`, `Quick Fix & Auto Fix Tests`, `OSV API Service Tests`, `Gradle Parser`, `Help Link Provider`, `License Policy Config`, `Diff Analyzer Tests`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Are the 46 inferred relationships involving `Dependency` (e.g. with `.`MavenFixer updates direct dependency version literal`()` and `.`MavenFixer updates property reference in dependency`()`) actually correct?**
  _`Dependency` has 46 INFERRED edges - model-reasoned connections that need verification._
- **Are the 31 inferred relationships involving `Vulnerability` (e.g. with `.makeVuln()` and `.vuln()`) actually correct?**
  _`Vulnerability` has 31 INFERRED edges - model-reasoned connections that need verification._
- **What connects `TeamConfigManager`, `Team`, `Member` to the rest of the system?**
  _89 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Notification Models` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `OSV Tool Window Panel` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._