import java.util.jar.JarFile

plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.dyuti"
version = project.findProperty("pluginVersion")?.toString() ?: "1.1.3"

repositories {
    mavenCentral()
}

dependencies {
    // JSON serialization (bundled in shadow JAR)
    implementation("com.google.code.gson:gson:2.11.0")

    // NOTE: org.apache.maven dependencies were removed in v1.1.2+
    // MavenParser uses pure regex parsing; no Maven model API needed.
    // This eliminates verifier warnings for java.net.URL and java.util.Locale
    // constructors used transitively by maven-model/maven-model-builder.

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

// Configure IntelliJ Platform
intellij {
    version.set(project.findProperty("intellijVersion")?.toString() ?: "2023.3")

    plugins.set(
        listOf(
            "com.intellij.java",
            "org.jetbrains.kotlin",
        ),
    )
}

tasks.patchPluginXml {
    pluginDescription.set(
        """
        A free, open-source IntelliJ IDEA plugin that provides security vulnerability scanning for open-source dependencies using the OSV database.
        """.trimIndent(),
    )

    changeNotes.set(
        """
        Version 1.1.1
        - Fixed AWT threading error when applying quick fix (IntelliJ 2026.1)
        - Fixed GHSA ID shown instead of CVE ID in scan results
        - Fixed fixed version showing as N/A (git commit hash filtering)
        - Fixed rate limit errors via OSV batch API
        - Fixed shortName mismatch for IntelliJ 2024.1+ compatibility
        - Fixed plugin compatibility with IntelliJ IDEA 2026.1
        - Test suite: 312 tests, zero failures

        Version 1.1.0
        - Added Vulnerable API Detection, Malicious Package Detection, Basic SAST
        - Added Privacy-Preserving Queries, Risk Scoring, Policy Enforcement
        - Added Team Collaboration, Differential Analysis, Historical Trending
        - Added SBOM Generation, Configuration Audit, IDE Notifications
        - Fixed CacheManager and OsVApiService singleton bugs
        - Replaced all System.err logging with IntelliJ Logger
        - Added configurable OSV API URL
        - Added dark mode support via JBColor
        - Added animated scanning indicator and status bar widget
        - Encrypted sensitive tokens via PasswordSafe
        - Compatible with IntelliJ IDEA 2023.3+

        Version 1.0.0
        - Initial release with dependency parsing, OSV API integration, tool window, inline inspection, and quick fixes
        """.trimIndent(),
    )

    // Support IntelliJ IDEA 2023.3 and later (no upper bound)
    sinceBuild.set("233.0")
    // No upper bound — plugin is API-compatible with all future IDEA versions.
    // Re-test and set an explicit untilBuild if binary incompatibilities arise.
    untilBuild.set("")
}

tasks.buildPlugin {
    dependsOn(tasks.test)
}

tasks.jarSearchableOptions {
    enabled = false
}

tasks.named("buildSearchableOptions") {
    enabled = false
}

// Shadow plugin configuration to bundle dependencies
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    dependencies {
        include(dependency("com.google.code.gson:gson"))
    }

    minimize {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:.*"))
        exclude(dependency("com.google.code.gson:.*"))
    }

    doLast {
        val jarFile = archiveFile.get().asFile
        println("=== Shadow JAR contents ===")
        println("Size: ${jarFile.length() / 1024} KB")
        val jar = JarFile(jarFile)
        try {
            val counts = mutableMapOf<String, Int>()
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory) {
                    val pkg = entry.name.substringBeforeLast('/')
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
            }
            counts.toSortedMap().forEach { (pkg, count) ->
                println("  $pkg/: $count files")
            }
        } finally {
            jar.close()
        }
        println("=== Total JAR size: ${jarFile.length() / 1024} KB ===")
    }
}

// Replace buildPlugin to use shadow JAR
val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.named<org.jetbrains.intellij.tasks.BuildPluginTask>("buildPlugin") {
    dependsOn(shadowJar)
    from(shadowJar.archiveFile) {
        into("/")
    }
}

// Plugin signing — requires IntelliJ Gradle Plugin 1.17.3+ or 2.x.
// For v1.17.0, signing must be done via the JetBrains Marketplace web UI after upload.
// Upgrade to 1.17.4 or 2.x to enable automated signing in CI.
//
// When available, uncomment the following:
//
// import org.jetbrains.intellij.tasks.SignPluginTask
//
// tasks.register<SignPluginTask>("signPlugin") {
//     dependsOn("buildPlugin")
//     inputArchiveFile.set(tasks.buildPlugin.get().archiveFile)
//     certificateChain.set(System.getenv("SIGN_CERTIFICATE_CHAIN"))
//     privateKey.set(System.getenv("SIGN_PRIVATE_KEY"))
//     password.set(System.getenv("SIGN_PRIVATE_KEY_PASSWORD"))
// }
