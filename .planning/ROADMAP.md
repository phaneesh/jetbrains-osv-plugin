# Project Roadmap: OSV IntelliJ Plugin

## Overview

This document outlines the phased implementation plan for the OSV IntelliJ Plugin - a free, open-source security vulnerability scanner for open-source dependencies using the OSV database.

---

### Phase 1: Core Foundation ✓ COMPLETE

**Goal:** Project setup, dependency parsing, basic vulnerability scanning
**Completed:** 2026-04-24

- [x] Project setup and initialization
- [x] Dependency parsing engine (Maven, Gradle, npm, pip)
- [x] Vulnerability scanning engine (OSV API)

---

### Phase 2: IntelliJ Integration ✓ COMPLETE

**Goal:** Seamless IDE integration - tool windows, code highlights, quick fixes
**Completed:** 2026-04-24

- [x] IntelliJ Inspection System Integration
- [x] UI Components and User Experience

---

### Phase 3: Advanced Features ✓ COMPLETE

**Goal:** Advanced vulnerability analysis - focus mode, transitive dependencies, data flow
**Completed:** 2026-04-24

- [x] Developer Focus Mode Implementation
- [x] Advanced Features and Polishing

---

### Phase 4: Modern UI ✓ COMPLETE

**Goal:** Modernize tool window UI with SonarLint-inspired design
**Completed:** 2026-04-28

- [x] UI Gap Analysis
- [x] Dashboard & Summary Panel

---

### Phase 5: Feature Completion ✓ COMPLETE (Plan Created)

**Goal:** Complete any remaining features from original requirements
**Status:** Plan created 2026-04-28

---

### Phase 6: Foundation Fixes (Post-Competitive Research) ✓ COMPLETE

**Goal:** Fix broken/non-functional features identified during competitive analysis
**Research:** Competitive analysis completed 2026-05-06
**Completed:** 2026-05-07

**Features:**

- [x] Fix CVSS severity parsing (OSV severity array → CVSS_V3/V2 → OsVSeverity)
- [x] Implement real license fetching from package registries (Maven Central, NPM, PyPI)
- [x] Convert inspections to async to prevent UI freezing (per-file cache, debounce, Backgroundable)
- [x] Replace string-based auto-fix with PSI-based refactoring + transitive dep support
- [x] Fix batch OSV API queries (parallel async with Semaphore(10))

---

### Phase 7: Problems Tab Integration ✓ COMPLETE

**Goal:** Align with JetBrains-native patterns by adding OSV tab to standard Problems tool window
**Rationale:** Mend.io and Qodana both use this pattern; custom tool windows feel foreign
**Completed:** 2026-05-07

**Features:**

- [x] Multi-language inspection registration (XML, Groovy, Kotlin, JSON, plain text)
- [x] Inspection runs on pom.xml, build.gradle, build.gradle.kts, package.json, requirements.txt
- [x] Severity mapping to IntelliJ highlight types: CRITICAL→ERROR, HIGH→WARNING, MEDIUM→WEAK_WARNING, LOW→INFO
- [x] Alt+Enter quick-fix integration (upgrade, suppress, ignore)
- [x] Custom tool window retained for dashboard, SARIF export, focus mode

**Note:** Line-level PsiElement registration for exact-line navigation is a future enhancement (currently file-level with lineNumber preserved in vulnerability model).

**Features:**

- [ ] Add "OSV Vulnerabilities" tab to Problems view (Alt+6)
- [ ] Line-level problem registration with exact navigation
- [ ] Severity icons matching IntelliJ conventions
- [ ] Alt+Enter quick-fixes on problems
- [ ] Retain custom tool window for dashboard/focus mode

---

### Phase 8: Vulnerable API Detection ✓ COMPLETE

**Goal:** Detect if vulnerable methods from dependencies are actually called in source code
**Rationale:** Mend.io and Qodana both offer this; OSV only checks versions
**Completed:** 2026-05-07

