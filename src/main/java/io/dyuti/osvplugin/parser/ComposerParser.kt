// PHP composer.lock parser
package io.dyuti.osvplugin.parser

import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for PHP `composer.lock` files.
 *
 * ## Supported format
 * ```json
 * {
 *   "packages": [
 *     {"name": "vendor/package", "version": "1.2.3", "type": "library"}
 *   ],
 *   "packages-dev": [
 *     {"name": "vendor/dev-pkg", "version": "2.0.0", "type": "library"}
 *   ]
 * }
 * ```
 */
class ComposerParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("composer.lock")

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val json = JsonParser.parseString(content).asJsonObject

        // Production packages
        json.getAsJsonArray("packages")?.forEach { element ->
            val obj = element.asJsonObject
            val name = obj.get("name")?.asString ?: return@forEach
            val version = obj.get("version")?.asString ?: return@forEach
            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "Packagist",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        // Dev packages
        json.getAsJsonArray("packages-dev")?.forEach { element ->
            val obj = element.asJsonObject
            val name = obj.get("name")?.asString ?: return@forEach
            val version = obj.get("version")?.asString ?: return@forEach
            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "Packagist",
                    scope = "test",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
