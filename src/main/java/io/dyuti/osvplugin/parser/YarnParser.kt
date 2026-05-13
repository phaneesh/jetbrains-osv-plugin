// JavaScript yarn.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for JavaScript `yarn.lock` files (v1 classic and v2+ berry).
 *
 * ## Supported format (yarn v1)
 * ```
 * # yarn lockfile v1
 * package-a@^1.0.0:
 *   version "1.2.3"
 *   resolved "https://registry.yarnpkg.com/..."
 *
 * "@scope/package@^2.0.0":
 *   version "2.1.0"
 *   resolved "..."
 * ```
 */
class YarnParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("yarn.lock")

    override fun detectEcosystem(filePath: String): String = "npm"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var currentName: String? = null
        val versionRegex = """^version\s+"([^"]+)"$""".toRegex()

        for (line in lines) {
            val trimmed = line.trim()

            // Package entry key: "name@version:" or name@version:
            // Name may contain @ (scoped packages), so find the LAST @ before :
            if (trimmed.endsWith(":") &&
                !trimmed.startsWith("#") &&
                !trimmed.startsWith("version ") &&
                trimmed.contains("@")
            ) {
                val clean = trimmed.trim('"').removeSuffix(":")
                val lastAt = clean.lastIndexOf('@')
                if (lastAt > 0) {
                    currentName = clean.substring(0, lastAt)
                }
                continue
            }

            // Version line within entry
            val vMatch = versionRegex.find(trimmed)
            if (vMatch != null && currentName != null) {
                dependencies.add(
                    Dependency(
                        name = currentName,
                        version = vMatch.groupValues[1],
                        ecosystem = "npm",
                        scope = "runtime",
                        transitive = true,
                    ),
                )
                currentName = null
            }
        }

        return dependencies
    }
}
