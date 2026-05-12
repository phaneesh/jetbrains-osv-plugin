# Phase 21 Research: Final Polish, Iconography, and Marketplace Release

> **Purpose:** What you need to know to PLAN this phase well. This research document maps every decision from 21-CONTEXT.md to concrete implementation patterns, existing code locations, and technical gotchas.

---

## 1. Iconography & Visual Polish

### 1.1 Dark Mode Support via JBColor

**Current state:** `SeverityUtil.kt` uses hardcoded AWT `Color` objects:

```kotlin
// src/main/java/io/dyuti/osvplugin/utils/SeverityUtil.kt
fun getColor(severity: OsVSeverity): Color =
    when (severity) {
        OsVSeverity.CRITICAL -> Color(220, 53, 69)
        ...
    }
```

**Also in:** `OsVToolWindowPanel.kt` uses `Color.GRAY` for `statusLabel.foreground`.

**How to fix:** Replace `java.awt.Color` with `com.intellij.ui.JBColor` which auto-adapts to Darcula/light themes. Pattern already used in:

- `LicensePolicyConfigurable.kt` (uses `JBColor.BLUE`)
- `SummaryButton.kt` (uses `JBColor.GRAY`)
- `SummaryUiModel.kt` (uses `JBColor?` param)

```kotlin
// Target pattern
import com.intellij.ui.JBColor
fun getColor(severity: OsVSeverity): JBColor =
    when (severity) {
        OsVSeverity.CRITICAL -> JBColor(Color(220, 53, 69), Color(255, 100, 100))
        // light, dark pair
    }
```

### 1.2 Severity Icons — Use IntelliJ Built-ins

**Current state:** Already using `AllIcons` in `SeverityUtil.kt`:

```kotlin
fun getSeverityIcon(severity: OsVSeverity): Icon =
    when (severity) {
        OsVSeverity.CRITICAL -> AllIcons.Ide.FatalError
        OsVSeverity.HIGH -> AllIcons.General.Error
        OsVSeverity.MEDIUM -> AllIcons.General.Warning
        OsVSeverity.LOW -> AllIcons.General.Information
    }
```

This is already correct per Decision D-01. **No change needed** in `SeverityUtil.getSeverityIcon()`. Confirm this pattern is used consistently across:

- `SeverityTreeCellRenderer` (`OsVToolWindowPanel.kt` lines 560-570)
- Notification icons via `NotificationType` (see 1.5)

### 1.3 Animated Scanning Indicator

**Current state:** Static "Scanning dependencies..." text in `statusLabel` (see `performScan()` in `OsVToolWindowPanel.kt`).

**Approach:** Use `com.intellij.ui.AnimatedIcon` or `com.intellij.openapi.progress.util.ProgressIndicatorBase` for a spinning icon.

Options:

- `AnimatedIcon.Default()` — standard IntelliJ spinning icon
- `AnimatedIcon.Blinking(AllIcons.Actions.Refresh)` — blinking refresh
- Custom `AnimatedIcon` with frames if you want a custom animation

Implementation location: Add to `setupUI()` in `OsVToolWindowPanel.kt`, swap `statusLabel` icon between static and animated during `performScan()`.

### 1.4 Toolbar Actions via ActionManager

**Current state:** `plugin.xml` has an empty `<actions>` block. The tool window only has a "Scan Dependencies" Swing `JButton` inside the panel.

**Approach:** Register `AnAction` subclasses for scan/clear/export and add them to the tool window toolbar:

```xml
<!-- In plugin.xml -->
<actions>
    <action id="osv.scan" class="io.dyuti.osvplugin.action.ScanAction"
            text="Scan Dependencies" icon="AllIcons.Actions.Refresh">
        <add-to-group group-id="ToolbarRunGroup" anchor="after" relative-to-action="RunClass"/>
    </action>
</actions>
```

Or, more idiomatically for tool windows: Add actions to `toolWindow.setTitleActions()` or `toolWindow.contentManager.addContent()` with action toolbar.

### 1.5 Severity-Colored Notifications

**Current state:** `NotificationService.kt` already maps severity to `NotificationType`:

```kotlin
private fun mapSeverityToNotificationType(severity: OsVSeverity): NotificationType =
    when (severity) {
        OsVSeverity.CRITICAL -> NotificationType.ERROR
        OsVSeverity.HIGH -> NotificationType.WARNING
        OsVSeverity.MEDIUM -> NotificationType.WARNING
        OsVSeverity.LOW -> NotificationType.INFORMATION
    }
```

This is already correct per Decision D-05. **No change needed.**

### 1.6 Status Bar Widget

**Current state:** `updateStatus()` already writes to `WindowManager.getStatusBar(project).setInfo()`. Decision D-06 wants a dedicated status bar widget with persistent scan status and vulnerability count.

**Approach:** Implement `com.intellij.openapi.wm.StatusBarWidget` and register in `plugin.xml`:

```xml
<statusBarWidgetFactory
    implementation="io.dyuti.osvplugin.statusbar.OsvStatusBarWidgetFactory"
    id="osv.statusBar"/>
```

