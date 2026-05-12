# Phase 21: Final Polish, Iconography, and Marketplace Release Checklist - Context

**Gathered:** 2026-05-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Final polish, branding, and marketplace readiness for the OSV IntelliJ Plugin v1.1.0. This phase delivers production-quality UI, fixes all known technical debt, completes marketplace packaging requirements, expands test coverage, polishes documentation, and hardens the build/distribution pipeline. No new features — only quality gates and release readiness.

</domain>

<decisions>
## Implementation Decisions

### Iconography & Visual Polish

- **D-01:** Keep IntelliJ `AllIcons` for severity icons (consistent with IDE) — custom icons only for plugin branding
- **D-02:** Add dark mode support via `JBColor` — replace all hardcoded AWT `Color` objects in `SeverityUtil.kt`
- **D-03:** Add animated scanning indicator via `AnimatedIcon` during active scans (replaces static "Scanning..." text)
- **D-04:** Add dedicated toolbar buttons for scan / clear / export via IntelliJ `ActionManager`
- **D-05:** Use severity-colored notification icons via `NotificationType` (ERROR, WARNING, INFORMATION)
- **D-06:** Add status bar widget showing scan status and vulnerability count (reuses existing `statusLabel` pattern)

### Known Issue Resolution

- **D-07:** Fix CacheManager singleton bug — convert `CacheManager.getInstance()` from `new instance` to true `ApplicationManager.getService()` singleton
- **D-08:** Add configurable OSV API URL to `OsVConfig` (remove hardcoded `https://api.osv.dev/v1/query`)
- **D-09:** Extend regex parser coverage patterns for Maven/Gradle; document known parsing limitations
- **D-10:** Populate `vulnerability.packageName` from query context in `OsVApiService.parseVulnerability()`
- **D-11:** Replace broad `catch (_: Exception)` with proper `Logger.error()` logging
- **D-12:** Move rate limit counters to `ApplicationManager` service scope (global, not per-instance)
- **D-13:** Encrypt sensitive tokens (Jira, GitHub) using IntelliJ `PasswordSafe`
- **D-14:** Replace all `System.err.println()` with `com.intellij.openapi.diagnostic.Logger`
- **D-15:** Keep ASCII sparkline for HistoricalTrendPanel (functional, acceptable for v1); document as known limitation
- **D-16:** Add file-level problem markers via `ProblemDescriptor` at dependency text offset (line-level requires PsiElement resolution, deferred)

### Marketplace Packaging

- **D-17:** Polish `plugin.xml` description and change notes for marketplace listing
- **D-18:** Verify `sinceBuild="233.0"` and `untilBuild="262.*"` against actual API usage in codebase
- **D-19:** Generate 3 annotated screenshots: tool window, inline inspection highlight, quick-fix popup
- **D-20:** Add marketplace tags: "security", "dependencies", "vulnerability scanning", "SAST"
- **D-21:** Sign plugin ZIP via JetBrains Plugin Signing Service
- **D-22:** Create 128x128 marketplace listing icon
- **D-23:** Update `vendor` element (name, email, URL) in `plugin.xml`
- **D-24:** Use JetBrains Marketplace built-in auto-update (no custom update site URL needed)

### Testing & Quality Gates

- **D-25:** Add integration tests via `HeavyPlatformTestCase` for PSI inspection + tool window pipeline
- **D-26:** Create `TESTING-CHECKLIST.md` with manual smoke-test steps per feature
- **D-27:** Add performance benchmark (JMH or `RepeatablePerformanceTest`) for large dependency lists
- **D-28:** Test compatibility against `2023.3` (minimum) and `2024.1+` (latest stable)
- **D-29:** Add `MockWebServer` dependency; replace live OSV API calls in `OsVApiServiceTest`
- **D-30:** Add thread safety tests for concurrent cache access and semaphore stress
- **D-31:** Add UI regression tests via `PlatformTestUtil.assertTreeStructure()` and light UI test

### Documentation & Onboarding

- **D-32:** Update README.md with all current features, accurate screenshots, and feature matrix
- **D-33:** Create `CHANGELOG.md` for versions 1.0.0 and 1.1.0
- **D-34:** Verify and update `GETTING_STARTED.md` build/run/debug instructions
- **D-35:** Update `PLUGIN_DOCUMENTATION.md` with full feature matrix
- **D-36:** Add `CONTRIBUTING.md` with contributor guidelines and PR template
- **D-37:** Add FAQ/Troubleshooting section documenting proxy, rate limit, and false positive handling

### Build & Distribution

- **D-38:** Audit shadow JAR for unused transitive deps; remove bloat
- **D-39:** Review shadow `minimize` exclusions; ensure no runtime `NoClassDefFoundError`
- **D-40:** Configure multi-version builds against IntelliJ platform 233 and 241+
- **D-41:** Verify IntelliJ Gradle Plugin v1.17.0 compatibility with Gradle 8.x
- **D-42:** Ensure artifact follows `osv-plugin-{version}.zip` naming convention
- **D-43:** Use `-SNAPSHOT` suffix for dev builds; strip for release builds
- **D-44:** Add GitHub Actions workflow for automated build → sign → publish to JetBrains Marketplace

