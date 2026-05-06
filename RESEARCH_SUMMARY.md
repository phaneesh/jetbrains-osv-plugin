# Competitive Research & Planning: Complete

**Date:** 2026-05-06
**Scope:** Snyk Security, Mend.io, Qodana Security Analysis plugins for JetBrains IDEs
**Status:** Research complete | 5 new phases planned (6-10)

---

## What Was Done

### 1. Extensive Competitive Research

Three research subagents analyzed competitive plugins:

| Plugin | Focus Area | Key Findings |
|--------|-----------|-------------|
| **Snyk Security** | SCA + SAST + IaC | 146 releases; has data flow analysis; auto-CLI management |
| **Mend.io** | SCA (bundled) + Enterprise | Malicious package detection; vulnerable API usage; privacy-preserving queries |
| **Qodana** | Taint analysis (SAST) | Interprocedural data flow; OWASP Top 10; custom rules via Kotlin DSL |

### 2. Current Plugin Gap Analysis

**What works well:**
- Multi-ecosystem parsing (Maven, Gradle, npm, pip)
- OSV API integration with caching
- Custom tool window with tree view
- Branch comparison (Focus Mode)
- SARIF export

**What's broken:**
- CVSS severity always defaults to MEDIUM (doesn't parse OSV severity data)
- License scanning is non-functional (guesses from package names, never queries registries)
- Inspections freeze UI on large files (synchronous, no debouncing)
- Auto-fix uses fragile string replacement instead of PSI refactoring

**What's missing vs. competitors:**
- Problems tab integration (Mend/Qodana use native Problems view)
- Malicious package detection (only Mend has this)
- Vulnerable API usage detection (Mend + Qodana)
- Real SAST/taint analysis (Snyk + Qodana)
- Privacy-preserving queries

### 3. Priority-Stacked Implementation Plan

| Priority | Phase | Feature | Why |
|----------|-------|---------|-----|
| 🔴 Critical | 6 | Fix CVSS severity parsing | Currently all vulns show MEDIUM |
| 🔴 Critical | 6 | Fix license scanning | UI exists but is non-functional |
| 🟡 High | 7 | Problems tab integration | Align with JetBrains-native patterns |
| 🟡 High | 8 | Vulnerable API detection | Match Mend/Qodana capability |
| 🟢 Medium | 9 | Malicious package detection | **UNIQUE** — no free alternative |
| 🟢 Medium | 10 | Basic SAST/taint analysis | Full security suite positioning |

### 4. Documents Created

| Document | Location | Lines |
|----------|----------|-------|
| Competitive Research Report | `.planning/research/COMPETITIVE_RESEARCH_REPORT.md` | 190 |
| Full Competitive Analysis | `osv-plugin-competitive-analysis.md` | 324 |
| Architecture & Gap Analysis | `osv-plugin-context.md` | 538 |
| Phase 6 Plan | `.planning/phases/06-foundation-fixes/06-01-PLAN.md` | 120 |
| Phase 7 Plan | `.planning/phases/07-problems-tab/07-01-PLAN.md` | 99 |
| Phase 8 Context | `.planning/phases/08-vulnerable-api-detection/08-CONTEXT.md` | — |
| Phase 9 Context | `.planning/phases/09-malicious-packages/09-CONTEXT.md` | — |

### 5. Updated State

- **ROADMAP.md** — Updated with Phases 6-10
- **STATE.md** — Updated with research summary and next actions
- `.planning/config.json` and `.planning/todos/` — Preserved

---

## Next Steps

To begin implementation, run:

```
/gsd-plan-phase 6
```

Phase 6 focuses on fixing foundation issues:
1. Proper CVSS severity parsing
2. Real license metadata fetching
3. Async inspection execution
4. PSI-based auto-fixes

These fixes are prerequisites for competitive feature additions in Phases 7+.

---

## Unique Market Opportunity Identified

**Malicious Package Detection (Phase 9)** is the highest-value differentiator:
- Only Mend.io offers this in the JetBrains ecosystem
- Open source feeds exist (OpenSSF, Socket.dev)
- No other free tool provides this
- High developer impact (supply chain security)

---

*Research completed by subagents: researcher, context-builder*
*Planning updated by: gsd-dispatcher*
