// Haskell stack.yaml.lock / cabal.project.freeze parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Haskell `stack.yaml.lock` and `cabal.project.freeze` files.
 *
 * ## stack.yaml.lock format
 * ```yaml
 * packages:
 * - completed:
 *     hackage: aeson-2.1.2.1@sha256:...,1234
 *     pantry-tree:
 *       size: 5678
 *       sha256: ...
 *   original:
 *     hackage: aeson-2.1.2.1
 * ```
 *
 * ## cabal.project.freeze format
 * ```
 * constraints: any.aeson ==2.1.2.1,
 *              any.base ==4.17.2.0,
 *              any.text ==2.0.2
 * ```
 */
class StackParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("stack.yaml.lock", "cabal.project.freeze")

    override fun detectEcosystem(filePath: String): String = "Hackage"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> =
        when {
            filePath.endsWith("stack.yaml.lock") -> parseStackLock(content)
            filePath.endsWith("cabal.project.freeze") -> parseCabalFreeze(content)
            else -> emptyList()
        }

    private fun parseStackLock(content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        // Only match hackage lines under "completed:" (they have @sha256)
        val hackageRegex =
            """^\s*hackage:\s+([a-zA-Z0-9-]+)-(\d[\w.]*)\s*@.*$""".toRegex()

        for (line in lines) {
            val match = hackageRegex.find(line) ?: continue
            val name = match.groupValues[1]
            val version = match.groupValues[2]
            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "Hackage",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }

    private fun parseCabalFreeze(content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        val constraintRegex = """any\.([a-zA-Z0-9_-]+)\s*==\s*([\w.]+)""".toRegex()

        for (line in lines) {
            val match = constraintRegex.find(line) ?: continue
            val name = match.groupValues[1]
            val version = match.groupValues[2]
            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "Hackage",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
