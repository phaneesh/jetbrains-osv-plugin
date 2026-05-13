// .NET packages.lock.json / packages.config parser
package io.dyuti.osvplugin.parser

import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for .NET `packages.lock.json` and `packages.config` files.
 *
 * ## packages.lock.json format
 * ```json
 * {
 *   "version": 2,
 *   "dependencies": {
 *     ".NETCoreApp,Version=v8.0": {
 *       "Newtonsoft.Json": {
 *         "type": "Direct",
 *         "resolved": "13.0.3"
 *       },
 *       "System.Text.Json": {
 *         "type": "Transitive",
 *         "resolved": "8.0.0"
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * ## packages.config format
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <packages>
 *   <package id="Newtonsoft.Json" version="13.0.3" targetFramework="net48" />
 * </packages>
 * ```
 */
class NugetParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("packages.lock.json", "packages.config")

    override fun detectEcosystem(filePath: String): String = "NuGet"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> =
        when {
            filePath.endsWith("packages.lock.json") -> parsePackagesLock(content)
            filePath.endsWith("packages.config") -> parsePackagesConfig(content)
            else -> emptyList()
        }

    private fun parsePackagesLock(content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val json = JsonParser.parseString(content).asJsonObject
        val depsObj = json.getAsJsonObject("dependencies") ?: return dependencies

        depsObj.entrySet().forEach { frameworkEntry ->
            val packages = frameworkEntry.value.asJsonObject
            packages.entrySet().forEach { pkgEntry ->
                val name = pkgEntry.key
                val pkgObj = pkgEntry.value.asJsonObject
                val version = pkgObj.get("resolved")?.asString ?: return@forEach
                val type = pkgObj.get("type")?.asString ?: "Transitive"
                dependencies.add(
                    Dependency(
                        name = name,
                        version = version,
                        ecosystem = "NuGet",
                        scope = "runtime",
                        transitive = type != "Direct",
                    ),
                )
            }
        }

        return dependencies
    }

    private fun parsePackagesConfig(content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val regex = """<package\s+[^>]*id="([^"]+)"[^>]*version="([^"]+)"[^/]*/?>""".toRegex()
        regex.findAll(content).forEach { match ->
            dependencies.add(
                Dependency(
                    name = match.groupValues[1],
                    version = match.groupValues[2],
                    ecosystem = "NuGet",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }
        return dependencies
    }
}