The factory creates a widget showing: "🛡️ 3 Critical, 5 High" or "✅ Clean"

---

## 2. Known Issue Resolution

### 2.1 CacheManager Singleton Bug (CRITICAL)

**Current state:** `CacheManager.kt` line 46:

```kotlin
companion object {
    fun getInstance(): CacheManager = CacheManager()  // BUG: New instance!
}
```

**Impact:** Every caller gets a fresh empty cache. The `@State` annotation on `CacheManager` suggests it was meant to be a `PersistentStateComponent`, but the companion object bypasses the platform's service registry.

**How to fix:** CacheManager is annotated `@State` (`PersistentStateComponent<CacheState>`) but is NOT registered as an `applicationService` in `plugin.xml`. Two paths:

**Path A (preferred):** Register in `plugin.xml` and use `ApplicationManager.getService()`:

```xml
<applicationService serviceImplementation="io.dyuti.osvplugin.utils.CacheManager"/>
```

```kotlin
companion object {
    fun getInstance(): CacheManager =
        ApplicationManager.getApplication().getService(CacheManager::class.java)
}
```

**Path B (quick fix):** Make the companion object hold a true singleton:

```kotlin
companion object {
    private val INSTANCE by lazy { CacheManager() }
    fun getInstance(): CacheManager = INSTANCE
}
```

**Recommendation:** Use Path A. CacheManager is already a `PersistentStateComponent` with `@State(name="OsVCacheManager", storages=[Storage("osv-cache.xml")])`. The platform expects it to be a registered service. The `getState()`/`loadState()` methods currently only save timestamps (not full entries) — that is acceptable for v1.

**Cascading fix required:** `OsVApiService.getInstance()` also returns a new instance. It should similarly be registered as an `applicationService` in `plugin.xml` or converted to a true companion singleton.

### 2.2 Configurable OSV API URL

**Current state:** `OsVApiService.kt` line 42:

```kotlin
private val osvApiUrl = "https://api.osv.dev/v1/query"
```

`OsVConfig.kt` does not have an `osvApiUrl` field.

**How to fix:**

1. Add `var osvApiUrl: String = "https://api.osv.dev/v1/query"` to `OsVConfig.kt`
2. Add UI field in `OsVConfigurable.kt` settings panel
3. Reference `config.osvApiUrl` in `OsVApiService.kt` instead of hardcoded string

Note: The lazy config fallback handles unit-test safety.

### 2.3 Exception Swallowing → Logger

**Current state:** At least 7 `System.err.println()` calls across the codebase:

| File                    | Line | Message                                          |
| ----------------------- | ---- | ------------------------------------------------ |
| `OsVApiService.kt`      | 425  | `Error querying dependency: $err`                |
| `LicenseInspection.kt`  | 74   | `Error scanning licenses for $fileName`          |
| `SarifExporter.kt`      | 36   | `Error exporting SARIF`                          |
| `OsVInspection.kt`      | 198  | `OSV: Error scanning file $fileName`             |
| `OsVToolWindowPanel.kt` | 495  | `Error parsing dependencies from $fileName`      |
| `OsVToolWindowPanel.kt` | 523  | `Error querying vulnerabilities for ${dep.name}` |

**How to fix:** Use IntelliJ's `com.intellij.openapi.diagnostic.Logger`:

```kotlin
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<OsVApiService>()
// or
private val LOG = Logger.getInstance(OsVApiService::class.java)

LOG.error("Batch query error", exception)
LOG.warn("Rate limit approaching")
```

**For classes that need to run without IntelliJ platform (tests):** Keep the `System.err` fallback or use `thisLogger()` which is safe. `OsVApiService` already has a lazy config that catches `Exception` for test safety.

### 2.4 Rate Limit Counters — Move to Application Scope

**Current state:** `OsVApiService.kt` lines 50-51:

```kotlin
private var requestsThisHour = 0
private var rateLimitWindowStart = System.currentTimeMillis()
```

These are instance fields. If `getInstance()` creates a new instance (which it does), rate limits reset every time.

**How to fix:** Move counters to a dedicated application-level service or use `ApplicationManager.getApplication().getUserData()` / `putUserData()`:

```kotlin
private val RATE_LIMIT_KEY = Key.create<AtomicInteger>("osv.rateLimit.count")
private val RATE_LIMIT_WINDOW_KEY = Key.create<Long>("osv.rateLimit.window")
```

Or simpler: Because rate limiting is per `OsVApiService` instance, just ensure `OsVApiService` is a true singleton (see 2.1). Then the instance fields work correctly.

### 2.5 Encrypt Sensitive Tokens via PasswordSafe

**Current state:** `OsVConfig.kt` stores tokens in plain XML:

```kotlin
var githubToken: String? = null
var jiraToken: String? = null
var privacySalt: String? = null
```

**How to fix:** Use `com.intellij.ide.passwordSafe.PasswordSafe`:

