// OSV Vulnerability Scanner Gradle Parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Gradle build.gradle files
 */
class GradleParser : DependencyParser() {
    
    override fun getSupportedExtensions(): List<String> = listOf("build.gradle", "build.gradle.kts")
    
    override fun parse(filePath: String, content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        
        // Extract group:name:version patterns from dependencies
        val depPattern = """(?:implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(\s*['"]([^:]+):([^:]+):([^'"]+)['"]\s*\)""".toRegex()
        
        depPattern.findAll(content).forEach { match ->
            val group = match.groupValues[1]
            val name = match.groupValues[2]
            val version = match.groupValues[3]
            
            dependencies.add(
                Dependency(
                    name = "$group:$name",
                    version = version,
                    ecosystem = "Gradle",
                    scope = "compile", // Default scope
                    transitive = false
                )
            )
        }
        
        return dependencies
    }
}
