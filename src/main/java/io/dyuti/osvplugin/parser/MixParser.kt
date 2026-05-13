// Elixir mix.lock parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Elixir `mix.lock` files.
 *
 * ## Supported format
 * ```elixir
 * %{
 *   "decimal": {:hex, :decimal, "2.1.1", "...", [:mix], [], "hexpm", "..."},
 *   "ecto": {:hex, :ecto, "3.11.0", "...", [:mix], [{:decimal, "~> 2.0"}], "hexpm", "..."},
 *   "phoenix": {:git, "https://github.com/...", "abc123", []},
 * }
 * ```
 */
class MixParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("mix.lock")

    override fun detectEcosystem(filePath: String): String = "Hex"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        val hexRegex =
            """^\s*[:"]?([\w-]+)["]?\s*:\s*\{:hex,\s*:[\w-]+,\s*"([^"]+)".*$""".toRegex()

        for (line in lines) {
            val match = hexRegex.find(line) ?: continue
            val name = match.groupValues[1]
            val version = match.groupValues[2]
            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "Hex",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
