# OSV Plugin FAQ & Troubleshooting

## General

### Q: The plugin says "No vulnerabilities found" but I know my dependency has a CVE. Why?
- The OSV database may not index that specific CVE yet (coverage is excellent but not exhaustive).
- Check that your dependency file is one of the supported formats: `pom.xml`, `build.gradle`, `build.gradle.kts`, `package-lock.json`, `requirements.txt`.
- Verify the dependency is not in your Ignored Packages list (Settings → OSV Scanner).
- Try clearing the cache: the plugin caches results for 1 hour by default.

### Q: How do I suppress a false positive?
- Use the Alt+Enter quick-fix on the highlighted dependency and select "Suppress for this dependency".
- Or add the package name to Settings → OSV Scanner → Ignored Packages.
- Adjust the Minimum Severity threshold if the finding is LOW and you want to hide it.

## Proxy & Network

### Q: My company uses an HTTP proxy. How do I configure the plugin?
The plugin uses `java.net.http.HttpClient`, which respects standard JVM system properties:
```
-Dhttp.proxyHost=proxy.company.com
-Dhttp.proxyPort=8080
-Dhttps.proxyHost=proxy.company.com
-Dhttps.proxyPort=8080
```
Set these in **Help → Edit Custom VM Options...** and restart IntelliJ IDEA.

### Q: Can I use a self-hosted OSV API?
Yes. Go to **Settings → Tools → OSV Scanner** and change the "OSV API URL" field.

## Rate Limiting

### Q: Is there a rate limit?
The OSV public API does not enforce strict rate limits for open queries. If you enable GitHub Advisory integration, the unauthenticated rate limit is 60 requests/hour. Add a GitHub token in **Settings → OSV Scanner** to raise this to 5000 requests/hour.

## Inspections & UI

### Q: Why aren't inline highlights showing in my dependency file?
1. Ensure the inspection is enabled: **Settings → Editor → Inspections → Security → "OSV Vulnerability Check"**
2. Check your Minimum Severity threshold — if set to MEDIUM, LOW findings are hidden
3. Verify the file type is supported (`pom.xml`, `build.gradle`, etc.)
4. The inspection runs asynchronously; wait a few seconds after opening the file

### Q: What is "Focus Mode"?
Focus Mode (Branch Comparison) shows only vulnerabilities that are NEW in your current branch compared to a configurable base branch (default: `main`).

## Known Limitations

- **Gradle version catalogs** (`libs.versions.toml`) are not yet supported for dependency parsing.
- **Historical trends** use ASCII sparklines; a proper chart library is planned for a future release.
- **Line-level problem markers** (exact-line navigation in the Problems tool window) are a planned enhancement.
- **Vulnerable API detection** requires the dependency source JAR to be attached to the project.
