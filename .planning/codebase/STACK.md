# Technology Stack

## Core Languages & Runtime

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 1.9.20 | Primary implementation language |
| **JVM Target** | 17 | Runtime compatibility (sourceCompatibility / targetCompatibility) |
| **Gradle** | 8.x | Build system (`./gradlew`) |

## Build Configuration

- **`build.gradle.kts`** — Kotlin DSL build script with the following plugins:
  - `org.jetbrains.intellij` v1.17.0 — IntelliJ Platform Gradle Plugin
  - `kotlin("jvm")` v1.9.20 — Kotlin JVM plugin
  - `com.github.johnrengelman.shadow` v8.1.1 — Shadow/uber-JAR bundling

- **`gradle/wrapper/`** — Gradle wrapper configuration
- **Shadow JAR configuration** bundles `gson`, `maven-model`, `maven-model-builder` into plugin distribution

## IntelliJ Platform SDK

| Detail | Value |
|--------|-------|
| Platform Version | 2023.3 |
| Since Build | 233.0 |
| Until Build | 262.* |
| Required Modules | `com.intellij.modules.platform`, `com.intellij.modules.java` |

## Runtime Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.google.code.gson:gson` | 2.11.0 | JSON parsing for OSV API responses |
| `org.apache.maven:maven-model` | 3.9.6 | Maven `pom.xml` object model parsing |
| `org.apache.maven:maven-model-builder` | 3.9.6 | Maven model building (effective POM) |

## Test Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `kotlin("test")` | (bundled) | Kotlin test assertions |
| `org.junit.jupiter:junit-jupiter` | 5.10.0 | JUnit 5 testing framework |
| `org.mockito:mockito-core` | 5.12.0 | Java mocking |
| `org.mockito.kotlin:mockito-kotlin` | 5.4.0 | Kotlin-friendly Mockito API |
| `io.mockk:mockk` | 1.13.9 | Native Kotlin mocking |

## Key Configuration Files

- `build.gradle.kts` — Build, dependencies, plugin packaging, shadow JAR config
- `src/main/resources/META-INF/plugin.xml` — IntelliJ plugin manifest (extensions, inspections, services, tool windows)
- `src/main/resources/OsVBundle.properties` — Internationalization bundle
- `gradle.properties` / `settings.gradle.kts` — Gradle project settings

## Version & Packaging

- Group: `io.dyuti`
- Version: `1.1.0`
- Plugin ID: `io.dyuti.osvplugin`
- Plugin Name: OSV Vulnerability Scanner
- Output: Shadow JAR bundled in plugin ZIP under `build/distributions/`
