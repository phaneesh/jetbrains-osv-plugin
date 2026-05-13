// Go modules dependency parser (go.mod)
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Go `go.mod` files.
 *
 * ## Supported format
 *
 * ```go
 * module github.com/example/project
 *
 * go 1.21
 *
 * require (
 *     github.com/gin-gonic/gin v1.9.1
 *     github.com/stretchr/testify v1.8.4
 *     golang.org/x/crypto v0.17.0
 * )
 *
 * require github.com/pkg/errors v0.9.1 // indirect
 * ```
 */
class GoParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("go.mod")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lines = content.lines()

        var inRequireBlock = false
        val requireRegex =
            """^\s*require\s*\(\s*$""".toRegex()
        val singleRequireRegex =
            """^\s*require\s+([\w./-]+)\s+(v?[^\s]+)(\s+//\s*(\w+))?.*$""".toRegex()
        val depInBlockRegex =
            """^\s*([\w./-]+)\s+(v?[^\s]+)(\s+//\s*(\w+))?.*$""".toRegex()
        val closingParenRegex =
            """^\s*\)\s*$""".toRegex()

        for ((index, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("//")) continue

            // Start of `require ( ... )` block
            if (requireRegex.matches(line)) {
                inRequireBlock = true
                continue
            }

            // End of require block
            if (inRequireBlock && closingParenRegex.matches(line)) {
                inRequireBlock = false
                continue
            }

            val match =
                when {
                    inRequireBlock -> depInBlockRegex.find(line)
                    else -> singleRequireRegex.find(line)
                }

            if (match != null) {
                val name = match.groupValues[1]
                val version = match.groupValues[2]
                val qualifier = match.groupValues[4] // indirect / test / etc.

                dependencies.add(
                    Dependency(
                        name = name,
                        version = version,
                        ecosystem = "Go",
                        scope = if (qualifier == "test") "test" else "runtime",
                        transitive = qualifier == "indirect",
                        lineNumber = index + 1,
                    ),
                )
            }
        }

        return dependencies
    }
}
