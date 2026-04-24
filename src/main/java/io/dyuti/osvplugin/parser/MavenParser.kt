// OSV Vulnerability Scanner Maven Parser
package io.dyuti.osvplugin.parser

import io.dyuti.osvplugin.api.model.Dependency

/**
 * Parser for Maven pom.xml files
 */
class MavenParser : DependencyParser() {
    
    override fun getSupportedExtensions(): List<String> = listOf("pom.xml")
    
    override fun parse(filePath: String, content: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        
        // Extract properties
        val properties = extractProperties(content)
        
        // Parse dependencies using simple XML parsing
        val depPattern = """<dependency>\s*<groupId>([^<]+)</groupId>\s*<artifactId>([^<]+)</artifactId>\s*<version>([^<]+)</version>.*?</dependency>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        depPattern.findAll(content).forEach { match ->
            val groupId = match.groupValues[1]
            val artifactId = match.groupValues[2]
            var version = match.groupValues[3]
            
            // Resolve version properties
            version = resolveProperty(version, properties)
            
            // Extract scope if present
            val scope = extractScope(content, match)
            
            dependencies.add(
                Dependency(
                    name = "$groupId:$artifactId",
                    version = version,
                    ecosystem = "Maven",
                    scope = scope,
                    transitive = false
                )
            )
        }
        
        return dependencies
    }
    
    private fun extractProperties(content: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        // Simple property extraction
        val propPattern = """<([^>]+)>([^<]+)</\1>""".toRegex()
        propPattern.findAll(content).forEach { match ->
            val tag = match.groupValues[1]
            val value = match.groupValues[2]
            
            // Check if this is a property
            if (tag !in listOf("dependency", "groupId", "artifactId", "version", "scope", "exclusions", "dependencies")) {
                properties[tag] = value
            }
        }
        
        return properties
    }
    
    private fun extractScope(content: String, match: MatchResult): String {
        // Extract scope from the full match context
        val fullMatch = match.groupValues[0]
        
        val scopePattern = """<scope>([^<]+)</scope>""".toRegex()
        val scopeMatch = scopePattern.find(fullMatch)
        
        return scopeMatch?.groupValues?.get(1) ?: "compile"
    }
}