```kotlin
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.components.service

class OsVConfig : PersistentStateComponent<OsVConfig> {
    // Store sensitive data in PasswordSafe, not in XML state
    companion object {
        private const val GITHUB_TOKEN_KEY = "osv.github.token"
        private const val JIRA_TOKEN_KEY = "osv.jira.token"

        fun getGithubToken(): String? =
            PasswordSafe.instance.getPassword(Credentials(GITHUB_TOKEN_KEY))

        fun setGithubToken(token: String?) {
            PasswordSafe.instance.setPassword(Credentials(GITHUB_TOKEN_KEY, token))
        }
    }
}
```

**Important:** `PasswordSafe` requires `com.intellij.modules.platform` dependency (already declared in `plugin.xml`). For unit tests outside IntelliJ, use null-fallback.

**Note:** `OsVConfig` stores `jiraBaseUrl`, `jiraEmail`, `jiraProjectKey`, `githubToken`, `jiraToken`. Only tokens need encryption; URLs and usernames can stay in `PersistentStateComponent`.

### 2.6 Regex Parser Coverage Extension

**Current state:** `GradleParser.kt` only handles `implementation("group:artifact:version")` syntax. It does NOT handle:

- `platform()` / `enforcedPlatform()` dependencies
- Gradle version catalogs (`libs.versions.toml`)
- `api()`, `testImplementation()`, `compileOnly()` etc. (may already work — verify)

**Decision D-09:** Document known limitations rather than full rewrite.

**How to address:**

1. Add regex for additional Gradle configurations (`api`, `testImplementation`, `compileOnly`, `runtimeOnly`, etc.)
2. Add regex for `platform()`/`enforcedPlatform()` wrapper syntax
3. Document `libs.versions.toml` as unsupported in documentation
4. (Optional) Add a `PARSING_LIMITATIONS.md` or section in `PLUGIN_DOCUMENTATION.md`

### 2.7 Populate `vulnerability.packageName`

**Current state:** `Vulnerability.kt` has `val packageName: String = ""` (default empty). Decision D-10 says populate from query context in `parseVulnerability()`.

**How to fix:** `parseVulnerability()` only receives a `JsonObject` (the OSV response). The package name is in the _query_ context (the `packageName` parameter to `queryVulnerabilities()` or `Dependency.name` in batch queries), not in the response JSON.

Fix: Pass the package name down through the call chain:

1. `queryVulnerabilities(packageName, ecosystem, version)` → `executeRequest(...)` → `parseVulnerabilities(response, packageName)`
2. `parseVulnerability(vuln, packageName)` → set `packageName = packageName`
3. `batchQueryVulnerabilities()` → include `dep.name` when calling `parseVulnerabilities()`

### 2.8 PSI-Level Problem Registration (file-level enhancement)

**Current state:** `OsVInspection.kt` registers problems at the file level:

```kotlin
holder.registerProblem(file, message, highlightType, upgradeFix, suppressFix, ignoreFix)
```

Decision D-16 wants file-level problem markers via `ProblemDescriptor` at dependency text offset.

**How to fix:** Use `ProblemDescriptorBase` or `InspectionManager.createProblemDescriptor()` with a `TextRange`:

```kotlin
val offset = findDependencyOffset(file, dep)
val range = TextRange(offset, offset + dep.name.length)
holder.registerProblem(
    file,
    message,
    highlightType,
    range,
    upgradeFix, suppressFix, ignoreFix
)
```

Requires finding the exact text offset of the dependency declaration in the file. Parsers already calculate `lineNumber` — extend them to also calculate `startOffset`.

---

## 3. Marketplace Packaging

### 3.1 Plugin Manifest Polish

**Current `plugin.xml` issues to fix:**

1. **Description is minimal:**

   ```xml
   <description><![CDATA[
   A free, open-source IntelliJ IDEA plugin that provides security vulnerability scanning for open-source dependencies using the OSV database.
   ]]></description>
   ```

   Should be expanded with feature bullet list for marketplace appeal.

2. **Change notes in `plugin.xml` vs `build.gradle.kts`:** `patchPluginXml` in `build.gradle.kts` overrides the change notes. Keep the source of truth in `build.gradle.kts`.

3. **Missing `<idea-version>` attributes:** These come from `patchPluginXml` in `build.gradle.kts` (`sinceBuild`, `untilBuild`). Currently:
   - `sinceBuild="233.0"` (2023.3)
   - `untilBuild="262.*"` (2026.1)

4. **No `<category>` element:** JetBrains Marketplace uses categories. Add:

   ```xml
   <category>Security</category>
   ```

5. **No `<tags>`:** Marketplace supports tags via the web UI, not plugin.xml. Set during upload.

### 3.2 Version Compatibility Verification

**Current claim:** "Updated compatibility to support IntelliJ IDEA 2026.1.x" in change notes.

**Reality check:**

- `build.gradle.kts`: `intellij { version.set("2023.3") }` — the plugin is built against 2023.3 APIs
- `sinceBuild="233.0"` — requires 2023.3+
- `untilBuild="262.*"` — claims compatibility up to 2026.1

