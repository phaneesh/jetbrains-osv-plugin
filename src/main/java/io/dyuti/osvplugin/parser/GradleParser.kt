// OSV Vulnerability Scanner Gradle Parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parses Gradle dependency declarations from build scripts.
 *
 * **Supported syntax:**
 * - `implementation("group:artifact:version")`
 * - `testImplementation("group:artifact:version")`
 * - `api("group:artifact:version")`
 * - `compileOnly("group:artifact:version")`
 * - `runtimeOnly("group:artifact:version")`
 * - `androidTestImplementation("group:artifact:version")`
 * - `debugImplementation("group:artifact:version")`
 * - `releaseImplementation("group:artifact:version")`
 *
 * **Not supported (known limitations):**
 * - `platform()` / `enforcedPlatform()` wrapper dependencies
 * - Gradle version catalogs (`libs.versions.toml`)
 * - Kotlin DSL `implementation(group = "...", name = "...", version = "...")` map syntax
 * - String interpolation / variable references for version numbers
 *
 * These patterns are parsed as best-effort; fallback to manual review if parsing appears incomplete.
 */
class GradleParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("build.gradle", "build.gradle.kts")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()

        // Extract group:name:version patterns from dependencies
        val depPattern =
            Regex(
                """(?:implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(\s*['"]([^:]+):([^:]+):([^'\"]+)['"]\s*\)""",
            )

        depPattern.findAll(content).forEach { match ->
            val group = match.groupValues[1]
            val name = match.groupValues[2]
            val version = match.groupValues[3]

            // Calculate line number
            val startOffset = match.range.first
            val lineNumber = calculateLineNumber(content, startOffset)

            dependencies.add(
                Dependency(
                    name = "$group:$name",
                    version = version,
                    ecosystem = "Gradle",
                    scope = "compile", // Default scope
                    transitive = false,
                    lineNumber = lineNumber,
                ),
            )
        }

        return dependencies
    }

    private fun calculateLineNumber(
        content: String,
        offset: Int,
    ): Int {
        val lines = content.substring(0, offset).count { it == '\n' }
        return lines + 1 // Line numbers are 1-based
    }
}
