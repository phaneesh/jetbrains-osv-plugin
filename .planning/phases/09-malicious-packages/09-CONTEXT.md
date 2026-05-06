# Phase 8: Malicious Package Detection

## Objective
Detect intentionally harmful packages (malware, typosquatting, protestware) in dependencies before they are installed.

## Why This Matters
- **Unique differentiator**: Neither Snyk nor Qodana offers this; only Mend.io does
- **High impact**: Prevents supply chain attacks at the source
- **Free tier viable**: Open source feeds exist (OpenSSF, Socket.dev)

## Features

### 8.1 OpenSSF Malicious Packages Feed
- Integrate https://github.com/ossf/malicious-packages
- Check package names against known malicious packages
- Severity: Always CRITICAL
- Detection at: dependency declaration level

### 8.2 Typosquatting Detection
- Compare package names against popular packages
- Flag packages with Levenshtein distance < 3 to known-popular packages
- Examples:
  - `lodash` vs `1odash` (one replaced with number)
  - `express` vs `expreess` (double letter)
  - `react` vs `reacct` (typo)

### 8.3 Pre-Commit Hooks
- Integrate with `com.intellij.openapi.vcs.checkin.CheckinHandlerFactory`
- Block commits if malicious packages detected
- Show warning dialog with:
  - Package name
  - Reason for flagging
  - Suggested alternatives
  - "Override" option for false positives

### 8.4 Real-Time Detection
- Flag malicious package names immediately when typed
- In pom.xml, build.gradle, package.json, requirements.txt
- Red underline + error icon

## Success Criteria
- [ ] Known malicious packages are flagged immediately
- [ ] Typosquatting candidates are warned
- [ ] Pre-commit hook blocks commits with malware
- [ ] False positive rate < 5%
- [ ] Cache for offline use

## Data Sources
1. OpenSSF malicious-packages repository (GitHub)
2. Socket.dev API (free tier available)
3. PyPI malware reports
4. npm security advisories (malicious category)

## Technical Notes
- Cache malicious package list locally (update daily)
- Use Bloom filter for fast lookup of 100K+ packages
- Typosquatting check against top 10K packages per ecosystem
- Async fetch so it doesn't block IDE