**Risk:** The `untilBuild` range is extremely wide. If the plugin uses APIs that were deprecated/removed in 2024.x, 2025.x, or 2026.x, it will crash at runtime.

**How to verify:**

1. Run `./gradlew runIde` with `version.set("2024.1")` to test
2. Run `./gradlew verifyPlugin` (already in CI)
3. Consider narrowing `untilBuild` to `"243.*"` (2024.3) initially and expanding after verification
4. Use `platformPlugins` for `com.intellij.java` and `org.jetbrains.kotlin` (already configured)

### 3.3 Plugin Signing

**Current state:** No signing configuration in `build.gradle.kts`.

**What is needed:** JetBrains Marketplace requires plugins to be signed using the JetBrains Plugin Signing Service.

**Approach:**

1. Generate a certificate via JetBrains Marketplace (Settings → Certificates)
2. Store the private key and certificate chain as repository secrets (`SIGN_PRIVATE_KEY`, `SIGN_CERTIFICATE_CHAIN`)
3. Configure the IntelliJ Gradle Plugin signing:

```kotlin
// build.gradle.kts
tasks.signPlugin {
    certificateChain.set(System.getenv("SIGN_CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("SIGN_PRIVATE_KEY"))
    password.set(System.getenv("SIGN_PRIVATE_KEY_PASSWORD"))
}
```

**Note:** Plugin signing is free and mandatory for JetBrains Marketplace distribution as of 2024.

### 3.4 Marketplace Icon

**Current state:** Icons exist at 16, 24, 32, 48, 64px in `src/main/resources/icons/`.

**Missing:** 128x128 PNG for marketplace listing page.

**Decision D-22:** Create 128x128 icon. Can be generated by upscaling the existing design or creating a dedicated high-res version.

### 3.5 Screenshots

**Decision D-19:** Generate 3 annotated screenshots:

1. Tool window showing vulnerability tree with severity groups
2. Inline inspection highlight in a dependency file (pom.xml/build.gradle)
3. Quick-fix popup (Alt+Enter)

**Where to capture:** Run `./gradlew runIde`, open a test project with known vulnerable dependencies, and use OS built-in screenshot tools.

**Test project idea:** A simple Maven project with `log4j-core:2.14.0`, `spring-core:5.3.20`, and `lodash:4.17.20`.

### 3.6 Marketplace Tags

**Decision D-20:** Tags to set during marketplace upload:

- "security"
- "dependencies"
- "vulnerability scanning"
- "SAST"
- "SCA" (Software Composition Analysis)
- "open source"

These are set in the JetBrains Marketplace web UI, not in plugin.xml.

---

## 4. Testing & Quality Gates

### 4.1 Mock HTTP Responses (MockWebServer)

**Current state:** `OsVApiServiceTest.kt` makes live API calls:

```kotlin
@Test
fun `queryVulnerabilities returns empty list for unknown package`() {
    val result = apiService.queryVulnerabilities("test-package", "Maven", "1.0.0")
    // Hits real OSV API!
}
```

**Decision D-29:** Add `MockWebServer` dependency.

**Approach:**

```kotlin
// build.gradle.kts
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
```

**Test pattern:**

```kotlin
class OsVApiServiceTest {
    private val mockWebServer = MockWebServer()
    private lateinit var apiService: OsVApiService

    @BeforeEach
    fun setup() {
        mockWebServer.start()
        val httpClient = HttpClient.newBuilder().build()
        apiService = OsVApiService(
            httpClient = httpClient,
            baseUrl = mockWebServer.url("/").toString()
        )
    }

    @Test
    fun `queryVulnerabilities returns parsed vulnerabilities`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"vulns": [{"id": "GHSA-xxx", ... }]}""")
        )
        val result = apiService.queryVulnerabilities("test", "Maven", "1.0.0")
        assertEquals(1, result.size)
    }
}
```

**Note:** `OsVApiService` constructor currently accepts `httpClient: HttpClient?` but not a base URL. You'll need to either:

- Make `osvApiUrl` a constructor parameter with the hardcoded URL as default
- Or add a test-only setter

### 4.2 Integration Tests via HeavyPlatformTestCase

**Decision D-25:** Add integration tests for PSI inspection + tool window pipeline.

**Approach:** Use `com.intellij.testFramework.HeavyPlatformTestCase` or `BasePlatformTestCase`:

```kotlin
class OsVInspectionIntegrationTest : HeavyPlatformTestCase() {
    fun testInspectionFindsVulnerabilitiesInPomXml() {
        // Create a pom.xml with a vulnerable dependency
        val pomFile = myFixture.configureByText("pom.xml", """
            <dependencies>
                <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-core</artifactId>
                    <version>5.3.20</version>
                </dependency>
            </dependencies>
        """.trimIndent())

        // Run inspection
        val inspection = OsVInspection()
        myFixture.enableInspections(inspection)
        val highlights = myFixture.doHighlighting()

        // Assert highlights exist
        assertTrue(highlights.isNotEmpty())
    }
}
```

