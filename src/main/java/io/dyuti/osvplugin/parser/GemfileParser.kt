// Ruby Gemfile.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Ruby `Gemfile.lock` and `gems.locked` files.
 *
 * ## Supported format (excerpt)
 * ```
 * GEM
 *   remote: https://rubygems.org/
 *   specs:
 *     activesupport (7.1.0)
 *       concurrent-ruby (~> 1.0)
 *     bundler (2.4.0)
 *
 * PLATFORMS
 *   ruby
 *
 * DEPENDENCIES
 *   activesupport
 * ```
 */
class GemfileParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("Gemfile.lock", "gems.locked")

    override fun detectEcosystem(filePath: String): String = "RubyGems"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var inSpecs = false
        val depRegex = """^\s{4}([a-zA-Z0-9_.-]+)\s+\(([0-9][^)]*)\).*$""".toRegex()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed == "specs:" -> {
                    inSpecs = true
                    continue
                }

                inSpecs && (trimmed == "PLATFORMS" || trimmed == "DEPENDENCIES" || trimmed.isEmpty()) -> {
                    inSpecs = false
                    continue
                }

                inSpecs -> {
                    val match = depRegex.find(line) ?: continue
                    val name = match.groupValues[1]
                    val version = match.groupValues[2]
                    dependencies.add(
                        Dependency(
                            name = name,
                            version = version,
                            ecosystem = "RubyGems",
                            scope = "runtime",
                            transitive = true,
                        ),
                    )
                }
            }
        }

        return dependencies
    }
}
