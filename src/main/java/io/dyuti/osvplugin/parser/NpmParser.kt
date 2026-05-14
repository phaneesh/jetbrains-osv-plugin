// OSV Vulnerability Scanner npm Parser
package io.dyuti.osvplugin.parser

import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for npm package-lock.json files
 */
class NpmParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("package-lock.json")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()

        val json = JsonParser.parseString(content).asJsonObject
        val packages = json.getAsJsonObject("packages")

        packages.entrySet().forEach { entry ->
            val key = entry.key
            val value = entry.value.asJsonObject

            // Skip root package
            if (key == "") return@forEach

            // Extract name and version
            val name = value.get("name")?.asString
            val version = value.get("version")?.asString

            if (name != null && version != null) {
                // Determine if this is a direct or transitive dependency
                val isDirect = key.startsWith("node_modules/") && !key.contains("/node_modules/")

                // For npm, we can't easily determine line numbers from package-lock.json
                // since it's a generated file, so we leave lineNumber as null

                dependencies.add(
                    Dependency(
                        name = name,
                        version = version,
                        ecosystem = "npm",
                        scope = if (isDirect) "compile" else "runtime",
                        transitive = !isDirect,
                        lineNumber = null, // Can't determine line number from package-lock.json
                    ),
                )
            }
        }

        return dependencies
    }
}