**Test framework setup:** Requires adding `testImplementation` dependency on IntelliJ platform test framework. The `intellij` Gradle plugin should handle this automatically via:

```kotlin
intellij {
    version.set("2023.3")
    // test framework is included
}
```

### 4.3 Thread Safety Tests

**Decision D-30:** Add tests for concurrent cache access and semaphore stress.

**What to test:**

1. **CacheManager concurrency:** Multiple threads calling `cacheVulnerabilities()` and `getCachedVulnerabilities()` simultaneously
2. **Batch query semaphore:** Verify max 10 concurrent requests via `CountDownLatch` or thread counter
3. **Rate limit thread safety:** Verify atomic increment under concurrent access

**Pattern:**

```kotlin
@Test
fun `cacheManager handles concurrent access`() {
    val cacheManager = CacheManager()
    val threads = (1..20).map {
        Thread {
            cacheManager.cacheVulnerabilities("key$it", emptyList())
            cacheManager.getCachedVulnerabilities("key$it")
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
    // Assert no ConcurrentModificationException, correct size
}
```

**Note:** With the singleton fix, `CacheManager` will be a platform service. Tests may need to use the test framework's `ServiceContainerUtil` to replace it with a test instance.

### 4.4 Performance Benchmark

**Decision D-27:** Add performance benchmark for large dependency lists.

**Approach:** Simple JUnit-based benchmark (no JMH needed for first pass):

```kotlin
@Test
fun `batchQuery completes within time limit for 100 dependencies`() {
    val dependencies = (1..100).map {
        Dependency("test-pkg-$it", "1.0.0", "Maven", "compile", false)
    }

    val start = System.currentTimeMillis()
    apiService.batchQueryVulnerabilities(dependencies)
    val elapsed = System.currentTimeMillis() - start

    assertTrue(elapsed < 30000, "Expected < 30s, took ${elapsed}ms")
}
```

Use `MockWebServer` with artificial delays to simulate realistic network conditions.

### 4.5 UI Regression Tests

**Decision D-31:** Use `PlatformTestUtil.assertTreeStructure()` and light UI tests.

**Approach:** Test the tree model building logic separately from Swing:

```kotlin
@Test
fun `treeModel groups vulnerabilities by severity`() {
    val builder = OsVTreeModelBuilder()
    val vulns = mapOf(
        virtualFile to listOf(
            Vulnerability(id="CVE-1", severity=CRITICAL, ...),
            Vulnerability(id="CVE-2", severity=HIGH, ...)
        )
    )
    builder.buildModel(vulns)

    val root = builder.getTreeModel().root as DefaultMutableTreeNode
    assertEquals(1, root.childCount) // 1 module
    val moduleNode = root.getChildAt(0) as ModuleTreeNode
    assertEquals(2, moduleNode.childCount) // Critical + High groups
}
```

The `OsVTreeModelBuilder` is already pure logic (no platform calls), making it unit-testable.

### 4.6 Compatibility Matrix

**Decision D-28:** Test against 2023.3 (minimum) and 2024.1+.

**Approach:** Use GitHub Actions matrix:

```yaml
strategy:
  matrix:
    intellij-version: ["2023.3", "2024.1", "2024.2"]
```

Set `intellij.version.set(matrix.intellij-version)` dynamically in Gradle.

---

## 5. Documentation & Onboarding

### 5.1 README.md Update

**Current state:** README lists v1.0.0 features only. Missing:

- Phases 6-20 features (vulnerable API detection, malicious packages, SAST, privacy, risk scoring, policy, team config, diff analysis, historical trends, SBOM, config audit, notifications)
- Accurate screenshots
- Feature matrix
- Updated installation instructions for marketplace

### 5.2 CHANGELOG.md

**Decision D-33:** Create CHANGELOG.md for versions 1.0.0 and 1.1.0.

**Suggested format (Keep a Changelog):**

```markdown
## [1.1.0] - 2024-XX-XX

### Added

- Vulnerable API Detection (Phase 8)
- Malicious Package Detection (Phase 9)
- Basic SAST / Taint Analysis (Phase 10)
- Privacy-Preserving Queries (Phase 11)
- Risk Scoring with EPSS + CISA KEV (Phase 13)
- Policy Enforcement (Phase 14)
- Team Collaboration & Config Sharing (Phase 15)
- Differential Analysis (Phase 16)
- Historical Trending (Phase 17)
- SBOM Generation (Phase 18)
- Configuration Audit (Phase 19)
- IDE Notification Service (Phase 20)

### Fixed

- CacheManager singleton bug
- Hardcoded OSV API URL
- Exception swallowing replaced with proper logging
- Rate limiting per-instance bug
- Token encryption via PasswordSafe
```

### 5.3 CONTRIBUTING.md

**Decision D-36:** Add contributor guidelines and PR template.

**Content needs:**

- Code style (Kotlin conventions, 4-space indent)
- Test requirements (all tests must pass, coverage targets)
- Issue/PR templates
- Development setup (JDK 17, `./gradlew runIde`)
- Branch naming conventions

