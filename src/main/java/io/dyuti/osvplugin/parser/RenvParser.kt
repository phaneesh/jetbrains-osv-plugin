// R renv.lock parser
package io.dyuti.osvplugin.parser

import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for R `renv.lock` files (JSON).
 *
 * ## Supported format
 * ```json
 * {
 *   "R": {"Version": "4.3.1"},
 *   "Packages": {
 *     "dplyr": {
 *       "Package": "dplyr",
 *       "Version": "1.1.3",
 *       "Source": "Repository",
 *       "Repository": "CRAN"
 *     },
 *     "ggplot2": {
 *       "Package": "ggplot2",
 *       "Version": "3.4.3",
 *       "Source": "GitHub",
 *       "RemoteSha": "abc123"
 *     }
 *   }
 * }
 * ```
 */
class RenvParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("renv.lock")

    override fun detectEcosystem(filePath: String): String = "CRAN"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val json = JsonParser.parseString(content).asJsonObject
        val packages = json.getAsJsonObject("Packages") ?: return dependencies

        packages.entrySet().forEach { entry ->
            val pkg = entry.value.asJsonObject
            val name = pkg.get("Package")?.takeIf { !it.isJsonNull }?.asString ?: entry.key
            val versionEl = pkg.get("Version")
            var version = if (versionEl == null || versionEl.isJsonNull) null else versionEl.asString
            val source = pkg.get("Source")?.takeIf { !it.isJsonNull }?.asString ?: "Repository"

            if (version == null && source == "GitHub") {
                version = pkg.get("RemoteSha")?.takeIf { !it.isJsonNull }?.asString
            }
            if (version == null) return@forEach

            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = if (source == "GitHub") "GitHub" else "CRAN",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
