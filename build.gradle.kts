plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.dyuti"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0") // OkHttp 4.x dependency
    implementation("com.google.code.gson:gson:2.11.0")

    // Maven model API for pom.xml parsing/writing
    implementation("org.apache.maven:maven-model:3.9.6")
    implementation("org.apache.maven:maven-model-builder:3.9.6")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
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
    version.set("2023.3")

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
        Version 1.1.0
        - Updated compatibility to support IntelliJ IDEA 2026.1.x and later
        - Fixed plugin.xml compatibility constraints
        - Added JSON module dependency for improved file parsing
        - Improved tree view rendering
        
        Version 1.0.0
        - Initial release
        - Dependency parsing for Maven, Gradle, npm, and pip
        - OSV API integration
        - Tool window with vulnerability display
        - Local inspection with inline highlighting
        - Settings configuration
        - Organization management
        - Jira integration
        - License detection
        - SARIF export
        """.trimIndent(),
    )

    // Support IntelliJ IDEA 2023.3 and later (including 2026.x)
    sinceBuild.set("233.0")
    untilBuild.set("262.*")
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
// OkHttp 4.x requires Okio as a transitive dependency
// Okio uses okio-jvm as the actual artifact for JVM target
// Maven model API for pom.xml parsing/writing
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    dependencies {
        include(dependency("com.squareup.okhttp3:okhttp"))
        include(dependency("com.squareup.okio:okio-jvm"))
        include(dependency("com.google.code.gson:gson"))
        include(dependency("org.apache.maven:maven-model"))
        include(dependency("org.apache.maven:maven-model-builder"))
    }

    minimize {
        // Exclude unused classes to reduce size
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:.*"))
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
