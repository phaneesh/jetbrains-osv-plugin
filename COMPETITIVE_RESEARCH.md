# Competitive Research Report: JetBrains Security Plugins

**Date:** 2026-05-06
**Scope:** Snyk Security, Mend.io (WhiteSource), Qodana Security Analysis
**Goal:** Identify missing features in OSV IntelliJ Plugin and create implementation plan

---

## Executive Summary

The OSV IntelliJ Plugin has a solid foundation for dependency vulnerability scanning (SCA) but lacks critical differentiators present in competitive tools:

1. **Malicious Package Detection** — Only Mend.io offers this; massive differentiator
2. **Vulnerable API Detection** — Mend.io and Qodana both offer this; OSV only checks versions
3. **Real SAST / Taint Analysis** — Snyk and Qodana both do custom code analysis
4. **Native Problems Tab Integration** — Mend.io and Qodana use standard IDE patterns
5. **Functional License Compliance** — Non-functional in current plugin despite UI existing

---

## Current Plugin Assessment

### What Works Well
- Multi-ecosystem dependency parsing (Maven, Gradle, npm, pip)
- OSV API integration with caching
- Custom tool window with tree view
- Inline inspections with quick-fixes
- Branch comparison (Focus Mode)
- SARIF export

### What's Broken/Non-Functional
- **License scanning**: UI exists but parser only does string matching on package names
- **CVSS scoring**: Always defaults to MEDIUM; doesn't parse OSV severity properly
- **Data flow analysis**: Claimed but not implemented
- **Batch API**: Simulated as sequential calls; true batch not working
- **Aggregated service**: Primitive merge logic; no confidence scoring

### Technical Debt
- Regex-based parsers instead of PSI-based
- Synchronous API calls freeze UI
- No proper line-level inspection highlighting
- String-based auto-fix instead of PSI-based
- Missing Gradle version catalog parsing

---

## Gap Analysis Matrix

| Capability | OSV | Snyk | Mend | Qodana |
|---|---|---|---|---|
| SCA / Dep Scanning | ✅ | ✅ | ✅ | ✅ |
| SAST / Custom Code | ❌ | ✅ | ✅ | ✅ |
| Taint Analysis | ❌ | ✅ | ❌ | ✅ |
| Malicious Packages | ❌ | ❌ | ✅ | ❌ |
| Vulnerable API Usage | ❌ | ⚠️ | ✅ | ✅ |
| Problems Tab Integration | ❌ (custom TW) | Custom TW | ✅ | ✅ |
| License Compliance | ⚠️ (broken) | ✅ | ✅ | ❌ |
| Pre-Commit Checks | ❌ | ❌ | ✅ | ❌ |
| Privacy-Preserving Queries | ❌ | ❌ | ✅ | ✅ |
| CI/CD Integration | ✅ (SARIF) | ❌ | ✅ | ✅ |
| Offline Mode | ❌ | ❌ | ❌ | ✅ (taint only) |
| Bundled with IntelliJ | ❌ | ❌ | ✅ | ❌ |
| Free / Open Source | ✅ | ✅ (free tier) | ⚠️ | ❌ |

---

## Recommended Phases

### Phase 5: Foundation Fixes (Immediate)
Fix what's broken before adding new features.

- **5.1**: Fix CVSS severity parsing from OSV API
- **5.2**: Implement proper license fetching from registries
- **5.3**: Migrate to PSI-based parsers for reliability
- **5.4**: Add async/debounced inspection to prevent UI freezing
- **5.5**: Fix batch OSV API queries

### Phase 6: Problems Tab Integration (Quick Win)
 Align with JetBrains-native patterns.

- **6.1**: Add OSV Vulnerabilities tab to native Problems tool window
- **6.2**: Implement proper line-level inspection highlighting
- **6.3**: PSI-based quick-fixes with refactoring support
- **6.4**: Severity icons matching JetBrains conventions

### Phase 7: Vulnerable API Detection (Major Differentiator)
Check if vulnerable methods are actually called.

- **7.1**: Collect vulnerable function signatures from OSV API
- **7.2**: Index library method calls via PSI
- **7.3**: Cross-reference call sites with vulnerability data
- **7.4**: Highlight actual vulnerable method invocations

### Phase 8: Malicious Package Detection (Unique Differentiator)
Detect intentionally harmful packages.

- **8.1**: Integrate OpenSSF malicious packages feed
- **8.2**: Add Socket.dev or Phylum API for package reputation
- **8.3**: Typosquatting detection (similar package names)
- **8.4**: Pre-commit hooks to block malicious dependencies

### Phase 9: Basic SAST / Taint Analysis (Advanced)
Lightweight data-flow analysis for common vulnerability classes.

- **9.1**: Implement source/sink detection for SQLi, XSS, path traversal
- **9.2**: Basic intra-procedural taint propagation
- **9.3**: Configuration via inspection profiles
- **9.4**: OWASP Top 10 coverage for Java/Kotlin

### Phase 10: Advanced Features

- **10.1**: Privacy-preserving queries (hash package names before API calls)
- **10.2**: CI/CD Qodana linter integration
- **10.3**: Policy enforcement (block/approve dependency changes)
- **10.4**: Risk scoring beyond severity (exploitability, EPSS)

---

## Priority Stack-Rank

1. **🔴 Fix CVSS/Severity parsing** — Currently all vulnerabilities show as MEDIUM
2. **🔴 Fix License Scanning** — UI exists but is non-functional; deceptive UX
3. **🟡 Problems Tab Integration** — Low effort, highJetBrains-alignment value
4. **🟡 Vulnerable API Detection** — High value differentiator; OSV API may support this
5. **🟢 Malicious Package Detection** — Unique differentiator if no one else in free tier
6. **🟢 Basic Taint Analysis** — High effort but positions plugin as full security suite
7. **🔵 Privacy-Preserving Queries** — Good practice, easy implementation
8. **🔵 CI/CD Integration** — SARIF exists but could be linter-native
