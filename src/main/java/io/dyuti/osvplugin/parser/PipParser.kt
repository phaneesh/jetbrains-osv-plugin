// OSV Vulnerability Scanner pip Parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for pip requirements.txt files
 */
class PipParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("requirements.txt", "pyproject.toml")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()

        val lines = content.lines()

        for (index in lines.indices) {
            val line = lines[index]
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Parse package==version or package>=version patterns
            val packagePattern = """([a-zA-Z0-9_-]+)\s*(==|>=|<=|~=|!=|>\s|<)\s*([a-zA-Z0-9._-]+)""".toRegex()
            val match = packagePattern.find(trimmed)

            if (match != null) {
                val name = match.groupValues[1]
                val version = match.groupValues[3]

                // Line numbers are 1-based, so add 1 to index
                val lineNumber = index + 1

                dependencies.add(
                    Dependency(
                        name = name,
                        version = version,
                        ecosystem = "PyPI",
                        scope = "runtime",
                        transitive = false,
                        lineNumber = lineNumber,
                    ),
                )
            }
        }

        return dependencies
    }
}
