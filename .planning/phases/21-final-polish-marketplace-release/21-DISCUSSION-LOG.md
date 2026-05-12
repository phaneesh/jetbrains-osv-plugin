# Phase 21: Final Polish, Iconography, and Marketplace Release Checklist — Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-12
**Phase:** 21-final-polish-marketplace-release
**Areas discussed:** Iconography & Visual Polish, Known Issue Resolution, Marketplace Packaging, Testing & Quality Gates, Documentation & Onboarding, Build & Distribution

---

## Iconography & Visual Polish

| # | Option | Description | Selected |
|---|--------|-------------|----------|
| 1 | Severity Icons — IntelliJ built-in | Use `AllIcons.Ide.FatalError`, `AllIcons.General.Error`, etc. (native IDE feel) | ✓ |
| 2 | Severity Icons — Custom | Create custom color icons per severity level | |
| 3 | Dark Mode — Yes | Replace hardcoded AWT `Color` with `JBColor` | ✓ |
| 4 | Dark Mode — No | Keep hardcoded colors (ignore dark theme users) | |
| 5 | Animated Scanning — Yes | Use `AnimatedIcon` during active scans | ✓ |
| 6 | Animated Scanning — No | Keep static "Scanning..." text | |
| 7 | Toolbar Actions — Yes | Add scan / clear / export via `ActionManager` | ✓ |
| 8 | Toolbar Actions — No | Keep only tool window buttons | |
| 9 | Notifications — Severity-colored | Use `NotificationType.ERROR/WARNING` | ✓ |
| 10 | Notifications — Basic | Keep basic uncolored notifications | |
| 11 | Status Bar — Yes | Show vuln count + scan status in status bar | ✓ |
| 12 | Status Bar — No | Status only in tool window | |

**User's choice:** All items selected ("all")
**Notes:** User wants comprehensive visual polish. No known alternatives rejected — all presented options accepted.

---

## Known Issue Resolution

| # | Issue | Priority | Selected |
|---|-------|----------|----------|
| 1 | CacheManager singleton bug | 🔴 High | ✓ |
| 2 | Hardcoded OSV API URL | 🔴 High | ✓ |
| 3 | Regex parser fragility | 🟡 Medium | ✓ |
| 4 | Missing `packageName` population | 🟡 Medium | ✓ |
| 5 | Exception swallowing | 🟡 Medium | ✓ |
| 6 | Rate limiting per-instance | 🟡 Medium | ✓ |
| 7 | Unencrypted token storage | 🟢 Low | ✓ |
| 8 | System.err logging | 🟢 Low | ✓ |
| 9 | Missing chart library | 🟢 Low | ✓ |
| 10 | PSI-level problem registration | 🟢 Low | ✓ |

**User's choice:** All 10 issues selected ("1,2,3,4,5,6,7,8,9,10")
**Notes:** User wants all known issues resolved before release. Even low-priority items (chart library, PSI registration) included.

---

## Marketplace Packaging

| # | Item | Selected |
|---|------|----------|
| 1 | Plugin Description polish | ✓ |
| 2 | Version Compatibility verification | ✓ |
| 3 | Marketplace Screenshots (3) | ✓ |
| 4 | Tags & Categories | ✓ |
| 5 | Plugin Signing | ✓ |
| 6 | 128x128 Icon | ✓ |
| 7 | Vendor Info update | ✓ |
| 8 | Update Site URL | ✓ |

**User's choice:** All items selected ("all")
**Notes:** Full marketplace readiness desired.

---

## Testing & Quality Gates

| # | Item | Selected |
|---|------|----------|
| 1 | Integration Tests (HeavyPlatformTestCase) | ✓ |
| 2 | Manual Testing Checklist | ✓ |
| 3 | Performance Benchmark | ✓ |
| 4 | Compatibility Matrix | ✓ |
| 5 | Mock HTTP Responses (MockWebServer) | ✓ |
| 6 | Thread Safety Tests | ✓ |
| 7 | UI Regression Tests | ✓ |

**User's choice:** All items selected ("all")
**Notes:** Comprehensive test coverage before release.

---

## Documentation & Onboarding

| # | Item | Selected |
|---|------|----------|
| 1 | README Polish | ✓ |
| 2 | CHANGELOG.md | ✓ |
| 3 | GETTING_STARTED.md verify/update | ✓ |
| 4 | PLUGIN_DOCUMENTATION.md update | ✓ |
| 5 | CONTRIBUTING.md | ✓ |
| 6 | FAQ / Troubleshooting | ✓ |

**User's choice:** All items selected ("all")
**Notes:** Full documentation refresh for release.

---

## Build & Distribution

| # | Item | Selected |
|---|------|----------|
| 1 | Shadow JAR Size audit | ✓ |
| 2 | Minimize Config review | ✓ |
| 3 | Multi-Version Builds | ✓ |
| 4 | Gradle Plugin Version verify | ✓ |
| 5 | Artifact Naming convention | ✓ |
| 6 | SNAPSHOT vs Release | ✓ |
| 7 | GitHub Actions automated release | ✓ |

**User's choice:** All items selected ("all")
**Notes:** Full CI/CD and distribution pipeline desired.

---

## Deferred Ideas

- Line-level `PsiElement` problem registration (exact-line navigation) — Phase 22
- Proper chart library for trends — Phase 22
- Gradle version catalog support — Phase 22
- Container scanning (Docker) — Backlog
- Mobile app scanning — Backlog
- Real-time package typing flagging — Backlog
- Pre-commit malware hooks — Backlog

---

## the agent's Discretion

- Exact icon design for marketplace listing
- Specific benchmark thresholds
- Exact documentation wording
- Animated scanning indicator visual design
- Exact transitive dependency exclusions from shadow JAR
