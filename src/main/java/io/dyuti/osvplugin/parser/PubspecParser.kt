// Dart/Flutter pubspec.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Dart `pubspec.lock` files (YAML-like).
 *
 * ## Supported format
 * ```yaml
 * packages:
 *   async:
 *     dependency: transitive
 *     description:
 *       name: async
 *       url: "https://pub.dartlang.org"
 *     source: hosted
 *     version: "2.11.0"
 * ```
 */
class PubspecParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("pubspec.lock")

    override fun detectEcosystem(filePath: String): String = "Pub"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var inPackages = false
        var currentName: String? = null
        var currentVersion: String? = null
        var currentTransitive = true
        var currentScope = "runtime"

        val nameRegex = """^([a-zA-Z0-9_-]+)$""".toRegex()
        val versionRegex = """^version:\s*"([^"]+)"$""".toRegex()
        val depTypeRegex = """^dependency:\s*(.+)$""".toRegex()

        fun saveCurrent() {
            val n = currentName
            val v = currentVersion
            if (n != null && v != null) {
                dependencies.add(
                    Dependency(
                        name = n,
                        version = v,
                        ecosystem = "Pub",
                        scope = currentScope,
                        transitive = currentTransitive,
                    ),
                )
            }
            currentName = null
            currentVersion = null
            currentTransitive = true
            currentScope = "runtime"
        }

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed == "packages:" -> {
                    inPackages = true
                    continue
                }

                !inPackages -> {
                    continue
                }

                // Package names are at indent level 2 (line starts with two spaces but not four)
                line.startsWith("  ") && !line.startsWith("    ") && trimmed.endsWith(":") -> {
                    val stripped = trimmed.removeSuffix(":")
                    if (stripped.matches(nameRegex)) {
                        saveCurrent()
                        currentName = stripped
                    }
                }

                // Skip description sub-block names (indent >= 4)
                line.startsWith("    ") && trimmed.startsWith("name:") -> {
                    // nested name under description: — ignore
                }

                else -> {
                    versionRegex.find(trimmed)?.let {
                        currentVersion = it.groupValues[1]
                    }
                    depTypeRegex.find(trimmed)?.let {
                        val depType = it.groupValues[1].trim().lowercase()
                        currentTransitive = depType == "transitive"
                        currentScope = if (depType.contains("dev")) "test" else "runtime"
                    }
                }
            }
        }

        saveCurrent()
        return dependencies
    }
}