**Features:**

- [x] Collect vulnerable function signatures from OSV API (`affected[].database_specific.functions`)
- [x] Index library method calls via PSI (`PsiRecursiveElementVisitor` for Java)
- [x] Cross-reference call sites with vulnerability data
- [x] Highlight actual vulnerable method invocations (`ReachabilityResult`)

**Note:** Kotlin call expression handling and full type resolution are future enhancements.

---

### Phase 9: Malicious Package Detection ✓ COMPLETE

**Goal:** Detect intentionally harmful packages and typosquatting attempts
**Rationale:** Unique differentiator — no free tool currently offers this
**Completed:** 2026-05-07

**Features:**

- [x] OSV API malicious package keyword analysis (backdoor, malware, protestware detection)
- [x] Typosquatting detection (Levenshtein distance against popular packages)
- [x] Homoglyph/unicode attack detection (Cyrillic look-alike characters)
- [x] Known malicious package list (`ua-parser-js`, `node-ipc`, `colors`, etc.)
- [ ] Pre-commit hooks to block malware commits (future)
- [ ] Real-time flagging when typing package names (future)

---

### Phase 10: Basic SAST / Taint Analysis ✓ COMPLETE

**Goal:** Lightweight static analysis for common vulnerability classes
**Rationale:** Snyk and Qodana both offer SAST; positions OSV as full security suite
**Completed:** 2026-05-07

**Features:**

- [x] SQL injection detection (`Statement.executeQuery` with untrusted input)
- [x] XSS detection (untrusted input to `PrintWriter.println`/`Model.addAttribute`)
- [x] Path traversal detection (`new FileInputStream(...)` with untrusted path)
- [ ] OWASP Top 10 coverage for Java/Kotlin (partial — 3 classes done)
- [ ] Configurable via inspection profiles (future)

**Limitations:** Pattern-based only (no full data-flow, no inter-procedural taint, Java only)

---

### Phase 11: Privacy-Preserving Queries ✓ COMPLETE

**Goal:** Obfuscate package names in UI, logs, exports, and persistent state
**Rationale:** Protect intellectual property without relying on users to self-censor screenshots and exports
**Completed:** 2026-05-07

**Features:**

- [x] SHA-256 deterministic hashing with per-installation salt (PrivacyHasher)
- [x] In-memory hash → name mapping (cleared on restart, non-recoverable)
- [x] Privacy settings panel under Tools → OSV → Privacy (toggle, rotate salt, clear cache)
- [x] SARIF export obfuscation (artifact URIs hashed when privacy mode enabled)
- [x] Thread-safe implementation via ConcurrentHashMap
- [x] Standalone PrivacyHasher with zero IntelliJ dependencies (fully unit-testable)

**Limitations:** OSV API still receives real package names (inherent — OSV indexes by name, no hash endpoint)

---

### Phase 13: Risk Scoring Beyond Severity ✓ COMPLETE

**Goal:** EPSS + CISA KEV composite risk scoring for exploitability-based prioritization
**Rationale:** Snyk/Mend.io offer EPSS as premium; positions OSV plugin as actionable decision tool
**Completed:** 2026-05-07

**Features:**

- [x] RiskScoringService: composite (CVSS×40% + EPSS×30% + KEV×20% + exploit×10%)
- [x] EPSS API integration (FIRST.org): exploitation probability + percentile
- [x] CISA KEV catalog feed: known exploited vulnerabilities
- [x] Graceful degradation: API unavailable → CVSS-only fallback
- [x] RiskLevel enum: CRITICAL≥80, HIGH≥60, MEDIUM≥40, LOW≥20, MINIMAL<20

**Limitations:** OSV API still receives real package names; EPSS scores updated daily (no persistent cache)

---

### Phase 14: Policy Enforcement ✓ COMPLETE

**Goal:** Organization-wide dependency compliance policies with auto-reject gates
**Rationale:** Enterprise users need policy-as-code for CI gates; differentiates from personal-use-only tools
**Completed:** 2026-05-07

