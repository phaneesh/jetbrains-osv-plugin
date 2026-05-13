# OSV Plugin Manual Testing Checklist

Run these steps before each marketplace release. Last updated for **v1.1.2**.

---

## Environment Setup

- [ ] Start IntelliJ IDEA via `./gradlew runIde`
- [ ] Create/open a test project with vulnerable dependencies (log4j-core:2.14.0, spring-core:5.3.20, lodash:4.17.20)

---

## Core Scanning

- [ ] Click **Scan Dependencies** in the OSV tool window
- [ ] Verify vulnerabilities appear grouped by severity (Critical, High, Medium, Low)
- [ ] Verify package names are populated in vulnerability detail view (not empty string)
- [ ] Verify CVE IDs are shown preferentially (not GHSA) where available
- [ ] Verify fix versions are populated (not "N/A") where OSV provides them
- [ ] Verify status bar shows "Scanning..." during scan, then shows count summary after completion
- [ ] Verify animated spinner appears during scan and disappears after

---

## Cross-IDE Compatibility

- [ ] Test in **PyCharm** — open a project with `requirements.txt`
- [ ] Test in **WebStorm** — open a project with `package-lock.json`
- [ ] Test in **GoLand** — open a project with `go.mod`
- [ ] Verify no `ClassNotFoundError` on startup for any IDE
- [ ] Verify Java-only features (SAST, reachability) gracefully skip on non-Java IDEs

---

## Format Parity (v1.1.2+)

- [ ] **Maven** — `pom.xml`, `gradle.lockfile`, `verification-metadata.xml`
- [ ] **JavaScript** — `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`
- [ ] **Python** — `requirements.txt`, `pyproject.toml`, `poetry.lock`
- [ ] **Go** — `go.mod`
- [ ] **Rust** — `Cargo.lock`
- [ ] **PHP** — `composer.lock`
- [ ] **Ruby** — `Gemfile.lock`
- [ ] **Dart** — `pubspec.lock`
- [ ] **.NET** — `packages.lock.json`
- [ ] **Haskell** — `stack.yaml.lock`
- [ ] **Elixir** — `mix.lock`
- [ ] **R** — `renv.lock`
- [ ] **C/C++** — `conan.lock`

---

## Configuration

- [ ] Open **Settings → Tools → OSV Scanner**
- [ ] Verify OSV API URL field defaults to `https://api.osv.dev/v1/query`
- [ ] Change API URL to a different value, close settings, reopen — verify value persists
- [ ] Verify license policy settings are editable and persist
- [ ] Verify privacy mode toggle works

---

## Cache & Performance

- [ ] Run scan twice on the same project
- [ ] Second scan should be significantly faster (cache hit)
- [ ] Clear cache and verify cold-start performance is reasonable

---

## Dark Mode

- [ ] Switch to **Darcula** theme
- [ ] Verify severity colors are readable (not washed out or invisible)
- [ ] Verify tool window background matches IDE theme
- [ ] Verify charts in Trends tab render correctly in dark mode

---

## Notifications

- [ ] Verify balloon notifications appear after scan (if notifications are enabled)
- [ ] Verify notification type matches severity (ERROR for critical, WARNING for high)
- [ ] Verify notifications respect the Minimum Severity setting

---

## Exports

- [ ] **SARIF** — click Export → SARIF → verify valid JSON with vulnerability details
- [ ] **SBOM** — open SBOM tab → scan → export CycloneDX JSON → verify structure
- [ ] **SBOM** — export SPDX Tag-Value → verify text format
- [ ] **CBOM** — open CBOM tab → scan project → verify crypto assets detected
- [ ] **CBOM** — export → verify JSON contains cryptoProperties
- [ ] **QBOM** — open QBOM tab → scan project → verify PQC assets detected
- [ ] **AIBOM** — open AIBOM tab → scan project → verify AI assets detected
- [ ] **AIBOM** — verify no false positives on generic code (`.build()`, `Completion`)

---

## Quick Fixes

- [ ] Open `pom.xml` with a vulnerable dependency
- [ ] Press **Alt+Enter** on the highlighted line
- [ ] Verify quick-fix menu shows **"Upgrade to fixed version"**
- [ ] Apply the fix and verify the version is updated in the file
- [ ] Repeat for `build.gradle`, `package-lock.json`, `requirements.txt`

---

## Tool Window Tabs

| Tab | Check |
| --- | --- |
| **Vulnerabilities** | Tree populates, groups by severity, double-click navigates |
| **Trends** | Charts render (area + donut), statistics table shows data |
| **SBOM** | Dependency tree populates, export buttons work |
| **CBOM** | Scan finds crypto assets, tree groups by type/subtype |
| **QBOM** | Scan finds PQC assets, tree groups by type |
| **AIBOM** | Scan finds AI assets, tree groups by type |

---

## Edge Cases

- [ ] Open a project with **no dependency files** → verify clean-scan notification
- [ ] Open a project with **unsupported format** → verify no crash, graceful message
- [ ] Open an **empty `pom.xml`** → verify no crash
- [ ] Open a **malformed JSON lockfile** → verify graceful error handling
- [ ] Test with **Privacy Mode enabled** → verify hashed package names in tree

---

## Status Bar Widget

- [ ] Verify widget appears in status bar
- [ ] Verify text changes to **"OSV: Scanning..."** during scan
- [ ] Verify text changes to **"OSV: N vulnerabilities found"** after scan
- [ ] Verify text shows **"OSV: ✓ Clean"** when no vulnerabilities found

---

## Regression Tests

- [ ] Verify v1.1.0 features still work: Risk Scoring, Policy Enforcement, Differential Analysis
- [ ] Verify v1.1.1 features still work: BOM panels, Trends charts, SARIF export
- [ ] Verify no verifier warnings in build log (IntelliJ 2023.3 target)

---

## Build Verification

- [ ] `./gradlew test` — all tests pass
- [ ] `./gradlew buildPlugin` — produces ZIP in `build/distributions/`
- [ ] `./gradlew compileKotlin` — zero warnings
- [ ] Plugin ZIP size < 10 MB
