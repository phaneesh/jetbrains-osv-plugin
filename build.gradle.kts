plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm") version "1.9.20"
}

group = "io.dyuti"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    
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
            "org.jetbrains.kotlin"
        )
    )
}

tasks.patchPluginXml {
    pluginDescription.set("""
        A free, open-source IntelliJ IDEA plugin that provides security vulnerability scanning for open-source dependencies using the OSV database.
    """.trimIndent())
    
    changeNotes.set("""
        Version 1.0.0
        - Initial release
        - Dependency parsing for Maven, Gradle, npm, and pip
        - OSV API integration
        - Tool window with vulnerability display
        - Local inspection with inline highlighting
        - Settings configuration
    """.trimIndent())
}

tasks.buildPlugin {
    dependsOn(tasks.test)
}

tasks.jarSearchableOptions {
    enabled = false
}
