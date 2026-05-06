# Phase 6: Foundation Fixes — Context

## Origin

This phase was identified during competitive research (see `.planning/research/COMPETITIVE_RESEARCH_REPORT.md`) which found that multiple core features in the OSV plugin are broken or non-functional. Before adding competitive differentiators, these foundation issues must be resolved.

## Competitive Drivers

1. **Snyk, Mend.io, Qodana** all have accurate severity scoring — OSV defaults everything to MEDIUM
2. **Mend.io** has functional license compliance with real SPDX data — OSV has UI but no backend
3. **All competitors** have smooth async scanning — OSV freezes UI on large files
4. **Snyk** has reliable quick-fixes with undo support — OSV uses fragile string replacement

## Current Broken Features

### Severity Parsing (OsVApiService.kt)
- OSV API returns `severity: [{"type": "CVSS_V3", "score": "9.8"}]`
- Plugin code always defaults to `OsVSeverity.MEDIUM`
- No parsing of CVSS vector strings or scores

### License Scanning (LicenseScannerService.kt)
- `LicenseParser.parseLicense()` only checks if package name contains "mit", "apache", etc.
- Never queries actual package metadata from registries
- LicenseConflictDetector has a full compatibility matrix but no real data to feed it
- Creates false confidence for users

### Inspection Performance (OsVInspection.kt)
- Runs synchronously on file open with network calls
- No debouncing — every keystroke in pom.xml triggers a rescan
- No caching — queries API every time file is inspected

### Auto-Fix (OsVQuickFix.kt)
- Reads entire file as string, does regex replacement
- No PSI awareness — breaks XML/JSON structure if version string appears elsewhere
- No undo support
- Fragile against formatting variations

## Success Criteria

All features must be competitive with Mend.io Package Checker (bundled free) quality.
