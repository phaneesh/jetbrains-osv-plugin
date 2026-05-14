# Marketplace Screenshots

These screenshots are used for the JetBrains Marketplace listing.

## How to Capture

1. Build and run the plugin: `./gradlew runIde`
2. Open or create the test project in `test-projects/vulnerable-sample/`
   (create this directory with a `pom.xml` containing log4j-core:2.14.0, spring-core:5.3.20)
3. Wait for the scan to complete

### Required Screenshots

**1. Tool Window (1280x800 recommended)**

- Show the OSV Vulnerability Scanner tool window at the bottom
- Severity groups (Critical, High) should be visible
- Tree should show at least 3 vulnerabilities
- Save as: `screenshots/01-tool-window.png`

**2. Inline Inspection Highlight (800x600 recommended)**

- Open `pom.xml` in the editor
- Show the yellow/orange/red underline on a vulnerable dependency line
- Tooltip should be visible (hover over the highlight)
- Save as: `screenshots/02-inline-highlight.png`

**3. Quick-Fix Popup (800x600 recommended)**

- Position cursor on a highlighted dependency line
- Press Alt+Enter
- Show the "Upgrade to fixed version" quick-fix option
- Save as: `screenshots/03-quick-fix.png`

## Notes

- Use the default IntelliJ light theme for consistency
- Ensure no personal/sensitive data is visible in the screenshot
- Annotate with simple arrows/circles if helpful (optional)
