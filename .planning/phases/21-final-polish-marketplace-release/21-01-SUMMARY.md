# Plan 21-01: Critical Bug Fixes — Execution Summary

**Executed:** 2026-05-12
**Status:** Complete
**Phase:** 21 - Final Polish, Iconography, and Marketplace Release

## Tasks Completed

### 21-01-a: Fix CacheManager singleton
- **Changed:** `CacheManager.getInstance()` now uses `ApplicationManager.getService(CacheManager::class.java)` instead of creating a new instance each call
- **Registry:** Added `<applicationService serviceImplementation="io.dyuti.osvplugin.utils.CacheManager"/>` to `plugin.xml`
- **Impact:** Caching now actually works across the plugin; all components share one cache instance
- **Commit:** `3755d9e`

### 21-01-b: Fix OsVApiService singleton
- **Changed:** `OsVApiService.getInstance()` now uses `ApplicationManager.getService(OsVApiService::class.java)`
- **Registry:** Added `<applicationService serviceImplementation="io.dyuti.osvplugin.api.OsVApiService"/>` to `plugin.xml`
- **Impact:** Rate limiting and request counters now persist globally; fixes per-instance reset bug
- **Commit:** `06ba117`

### 21-01-c: Add configurable OSV API URL
- **Changed:** Added `osvApiUrl` field to `OsVConfig` (default: `https://api.osv.dev/v1/query`)
- **UI:** Added text field in `OsVConfigurable` settings panel
- **Wire:** `OsVApiService` reads API URL from config via lazy getter
- **Impact:** Users can now configure private/custom OSV instances
- **Commit:** `59b69d8`

### 21-01-d: Replace System.err.println with Logger
- **Changed:** Replaced all 6 `System.err.println()` calls across 5 files with `LOG.error()` using IntelliJ `Logger`
- **Files:** `OsVApiService.kt`, `OsVInspection.kt`, `OsVToolWindowPanel.kt`, `SarifExporter.kt`, `LicenseInspection.kt`
- **Impact:** Proper IDE logging integration; errors appear in IDE log, are filterable by category, and respect log level settings
- **Commit:** `8bfe6fb`

### 21-01-e: Populate Vulnerability.packageName from query context
- **Changed:** Threaded `packageName` through `parseVulnerabilities(responseBody, packageName)` and `parseVulnerability(vuln, packageName)`
- **Callers:** `queryVulnerabilities(packageName, ...)` and `batchQueryVulnerabilities(dep.name)` both pass package name
- **Impact:** `vulnerability.packageName` is no longer always empty string; enables proper package association in displays
- **Commit:** `8bfe6fb`

## Verification

| Check | Command | Status |
|-------|---------|--------|
| Zero System.err remaining | `grep -r "System.err.println" src/main/java/` | ✓ 0 matches |
| CacheManager uses ApplicationManager | `grep "getService(CacheManager" src/main/java/io/dyuti/osvplugin/utils/CacheManager.kt` | ✓ Present |
| OsVApiService uses ApplicationManager | `grep "getService(OsVApiService" src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt` | ✓ Present |
| Config has osvApiUrl | `grep "osvApiUrl" src/main/java/io/dyuti/osvplugin/config/OsVConfig.kt` | ✓ Present |
| parseVulnerabilities has packageName param | `grep "parseVulnerabilities(responseBody: String, packageName: String)" src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt` | ✓ Present |

## Key Files Modified

- `src/main/java/io/dyuti/osvplugin/utils/CacheManager.kt`
- `src/main/java/io/dyuti/osvplugin/api/OsVApiService.kt`
- `src/main/java/io/dyuti/osvplugin/config/OsVConfig.kt`
- `src/main/java/io/dyuti/osvplugin/settings/OsVConfigurable.kt`
- `src/main/java/io/dyuti/osvplugin/inspection/OsVInspection.kt`
- `src/main/java/io/dyuti/osvplugin/toolwindow/OsVToolWindowPanel.kt`
- `src/main/java/io/dyuti/osvplugin/export/SarifExporter.kt`
- `src/main/java/io/dyuti/osvplugin/license/LicenseInspection.kt`
- `src/main/resources/META-INF/plugin.xml`

## Notes

All tasks in Wave 1 are complete. Wave 2 (UI polish) can now proceed safely since the CacheManager and OsVApiService singleton bugs are fixed.
