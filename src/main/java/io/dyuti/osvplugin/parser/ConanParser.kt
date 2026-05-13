// C/C++ conan.lock parser
package io.dyuti.osvplugin.parser

import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for C/C++ `conan.lock` files.
 *
 * ## Supported format
 * ```json
 * {
 *     "version": "0.5",
 *     "requires": [
 *         "zlib/1.3#...",
 *         "openssl/3.1.2#...",
 *         "boost/1.82.0#..."
 *     ]
 * }
 * ```
 */
class ConanParser : DependencyParser() {
    override fun getSupportedExtensions(): List<String> = listOf("conan.lock")

    override fun detectEcosystem(filePath: String): String = "ConanCenter"

    override fun parse(
        filePath: String,
        content: String,
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val json = JsonParser.parseString(content).asJsonObject
        val requires = json.getAsJsonArray("requires") ?: return dependencies

        requires.forEach { element ->
            val req = element.asString
            val parts = req.split("/", limit = 2)
            if (parts.size < 2) return@forEach
            val name = parts[0]
            val versionWithRev = parts[1]
            val version = versionWithRev.split("#", limit = 2)[0]

            dependencies.add(
                Dependency(
                    name = name,
                    version = version,
                    ecosystem = "ConanCenter",
                    scope = "runtime",
                    transitive = true,
                ),
            )
        }

        return dependencies
    }
}
