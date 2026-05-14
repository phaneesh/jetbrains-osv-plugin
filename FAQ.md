# OSV Plugin FAQ & Troubleshooting

> Last updated for **v1.1.3**. For earlier versions, see [CHANGELOG](CHANGELOG.md).

---

## General

### Q: Which JetBrains IDEs are supported?

**All of them.** The plugin installs on IntelliJ IDEA, PyCharm, WebStorm, GoLand, PhpStorm, Rider, CLion, RubyMine, and DataGrip. Java-specific features (SAST, reachability analysis) are automatically skipped on non-Java IDEs.

### Q: The plugin says "No vulnerabilities found" but I know my dependency has a CVE. Why?

- The OSV database may not yet index that specific CVE (coverage is excellent but not exhaustive).
- Check that your lockfile is one of the [supported formats](README.md#supported-languages--formats).
- Verify the dependency is not in your Ignored Packages list (Settings → Tools → OSV Scanner).
- Try clearing the cache: results are cached for 60 minutes by default.

### Q: How do I suppress a false positive?

- Press **Alt+Enter** on the highlighted dependency and select **"Suppress for this dependency"**.
- Or add the package name to **Settings → Tools → OSV Scanner → Ignored Packages**.
- Adjust the **Minimum Severity** threshold to filter out LOW findings.

### Q: Do I need an API key?

**No.** The OSV API is free and open. GitHub Advisory enrichment uses unauthenticated requests (60/hour). Add a GitHub token in Settings to raise this to 5,000 requests/hour.

---

## Installation & Updates

### Q: How do I update the plugin?

JetBrains IDEs notify you automatically when an update is available. You can also check manually at **Settings → Plugins → Installed → OSV Vulnerability Scanner**.

### Q: Can I install from source?

Yes:

```bash
git clone https://github.com/dyuti/jetbrains-osv-plugin.git
cd jetbrains-osv-plugin
./gradlew buildPlugin
# Then: Settings → Plugins → ⚙️ → Install from Disk → build/distributions/*.zip
```

---

## Network & Proxy

### Q: My company uses an HTTP proxy. How do I configure it?

The plugin uses `java.net.http.HttpClient`, which respects standard JVM system properties:

```
-Dhttp.proxyHost=proxy.company.com
-Dhttp.proxyPort=8080
-Dhttps.proxyHost=proxy.company.com
-Dhttps.proxyPort=8080
```

Set these in **Help → Edit Custom VM Options...** and restart.

### Q: Can I use a self-hosted OSV API?

Yes. Go to **Settings → Tools → OSV Scanner** and update the **OSV API URL** field.

### Q: What about rate limiting?

The OSV public API does not enforce strict rate limits. GitHub Advisory (optional) is limited to 60/hour unauthenticated, 5,000/hour authenticated. The plugin uses OSV batch API to minimize requests.

---

## Inspections & UI

### Q: Why aren't inline highlights showing in my dependency file?

1. Ensure the inspection is enabled: **Settings → Editor → Inspections → Security → "OSV Vulnerability Check"**
2. Check your **Minimum Severity** — if set to MEDIUM, LOW findings are hidden
3. Verify the file type is supported (see [supported formats](README.md#supported-languages--formats))
4. The inspection runs asynchronously; wait a few seconds after opening the file

### Q: What is "Focus Mode"?

Focus Mode (Branch Comparison) shows only vulnerabilities that are **NEW** in your current branch compared to a configurable base branch (default: `main`). Useful for PR reviews.

### Q: What are the 6 tabs in the tool window?

| Tab                 | Purpose                                         |
| ------------------- | ----------------------------------------------- |
| **Vulnerabilities** | Tree view of all findings                       |
| **Trends**          | Historical scan data with charts and statistics |
| **SBOM**            | Export CycloneDX / SPDX                         |
| **CBOM**            | Cryptographic asset inventory                   |
| **QBOM**            | Post-quantum cryptography inventory             |
| **AIBOM**           | AI/ML asset inventory                           |

---

## BOM Exports

### Q: What's the difference between SBOM, CBOM, QBOM, and AIBOM?

| BOM       | Use Case                                                                  |
| --------- | ------------------------------------------------------------------------- |
| **SBOM**  | Compliance, customer disclosure, vulnerability management                 |
| **CBOM**  | Regulated environments requiring crypto inventory (e.g., NSA CNSA 2.0)    |
| **QBOM**  | Preparing for post-quantum migration; audit quantum-vulnerable algorithms |
| **AIBOM** | AI governance, model provenance, LLM supply-chain audit                   |

### Q: Can I export BOMs automatically in CI/CD?

The BOM panels are interactive (tree review + export). For automated CI/CD, use the **SARIF export** from the Vulnerabilities tab toolbar, or consider [OSV-Scanner CLI](https://google.github.io/osv-scanner/) for headless SBOM generation.

---

## Troubleshooting

### Q: Scan takes a long time on a large project?

- Increase **Cache TTL** to reduce API calls
- The plugin uses OSV batch API — all deps are sent in one request
- Results are cached locally; second scans are fast

### Q: "Access is allowed from write thread only" error?

Fixed in v1.1.1. If you see this, update to the latest plugin version.

### Q: Plugin crashes on startup with `ClassNotFoundError`?

Fixed in v1.1.3. The plugin now detects Java PSI availability at runtime and skips Java-only features on non-Java IDEs.

### Q: Notifications not appearing?

Check **Settings → Appearance & Behavior → Notifications → OSV Scanner** — ensure the notification group is enabled.

---

## Known Limitations

- **Gradle version catalogs** (`libs.versions.toml`) are not yet supported for dependency parsing.
- **Line-level problem markers** in the Problems tool window are a planned enhancement (currently only file-level).
- **Vulnerable API detection** requires the dependency source JAR to be attached to the project.
- **Reachability analysis** is basic (method signature matching); does not trace data flow.

---

## Still Stuck?

- [GitHub Issues](https://github.com/dyuti/jetbrains-osv-plugin/issues)
- [Plugin Documentation](PLUGIN_DOCUMENTATION.md)
- [Getting Started Guide](GETTING_STARTED.md)
