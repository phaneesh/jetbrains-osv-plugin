// Python poetry.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Python `poetry.lock` files (TOML format).
 *
 * ## Supported format
 * ```toml
 * [[package]]
 * name = "requests"
 * version = "2.31.0"
 * description = "..."
 * optional = false
 * python-versions = ">=3.7"
 * files = [...]
 * ```
 */
class PoetryParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("poetry.lock")

    override fun detectEcosystem(filePath: String): String = "PyPI"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var currentName: String? = null
        var currentVersion: String? = null

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed == "[[package]]" -> {
                    if (currentName != null && currentVersion != null) {
                        dependencies.add(
                            Dependency(
                                name = currentName,
                                version = currentVersion,
                                ecosystem = "PyPI",
                                scope = "runtime",
                                transitive = true,
                            ),
                        )
                    }
                    currentName = null
                    currentVersion = null
                }

                trimmed.startsWith("name =") -> {
                    currentName = trimmed.substringAfter("\"").substringBeforeLast("\"")
                }

                trimmed.startsWith("version =") -> {
                    currentVersion = trimmed.substringAfter("\"").substringBeforeLast("\"")
                }
            }
        }

        // Save last
        if (currentName != null && currentVersion != null) {
            dependencies.add(
                Dependency(
                    name = currentName,
                    version = currentVersion,
                    ecosystem = "PyPI",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