### 5.4 FAQ / Troubleshooting

**Decision D-37:** Document proxy, rate limit, and false positive handling.

**Topics to cover:**

1. **Proxy configuration:** The plugin uses `java.net.http.HttpClient` — respects JVM system properties (`http.proxyHost`, `http.proxyPort`). Document this.
2. **Rate limiting:** OSV API has no rate limits for open queries, but GitHub Advisory does (60/hr unauthenticated). Document how to configure tokens.
3. **False positives:** How to suppress, ignore packages, or adjust severity threshold.
4. **No vulnerabilities found:** Check network, file format support, cache clearing.
5. **Plugin not showing highlights:** Check inspection is enabled in Settings → Editor → Inspections → Security → OSV Vulnerability Check.

---

## 6. Build & Distribution

### 6.1 Shadow JAR Audit

**Current state:** Shadow JAR bundles `gson`, `maven-model`, `maven-model-builder`.

```kotlin
tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("com.google.code.gson:gson"))
        include(dependency("org.apache.maven:maven-model"))
        include(dependency("org.apache.maven:maven-model-builder"))
    }
    minimize {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:.*"))
    }
}
```

**Decision D-38:** Audit for unused transitive deps.

**How to audit:**

1. Run `./gradlew dependencies --configuration runtimeClasspath`
2. Check which transitive deps come with `maven-model` and `maven-model-builder`
3. Use `minimize()` to strip unused classes (already configured)
4. Verify with `./gradlew buildPlugin` and inspect the ZIP

