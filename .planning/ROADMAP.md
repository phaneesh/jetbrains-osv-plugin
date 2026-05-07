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

### Phase 6: Foundation Fixes (Post-Competitive Research)

**Goal:** Fix broken/non-functional features identified during competitive analysis
**Research:** Competitive analysis completed 2026-05-06

**Features:**

- [ ] Fix CVSS severity parsing (currently defaults to MEDIUM)
- [ ] Implement real license fetching from package registries
- [ ] Convert inspections to async to prevent UI freezing
- [ ] Replace string-based auto-fix with PSI-based refactoring
- [ ] Fix batch OSV API queries

---

### Phase 7: Problems Tab Integration

**Goal:** Align with JetBrains-native patterns by adding OSV tab to standard Problems tool window
**Rationale:** Mend.io and Qodana both use this pattern; custom tool windows feel foreign

**Features:**

- [ ] Add "OSV Vulnerabilities" tab to Problems view (Alt+6)
- [ ] Line-level problem registration with exact navigation
- [ ] Severity icons matching IntelliJ conventions
- [ ] Alt+Enter quick-fixes on problems
- [ ] Retain custom tool window for dashboard/focus mode

---

### Phase 8: Vulnerable API Detection

**Goal:** Detect if vulnerable methods from dependencies are actually called in source code
**Rationale:** Mend.io and Qodana both offer this; OSV only checks versions

**Features:**

- [ ] Collect vulnerable function signatures from OSV API
- [ ] Index library method calls via PSI
- [ ] Cross-reference call sites with vulnerability data
- [ ] Highlight actual vulnerable method invocations

---

### Phase 9: Malicious Package Detection

**Goal:** Detect intentionally harmful packages and typosquatting attempts
**Rationale:** Unique differentiator — only Mend.io offers this; no free tool does

**Features:**

- [ ] OpenSSF malicious packages feed integration
- [ ] Typosquatting detection (Levenshtein distance)
- [ ] Pre-commit hooks to block malware commits
- [ ] Real-time flagging when typing package names

---

### Phase 10: Basic SAST / Taint Analysis

**Goal:** Lightweight data-flow analysis for common vulnerability classes
**Rationale:** Snyk and Qodana both offer SAST; positions OSV as full security suite

**Features:**

- [ ] SQL injection detection (source → sink tracking)
- [ ] XSS detection (untrusted input to HTML output)
- [ ] Path traversal detection
- [ ] OWASP Top 10 coverage for Java/Kotlin
- [ ] Configurable via inspection profiles

---

### Future Phases (Backlog)

- Phase 11: Privacy-preserving queries (hash package names before API calls)
- Phase 12: CI/CD integration as Qodana linter
- Phase 13: Risk scoring beyond severity (EPSS, exploitability)
- Phase 14: Policy enforcement (auto-approve/reject dependencies)
- Phase 15: Multi-tenant / team collaboration features

---

## Progress

| Phase | Goal                        | Status          | Complete   |
| ----- | --------------------------- | --------------- | ---------- |
| 1     | Core Foundation             | ✅ Complete     | 2026-04-24 |
| 2     | IntelliJ Integration        | ✅ Complete     | 2026-04-24 |
| 3     | Advanced Features           | ✅ Complete     | 2026-04-24 |
| 4     | Modern UI                   | ✅ Complete     | 2026-04-28 |
| 5     | Feature Completion          | ✅ Plan Created | 2026-04-28 |
| 6     | Foundation Fixes            | 📋 Planned      | —          |
| 7     | Problems Tab Integration    | 📋 Planned      | —          |
| 8     | Vulnerable API Detection    | 📋 Planned      | —          |
| 9     | Malicious Package Detection | 📋 Planned      | —          |
| 10    | Basic SAST / Taint Analysis | 📋 Planned      | —          |

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
