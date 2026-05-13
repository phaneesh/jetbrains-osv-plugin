// Rust Cargo.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Rust `Cargo.lock` files (TOML format).
 *
 * Example:
 * ```toml
 * [[package]]
 * name = "serde"
 * version = "1.0.190"
 * source = "registry+https://github.com/rust-lang/crates.io-index"
 * checksum = "..."
 * ```
 */
class CargoParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("Cargo.lock")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var currentName: String? = null
        var currentVersion: String? = null
        var inPackage = false

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed == "[[package]]" -> {
                    // Save previous package if complete
                    if (currentName != null && currentVersion != null) {
                        dependencies.add(
                            Dependency(
                                name = currentName,
                                version = currentVersion,
                                ecosystem = "crates.io",
                                scope = "runtime",
                                transitive = true,
                            ),
                        )
                    }
                    currentName = null
                    currentVersion = null
                    inPackage = true
                }

                trimmed.startsWith("name =") && inPackage -> {
                    currentName = trimmed.substringAfter("\"").substringBeforeLast("\"")
                }

                trimmed.startsWith("version =") && inPackage -> {
                    currentVersion = trimmed.substringAfter("\"").substringBeforeLast("\"")
                }
            }
        }

        // Don't forget the last package
        if (currentName != null && currentVersion != null) {
            dependencies.add(
                Dependency(
                    name = currentName,
                    version = currentVersion,
                    ecosystem = "crates.io",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