**Known risk (CONCERNS.md #12):** Minimize exclusions for `org.jetbrains.kotlin:.*` and `org.jetbrains:.*` may not preserve all required classes. Test by running the plugin in a clean IDE (`./gradlew runIde`).

### 6.2 Multi-Version Builds

**Decision D-40:** Configure builds against IntelliJ platform 233 and 241+.

**Approach:** Use Gradle property:

```bash
./gradlew buildPlugin -PintellijVersion=2024.1
```

In `build.gradle.kts`:

```kotlin
intellij {
    version.set(project.findProperty("intellijVersion")?.toString() ?: "2023.3")
}
```

### 6.3 Artifact Naming

**Decision D-42:** Ensure `osv-plugin-{version}.zip` naming.

**Current state:** The IntelliJ Gradle Plugin generates `osv-plugin-1.1.0.zip` automatically from `group` + `version`.

Verify in `build/distributions/` after `./gradlew buildPlugin`.

### 6.4 SNAPSHOT vs Release

**Decision D-43:** Use `-SNAPSHOT` suffix for dev builds; strip for release.

**Current state:** `version = "1.1.0"` in `build.gradle.kts`.

**Approach:** Use `gradle.properties`:

```properties
pluginVersion=1.1.0-SNAPSHOT
```

```kotlin
// build.gradle.kts
version = project.findProperty("pluginVersion") ?: "1.1.0-SNAPSHOT"
```

Release CI strips `-SNAPSHOT`:

```bash
VERSION=${GITHUB_REF#refs/tags/v}  # v1.1.0 -> 1.1.0
./gradlew buildPlugin -PpluginVersion=$VERSION
```

### 6.5 GitHub Actions — Marketplace Publish

**Decision D-44:** Add automated build → sign → publish to JetBrains Marketplace.

**Current CI:** `.github/workflows/build.yml` builds and creates GitHub releases. It does NOT publish to JetBrains Marketplace.

**What to add:**

```yaml
# In .github/workflows/build.yml or new publish.yml
publish:
  needs: [build, test, verify]
  if: startsWith(github.ref, 'refs/tags/v')
  steps:
    - name: Publish to JetBrains Marketplace
      env:
        JETBRAINS_TOKEN: ${{ secrets.JETBRAINS_TOKEN }}
      run: ./gradlew publishPlugin -PintellijPublishToken=$JETBRAINS_TOKEN
```

**Prerequisites:**

1. Create JetBrains Hub account: https://hub.jetbrains.com/
2. Create plugin listing: https://plugins.jetbrains.com/author/me
3. Generate token: https://plugins.jetbrains.com/author/me/tokens
4. Add `JETBRAINS_TOKEN` to GitHub repository secrets

**Note:** The `publishPlugin` task from the IntelliJ Gradle Plugin handles uploading, verification, and publishing. It requires the plugin ZIP (from `buildPlugin`) and the token.

### 6.6 Verify Gradle Plugin Compatibility

**Decision D-41:** Verify `org.jetbrains.intellij` v1.17.0 with Gradle 8.x

**Current state:** `kotlin("jvm") version "1.9.20"`, `org.jetbrains.intellij` version "1.17.0", Gradle wrapper 8.x.

**Compatibility matrix:**
| IntelliJ Gradle Plugin | Gradle | IntelliJ Platform |
|------------------------|--------|-------------------|
| 1.17.0 | 8.0-8.5 | 2022.2+ |
| 1.17.4 | 8.0-8.6 | 2022.2+ |
| 2.x | 8.5+ | 2023.3+ |

**Note:** Consider upgrading to `org.jetbrains.intellij` v1.17.4 for bug fixes, or v2.x for latest features (requires Gradle 8.5+ and API changes). For a release phase, **stay on 1.17.x** to avoid build system churn.

---

## 7. Mapping Decisions to Files

| Decision                           | File(s)                                                  | Type of Change                       |
| ---------------------------------- | -------------------------------------------------------- | ------------------------------------ |
| D-01 (severity icons)              | `SeverityUtil.kt`                                        | None needed — already correct        |
| D-02 (dark mode)                   | `SeverityUtil.kt`, `OsVToolWindowPanel.kt`               | Replace Color with JBColor           |
| D-03 (animated scan)               | `OsVToolWindowPanel.kt`                                  | Add AnimatedIcon                     |
| D-04 (toolbar actions)             | `plugin.xml`, new action classes                         | Add AnAction subclasses              |
| D-05 (severity notifications)      | `NotificationService.kt`                                 | None needed — already correct        |
| D-06 (status bar widget)           | New file + `plugin.xml`                                  | Add StatusBarWidgetFactory           |
| D-07 (CacheManager singleton)      | `CacheManager.kt`, `plugin.xml`                          | Register as applicationService       |
| D-08 (configurable API URL)        | `OsVConfig.kt`, `OsVConfigurable.kt`, `OsVApiService.kt` | Add field, UI, consume field         |
| D-09 (parser coverage)             | `GradleParser.kt`, docs                                  | Extend regex, document limits        |
| D-10 (packageName population)      | `OsVApiService.kt`, `Vulnerability.kt`                   | Thread package name through parse    |
| D-11 (exception logging)           | 7+ files across codebase                                 | Replace System.err with Logger       |
| D-12 (rate limit global)           | `OsVApiService.kt`                                       | Make singleton + atomic counters     |
| D-13 (PasswordSafe tokens)         | `OsVConfig.kt`, `JiraConnector.kt`                       | Encrypt jiraToken, githubToken       |
| D-14 (System.err → Logger)         | Same as D-11                                             | Replace System.err with Logger       |
| D-15 (sparkline docs)              | `HistoricalTrendPanel.kt`, docs                          | Document as known limitation         |
| D-16 (PSI problem markers)         | `OsVInspection.kt`, parsers                              | Add text offset to dependency model  |
| D-17 (plugin.xml polish)           | `build.gradle.kts`, `plugin.xml`                         | Expand description, change notes     |
| D-18 (version compatibility)       | `build.gradle.kts`                                       | Verify/narrow untilBuild             |
| D-19 (screenshots)                 | `screenshots/` dir                                       | Manual capture with test project     |
| D-20 (marketplace tags)            | JetBrains web UI                                         | Set during upload                    |
| D-21 (plugin signing)              | `build.gradle.kts`, GitHub secrets                       | Add signPlugin config + secrets      |
| D-22 (128x128 icon)                | `src/main/resources/icons/`                              | Add osv-icon-128.png                 |
| D-23 (vendor info)                 | `plugin.xml`                                             | Update vendor element                |
| D-24 (auto-update)                 | n/a                                                      | Use marketplace built-in             |
| D-25 (integration tests)           | `src/test/...`                                           | Add HeavyPlatformTestCase tests      |
| D-26 (TESTING-CHECKLIST.md)        | New file                                                 | Manual smoke test steps              |
| D-27 (performance benchmark)       | `src/test/...`                                           | Add timed test with MockWebServer    |
| D-28 (compatibility matrix)        | `.github/workflows/build.yml`                            | Add matrix builds                    |
| D-29 (MockWebServer)               | `build.gradle.kts`, `OsVApiServiceTest.kt`               | Add dependency + mock tests          |
| D-30 (thread safety tests)         | `src/test/...`                                           | Add concurrent test patterns         |
| D-31 (UI regression)               | `src/test/...`                                           | Add tree model assertions            |
| D-32 (README update)               | `README.md`                                              | Expand features, screenshots         |
| D-33 (CHANGELOG.md)                | New file                                                 | Keep a Changelog format              |
| D-34 (GETTING_STARTED verify)      | `GETTING_STARTED.md`                                     | Verify all steps work                |
| D-35 (PLUGIN_DOCUMENTATION update) | `PLUGIN_DOCUMENTATION.md`                                | Expand feature matrix                |
| D-36 (CONTRIBUTING.md)             | New file                                                 | Guidelines + PR template             |
| D-37 (FAQ/Troubleshooting)         | `FAQ.md` or section in README                            | Common issues                        |
| D-38 (shadow JAR audit)            | `build.gradle.kts`                                       | Review minimize config               |
| D-39 (minimize exclusions)         | `build.gradle.kts`                                       | Test for NoClassDefFoundError        |
| D-40 (multi-version builds)        | `build.gradle.kts`, CI                                   | Add property-driven platform version |
| D-41 (Gradle plugin verify)        | `build.gradle.kts`                                       | Verify 1.17.x works with Gradle 8.x  |
| D-42 (artifact naming)             | `build.gradle.kts`                                       | Verify zip naming                    |
| D-43 (SNAPSHOT handling)           | `build.gradle.kts`, CI                                   | Add SNAPSHOT strip logic             |
| D-44 (GitHub Actions publish)      | `.github/workflows/build.yml`                            | Add publishPlugin step               |

---

## 8. Critical Path for Planning

**What MUST happen for a successful release (in rough order):**

1. Fix CacheManager singleton (D-07) — without this, caching is completely broken
2. Fix OsVApiService singleton (cascading from D-07) — without this, rate limiting is broken
3. Replace System.err with Logger (D-11, D-14) — basic hygiene, easy wins
4. Add PasswordSafe for tokens (D-13) — security requirement for marketplace
5. Add configurable API URL (D-08) — correctness
6. Dark mode via JBColor (D-02) — marketplace quality expectation
7. Update plugin.xml description + change notes (D-17) — marketplace listing
8. Plugin signing setup (D-21) — mandatory for marketplace
9. Generate 128x128 icon (D-22) — marketplace listing
10. Add MockWebServer + convert live API tests (D-29) — CI reliability
11. Integration tests (D-25) — quality gate
12. Create CHANGELOG.md (D-33) — release requires it
13. README update with current features (D-32) — first impression
14. GitHub Actions publishPlugin step (D-44) — automated release
15. Manual smoke test via TESTING-CHECKLIST.md (D-26) — final validation

**What is NICE but not blocking:**

- Animated scanning indicator (D-03)
- Toolbar actions (D-04)
- Status bar widget (D-06)
- Performance benchmarks (D-27)
- Multi-version CI matrix (D-28)
- CONTRIBUTING.md (D-36)
- FAQ.md (D-37)
- UI regression tests (D-31)

**What is explicitly deferred to Phase 22:**

- Line-level `PsiElement` problem registration (difficulty: high, value: moderate)
- Proper chart library for trends (difficulty: high, value: visual polish)
- Gradle version catalog support (difficulty: medium, value: moderate)

---

## 9. Risk Register

| Risk                                                  | Severity | Likelihood | Mitigation                                                                  |
| ----------------------------------------------------- | -------- | ---------- | --------------------------------------------------------------------------- |
| Plugin signing JAR validation fails with `minimize()` | High     | Medium     | Test `buildPlugin` + `runIde` before release; audit shadow exclusions       |
| `untilBuild="262.*"` too wide, crashes on 2025.x+     | Medium   | Medium     | Test on 2024.1 via `./gradlew runIde`; consider narrowing to `243.*`        |
| CacheManager singleton fix breaks existing tests      | Medium   | Low        | CacheManager is NOT registered in current tests; pure unit tests unaffected |
| PasswordSafe unavailable in headless CI tests         | Low      | Low        | Use null-fallback pattern; test in `runIde` environment                     |
| MockWebServer changes force API service refactor      | Low      | Medium     | Make `baseUrl` constructor-param with default; no breaking changes          |
| Plugin size exceeds marketplace limit (200MB)         | Low      | Very Low   | Shadow JAR is small (~2MB); verify after build                              |
| JetBrains marketplace review rejects listing          | Low      | Low        | Follow guidelines: https://plugins.jetbrains.com/docs/marketplace/          |

---

## 10. Reference: Marketplace Upload Checklist

Based on JetBrains Marketplace requirements:

- [ ] Plugin ZIP is signed (`signPlugin` task succeeds)
- [ ] `plugin.xml` has valid `<id>`, `<name>`, `<version>`, `<vendor>`
- [ ] `sinceBuild` and `untilBuild` are set and verified
- [ ] 128x128 plugin icon uploaded
- [ ] 3-5 screenshots (ideally annotated)
- [ ] Description is compelling and accurate
- [ ] Change notes list what's new
- [ ] Tags: security, dependencies, vulnerability scanning, SCA
- [ ] License: Apache 2.0 (already set)
- [ ] Source code repo is public and linked
- [ ] Issue tracker URL provided (GitHub issues)
- [ ] Compatible IDE versions tested
- [ ] No hardcoded secrets or credentials in source
- [ ] No malicious code or analytics without disclosure

---

## 11. Key Metrics at a Glance

| Metric            | Current                    | Target for Release                             |
| ----------------- | -------------------------- | ---------------------------------------------- |
| Total tests       | 282                        | 320+ (after MockWebServer + integration tests) |
| Test classes      | 25                         | 30+                                            |
| System.err calls  | 7                          | 0                                              |
| Singleton bugs    | 2                          | 0                                              |
| Hardcoded values  | 1 (API URL)                | 0                                              |
| Plain-text tokens | 2 (jiraToken, githubToken) | 0                                              |
| Icon sizes        | 5 (16-64px)                | 6 (add 128px)                                  |
| Plugin version    | 1.1.0                      | 1.1.0 (release)                                |
| Marketplace ready | No                         | Yes                                            |

---

_Phase 21 Research Document — For planning use only_
_Generated: 2026-05-12_
