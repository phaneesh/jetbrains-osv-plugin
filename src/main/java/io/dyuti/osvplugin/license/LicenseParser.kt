// SPDX License Scanner Integration - Parser
package io.dyuti.osvplugin.license

import io.dyuti.osvplugin.api.model.Dependency

/**
 * License parser for various package ecosystems
 */
object LicenseParser {
    
    /**
     * Parse license from dependency metadata
     */
    fun parseLicense(dependency: Dependency): String {
        // Try to extract license from common patterns
        val name = dependency.name.lowercase()
        @Suppress("UNUSED_VARIABLE")
        val version = dependency.version.lowercase()
        
        // Maven/Gradle: Often in metadata
        return when {
            // Common patterns in package names
            "mit" in name || "mit-license" in name -> "MIT"
            "apache" in name -> "Apache-2.0"
            "gpl" in name -> detectGplVersion(name)
            "lgpl" in name -> "LGPL-3.0-only"
            "bsd" in name -> "BSD-3-Clause"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Detect GPL version from name
     */
    private fun detectGplVersion(name: String): String {
        return when {
            "gpl2" in name || "gpl-2" in name -> "GPL-2.0-only"
            "gpl3" in name || "gpl-3" in name -> "GPL-3.0-only"
            else -> "GPL-3.0-or-later"
        }
    }
    
    /**
     * Parse license from common file contents
     */
    fun parseLicenseFromFile(content: String): List<String> {
        val licenses = mutableListOf<String>()
        
        // Try to find SPDX license expressions
        val spdxPattern = "SPDX-License-Identifier:\\s*([^\n]+)".toRegex()
        val match = spdxPattern.find(content)
        if (match != null) {
            licenses.add(match.groupValues[1].trim())
        }
        
        // Look for license headers
        val licenseHeaders = listOf(
            "MIT License" to "MIT",
            "Apache License, Version 2.0" to "Apache-2.0",
            "GNU General Public License" to "GPL-3.0-or-later",
            "GNU Lesser General Public License" to "LGPL-3.0-only",
            "BSD License" to "BSD-3-Clause"
        )
        
        for ((header, license) in licenseHeaders) {
            if (header in content) {
                licenses.add(license)
            }
        }
        
        return licenses.distinct()
    }
    
    /**
     * Extract license from package.json (npm)
     */
    fun parseNpmLicense(content: String): String {
        // Simple JSON parsing without external dependency
        val licensePattern = "\"license\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }
    
    /**
     * Extract license from package-lock.json
     */
    fun parseLockfileLicense(content: String, packageName: String): String {
        // Extract license from lockfile for specific package
        val pattern = "\"$packageName\"\\s*\\{[^}]*\"license\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }
    
    /**
     * Extract license from requirements.txt (pip)
     */
    fun parsePipLicense(content: String): String {
        // Look for license specifier
        val licensePattern = "#\\s*License:\\s*(.+)".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1)?.trim() ?: "UNKNOWN"
    }
    
    /**
     * Extract license from setup.py (pip)
     */
    fun parseSetupPyLicense(content: String): String {
        val licensePattern = "license\\s*=\\s*[\"']([^\"']+)[\"']".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }
}
