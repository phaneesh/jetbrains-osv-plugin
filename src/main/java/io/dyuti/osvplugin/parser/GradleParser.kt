// OSV Vulnerability Scanner Gradle Parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Gradle build.gradle files
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