**Features:**

- [x] PolicyConfig: severity threshold, CVSS cap, CISA KEV block, license block, glob ignore patterns
- [x] PolicyEvaluator: evaluates dependencies against policy rules
- [x] Enforcement modes: FAIL (block), WARN (non-blocking), IGNORE (skip)
- [x] Batch evaluation API for multi-dependency projects
- [x] PolicyViolation data model with human-readable messages

**Limitations:** No persistent policy storage yet (uses in-memory config); no policy file format (JSON/YAML)

---

### Phase 15: Multi-tenant / Team Collaboration ✓ COMPLETE

**Goal:** Project-level team config and IDE notifications for shared policy enforcement
**Rationale:** Organizations need consistent security posture across all IDE users on a team
**Completed:** 2026-05-07

**Features:**

- [x] TeamConfig: project-level scan settings, notification thresholds, auto-fix enablement
- [x] TeamPolicyOverrides: project-level policy overrides (maxSeverity, CVSS cap, CISA KEV, license block)
- [x] TeamConfigManager: `.idea/osv-plugin-config.json` persistence (version-controllable via git)
- [x] Config hierarchy: project-level → user-level → plugin defaults
- [x] Policy merge: project overrides applied on top of base policy
- [x] VulnerabilityNotification data model: IDE balloon/toast structured data
- [x] ScanMetrics: per-scan aggregation for team reporting

**Limitations:** No actual IDE notification service yet (models only); no UI for editing team config

---

### Future Phases (Backlog)

- Phase 16: Differential analysis (compare two scans over time)
- Phase 17: Historical trending (vulnerability counts over time charts)
- Phase 18: SBOM generation (CycloneDX/SPDX export)
- Phase 19: Configuration audit (detect insecure framework configs)
- Phase 20: IDE notification service (real-time balloon alerts for new CVEs)

---

## Progress

| Phase | Goal                         | Status          | Complete   |
| ----- | ---------------------------- | --------------- | ---------- |
| 1     | Core Foundation              | ✅ Complete     | 2026-04-24 |
| 2     | IntelliJ Integration         | ✅ Complete     | 2026-04-24 |
| 3     | Advanced Features            | ✅ Complete     | 2026-04-24 |
| 4     | Modern UI                    | ✅ Complete     | 2026-04-28 |
| 5     | Feature Completion           | ✅ Plan Created | 2026-04-28 |
| 6     | Foundation Fixes             | ✅ Complete     | 2026-05-07 |
| 7     | Problems Tab Integration     | ✅ Complete     | 2026-05-07 |
| 8     | Vulnerable API Detection     | ✅ Complete     | 2026-05-07 |
| 9     | Malicious Package Detection  | ✅ Complete     | 2026-05-07 |
| 10    | Basic SAST / Taint Analysis  | ✅ Complete     | 2026-05-07 |
| 11    | Privacy-Preserving Queries   | ✅ Complete     | 2026-05-07 |
| 12    | CI/CD Qodana linter          | ⏭ Dropped      | —          |
| 13    | Risk Scoring Beyond Severity | ✅ Complete     | 2026-05-07 |
| 14    | Policy Enforcement           | ✅ Complete     | 2026-05-07 |
| 15    | Team Collaboration           | ✅ Complete     | 2026-05-07 |

---

## Research Artifacts

- **Competitive Research Report**: `.planning/research/COMPETITIVE_RESEARCH_REPORT.md`
- **Context Analysis**: `osv-plugin-context.md` (generated)
- **Competitive Analysis**: `osv-plugin-competitive-analysis.md` (generated)

## Commands

- `/gsd-plan-phase {N}` - Plan a specific phase
- `/gsd-execute-phase {N} --auto` - Execute a phase with auto-advance
- `/gsd-discuss-phase {N} --auto` - Capture design decisions for a phase

---

_Last updated: 2026-05-06_
