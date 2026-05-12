# OSV Plugin Manual Testing Checklist

Run these steps before each marketplace release.

## Environment Setup
- [ ] Start IntelliJ IDEA via `./gradlew runIde`
- [ ] Create/open a test project with vulnerable dependencies (log4j-core:2.14.0, spring-core:5.3.20, lodash:4.17.20)

## Core Scanning
- [ ] Click "Scan Dependencies" in the OSV tool window
- [ ] Verify vulnerabilities appear grouped by severity (Critical, High, Medium, Low)
- [ ] Verify package names are populated (not empty string) in vulnerability detail view
- [ ] Verify status bar shows "Scanning..." during scan, then shows count summary after completion
- [ ] Verify animated spinner appears during scan and disappears after

## Configuration
- [ ] Open Settings → Tools → OSV Scanner
- [ ] Verify OSV API URL field defaults to `https://api.osv.dev/v1/query`
- [ ] Change API URL to a different value, close settings, reopen — verify value persists

## Cache & Performance
- [ ] Run scan twice on the same project
- [ ] Second scan should be significantly faster (cache hit)
- [ ] Clear cache via cache manager test or restart IDE to validate cold-start performance

## Dark Mode
- [ ] Switch to Darcula theme
- [ ] Verify severity colors are readable (not washed out or invisible)
- [ ] Verify tool window background matches IDE theme

## Notifications
- [ ] Verify balloon notifications appear after scan (if notifications are enabled)
- [ ] Verify notification type matches severity (ERROR for critical, WARNING for high)

## Export
- [ ] Click Export → SARIF
- [ ] Verify exported JSON is valid and contains vulnerability details

## Quick Fixes
- [ ] Open pom.xml with a vulnerable dependency
- [ ] Press Alt+Enter on the highlighted line
- [ ] Verify quick-fix menu shows "Upgrade to fixed version" option

## Edge Cases
- [ ] Open a project with no dependency files → verify clean-scan notification
- [ ] Open a project with unsupported format → verify no crash, graceful message

## Toolbar Actions
- [ ] Verify Scan button in toolbar triggers a new scan
- [ ] Verify Clear button removes all results from the tree
- [ ] Verify Export button opens export dialog

## Status Bar Widget
- [ ] Verify widget appears in status bar
- [ ] Verify text changes to "OSV: Scanning..." during scan
- [ ] Verify text changes to "OSV: N vulnerabilities found" after scan
