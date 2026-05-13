// OSV Vulnerability Scanner Parser Package
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Abstract dependency parser interface
 */
abstract class DependencyParser {
    /**
     * Parse dependencies from file content
     */
    abstract fun parse(
        filePath: String,
        content: String,
    ): List<Dependency>

    /**
     * Check if this parser can handle the given file path
     */
    open fun canHandle(filePath: String): Boolean = getSupportedExtensions().any { ext -> filePath.endsWith(ext) }

    /**
     * Get supported file extensions
     */
    open fun getSupportedExtensions(): List<String> = emptyList()

    /**
     * Detect ecosystem from file path
     */
    open fun detectEcosystem(filePath: String): String =
        when {
            filePath.endsWith("pom.xml") || filePath.endsWith("gradle.lockfile") ||
                filePath.endsWith("buildscript-gradle.lockfile") ||
                filePath.endsWith("verification-metadata.xml") -> "Maven"

            filePath.endsWith("build.gradle") || filePath.endsWith("build.gradle.kts") -> "Gradle"

            filePath.endsWith("package-lock.json") || filePath.endsWith("yarn.lock") ||
                filePath.endsWith("pnpm-lock.yaml") || filePath.endsWith("bun.lock") -> "npm"

            filePath.endsWith("requirements.txt") || filePath.endsWith("pyproject.toml") ||
                filePath.endsWith("poetry.lock") || filePath.endsWith("Pipfile.lock") ||
                filePath.endsWith("pdm.lock") || filePath.endsWith("uv.lock") ||
                filePath.endsWith("pylock.toml") -> "PyPI"

            filePath.endsWith("go.mod") || filePath.endsWith("go.sum") -> "Go"

            filePath.endsWith("Cargo.lock") -> "crates.io"

            filePath.endsWith("composer.lock") -> "Packagist"

            filePath.endsWith("Gemfile.lock") || filePath.endsWith("gems.locked") -> "RubyGems"

            filePath.endsWith("pubspec.lock") -> "Pub"

            filePath.endsWith("packages.lock.json") || filePath.endsWith("packages.config") ||
                filePath.endsWith(".deps.json") -> "NuGet"

            filePath.endsWith("stack.yaml.lock") || filePath.endsWith("cabal.project.freeze") -> "Hackage"

            filePath.endsWith("mix.lock") -> "Hex"

            filePath.endsWith("renv.lock") -> "CRAN"

            filePath.endsWith("conan.lock") -> "ConanCenter"

            else -> "Unknown"
        }

    /**
     * Extract package name from full dependency name
     */
    fun extractPackageName(fullName: String): String =
        when {
            fullName.contains(':') -> fullName.substringAfterLast(':')
            else -> fullName
        }

    /**
     * Normalize version string
     */
    fun normalizeVersion(version: String): String {
        // Remove leading 'v' or 'V' from version
        var normalized = version
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1)
        }

        // Remove leading 'v' from semantic versions (e.g., v1.0.0 -> 1.0.0)
        return normalized.trim()
    }

    /**
     * Resolve version property references
     */
    fun resolveProperty(
        version: String,
        properties: Map<String, String>,
    ): String {
        var resolved = version

        // Resolve ${property} style references
        val propertyRegex = """\$\{([^}]+)\}""".toRegex()
        val matches = propertyRegex.findAll(resolved)

        matches.forEach { match ->
            val propertyName = match.groupValues[1]
            val propertyValue = properties[propertyName]
            if (propertyValue != null) {
                resolved = resolved.replace(match.value, propertyValue)
            }
        }

        return resolved
    }
}
