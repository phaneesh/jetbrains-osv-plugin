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
    abstract fun parse(filePath: String, content: String): List<Dependency>
    
    /**
     * Check if this parser can handle the given file path
     */
    open fun canHandle(filePath: String): Boolean {
        return getSupportedExtensions().any { ext -> filePath.endsWith(ext) }
    }
    
    /**
     * Get supported file extensions
     */
    open fun getSupportedExtensions(): List<String> = emptyList()
    
    /**
     * Detect ecosystem from file path
     */
    open fun detectEcosystem(filePath: String): String {
        return when {
            filePath.endsWith("pom.xml") -> "Maven"
            filePath.endsWith("build.gradle") || filePath.endsWith("build.gradle.kts") -> "Gradle"
            filePath.endsWith("package-lock.json") -> "npm"
            filePath.endsWith("requirements.txt") || filePath.endsWith("pyproject.toml") -> "PyPI"
            else -> "Unknown"
        }
    }
    
    /**
     * Extract package name from full dependency name
     */
    fun extractPackageName(fullName: String): String {
        return when {
            fullName.contains(':') -> fullName.substringAfterLast(':')
            else -> fullName
        }
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
    fun resolveProperty(version: String, properties: Map<String, String>): String {
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
