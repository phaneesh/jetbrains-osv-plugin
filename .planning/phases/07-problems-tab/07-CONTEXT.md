# Phase 7: Problems Tab Integration

## Objective

Migrate from a custom tool window to the standard JetBrains Problems tool window (`Alt+6`) with a dedicated **OSV Vulnerabilities** tab. This aligns with JetBrains' bundled Package Checker (Mend.io) and Qodana patterns.

## Why This Matters

Competitive analysis shows:

- **Mend.io Package Checker** uses native Problems tab — bundled, zero-friction
- **Qodana Security Analysis** uses native Problems tab — feels like part of IDE
- **OSV Plugin** uses custom tool window — feels foreign, breaks muscle memory
- JetBrains Platform SDK recommends ProblemsView for code quality findings

## Features

### 7.1 Problems Tab Extension

- Add "OSV Vulnerabilities" tab alongside "Project Errors", "Current File", etc.
- Use `com.intellij.analysis.problemsView.toolWindow` extension point
- Or create custom `ProblemsViewTab` implementing `ProblemsProvider`

### 7.2 Line-Level Precision

- Register problems at exact line numbers in dependency files
- Show CVE ID, severity icon, summary in problem description
- Support click-to-navigate to exact dependency line

### 7.3 Severity Matching

- CRITICAL → Error (red icon)
- HIGH → Warning (orange icon)
- MEDIUM → Weak Warning (yellow icon)
- LOW → Info (gray icon)

### 7.4 Quick-Fix Integration

- Alt+Enter context menu on problem shows:
  - Update to fixed version
  - Navigate to OSV page
  - Suppress for this package
  - Ignore this CVE

### 7.5 Retain Custom Tool Window (Optional)

- Keep existing OSV tool window for:
  - Project-wide vulnerability dashboard
  - Filtering and search across all modules
  - SARIF export
  - Focus mode / branch comparison
- Problems tab used for:
  - Real-time inline feedback
  - Per-file vulnerability list
  - Quick navigation and fixing

## Success Criteria

- [ ] OSV Vulnerabilities tab appears in Problems tool window
- [ ] Problems show at exact line numbers
- [ ] Severity icons match standard IntelliJ conventions
- [ ] Alt+Enter quick-fixes work on problems
- [ ] Clicking problem navigates to dependency line
- [ ] Existing tool window still functional

## Technical Notes

- Extension point: `com.intellij.problemsView` or `com.intellij.analysis.problemsView`
- May need `com.intellij.diagnostic` for errors
- Or use `ExternalAnnotator` + `ProblemsView` combination
- Reference: `com.intellij.codeInsight.daemon.impl.DefaultHighlightInfoProcessor`