### the agent's Discretion

- Exact icon SVG/PNG design for marketplace listing
- Specific benchmark thresholds (document what's reasonable)
- Exact wording of documentation copy
- Visual styling of animated scanning indicator
- Which exact transitive deps to exclude from shadow JAR

</decisions>

<canonical_refs>

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Plugin Manifest & Build

- `src/main/resources/META-INF/plugin.xml` — Extension points, inspections, tool windows, services
- `build.gradle.kts` — Build configuration, shadow JAR, platform versions, dependencies
- `META-INF/plugin.xml` — Alternative manifest location in project root

### Codebase Mapping

- `.planning/codebase/STACK.md` — Technology stack and dependencies
- `.planning/codebase/ARCHITECTURE.md` — System design, data flow, plugin extension points
- `.planning/codebase/CONVENTIONS.md` — Coding patterns, error handling, threading model
- `.planning/codebase/TESTING.md` — Test framework, patterns, coverage targets
- `.planning/codebase/CONCERNS.md` — Complete issue list (19 items) with file paths and context

### Existing Documentation

- `README.md` — User-facing documentation
- `PLUGIN_DOCUMENTATION.md` — Detailed plugin docs
- `GETTING_STARTED.md` — Developer onboarding
- `COMPETITIVE_RESEARCH.md` — Market analysis
- `ENTERPRISE_GAP_ANALYSIS.md` — Enterprise feature gaps

### Prior Phase Contexts

- `.planning/phases/06-foundation-fixes/06-CONTEXT.md` — Foundation fix decisions and patterns
- `.planning/phases/05-add-not-yet-implemented-features-as-phase-2/05-CONTEXT.md` — Feature completion context

</canonical_refs>

<code_context>

## Existing Code Insights

### Reusable Assets

- **SeverityUtil** (`src/main/java/io/dyuti/osvplugin/utils/SeverityUtil.kt`) — Severity-to-color/icon mapping; needs `JBColor` migration
- **OsVToolWindowFactory** — 3-tab tool window; needs toolbar actions wired
- **OsVInspection** — Multi-language inspection; needs PSI-level problem registration enhancement
- **OsVToolWindowPanel** — Tree-based vulnerability display; needs `AnimatedIcon` for scanning state
- **CacheManager** — Broken singleton pattern; fixable with `ApplicationManager.getService()`
- **OsVConfig** — Persistent state component; extensible for new config fields (API URL, encrypted tokens)

### Established Patterns

- **PersistentStateComponent** — Plugin config via `@State` + `Storage` annotations (XML persistence)
- **Backgroundable Task** — All long-running ops wrapped in `ProgressManager.run(Backgroundable(...))`
- **WriteCommandAction** — Document mutations with undo support
- **Companion object singletons** — Most services use `getInstance()` pattern
- **Regex-based parsing** — All dependency parsers use regex on raw text (Maven, Gradle, Npm, Pip)

### Integration Points

- **plugin.xml** — All new extensions (inspections, actions, tool windows, status bar widgets) register here
- **OsVConfig** — New config fields automatically persisted via `PersistentStateComponent`
- **NotificationGroup** — Already registered in plugin.xml for balloon notifications
- **ToolWindow** — Already registered; new tabs added in factory's `createToolWindowContent()`

</code_context>

<specifics>
## Specific Ideas

- "Severity icons should feel native — use IntelliJ's built-in FatalError, Error, Warning icons"
- "Dark mode is table stakes for an IntelliJ plugin — every color must use JBColor"
- "Cache bug is embarrassing for production — fix before anything else"
- "MockWebServer is standard for Kotlin HTTP testing — add to test dependencies"
- "JetBrains marketplace screenshots should show a real project with real vulnerabilities"
- "ASCII sparkline is fine for v1 — don't over-engineer charts before release"
- "PasswordSafe is the IntelliJ way to store secrets — use it for Jira/GitHub tokens"

</specifics>

<deferred>
## Deferred Ideas

- Line-level `PsiElement` problem registration (exact-line navigation in problems view) — requires PSI tree traversal, Phase 22
- Proper chart library for historical trends (JFreeChart, XChart) — Phase 22
- Support for Gradle version catalogs / `libs.versions.toml` — Phase 22
- Container scanning (Docker images) — Backlog
- Mobile app scanning (Android/iOS) — Backlog
- Real-time flagging when typing package names — Backlog
- Pre-commit hooks to block malware commits — Backlog

</deferred>

---

_Phase: 21-final-polish-marketplace-release_
_Context gathered: 2026-05-12_
