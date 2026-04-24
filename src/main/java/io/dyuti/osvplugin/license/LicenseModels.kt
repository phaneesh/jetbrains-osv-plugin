// SPDX License Scanner Integration
package io.dyuti.osvplugin.license

/**
 * License model representing a software license
 */
data class License(
    val spdxId: String,
    val name: String,
    val url: String? = null,
    val isCopyleft: Boolean = false,
    val isPermissive: Boolean = false,
    val isOSIApproved: Boolean = false,
    val isFSFFree: Boolean = false
) {
    companion object {
        // Common permissive licenses
        val PERMISSIVE_LICENSES = setOf(
            "MIT",
            "Apache-2.0",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ISC",
            "Unlicense",
            "CC0-1.0"
        )

        // Common copyleft licenses
        val COPYLEFT_LICENSES = setOf(
            "GPL-2.0-only",
            "GPL-2.0-or-later",
            "GPL-3.0-only",
            "GPL-3.0-or-later",
            "LGPL-2.1-only",
            "LGPL-2.1-or-later",
            "LGPL-3.0-only",
            "LGPL-3.0-or-later",
            "AGPL-3.0-only",
            "AGPL-3.0-or-later"
        )

        fun isPermissive(license: String): Boolean {
            return PERMISSIVE_LICENSES.contains(license)
        }

        fun isCopyleft(license: String): Boolean {
            return COPYLEFT_LICENSES.contains(license)
        }
    }
}

/**
 * License conflict detector
 */
object LicenseConflictDetector {
    
    /**
     * Check if two licenses are compatible
     */
    fun areCompatible(license1: String, license2: String): Boolean {
        // Same license is always compatible
        if (license1 == license2) return true
        
        // Permissive licenses are generally compatible with each other
        if (License.isPermissive(license1) && License.isPermissive(license2)) {
            return true
        }
        
        // Copyleft licenses may not be compatible with permissive
        // (depends on specific license terms)
        if (License.isCopyleft(license1) && License.isCopyleft(license2)) {
            return areCopyleftCompatible(license1, license2)
        }
        
        // Mixed copyleft and permissive - check specific cases
        return areMixedCompatible(license1, license2)
    }
    
    /**
     * Check if two copyleft licenses are compatible
     */
    private fun areCopyleftCompatible(license1: String, license2: String): Boolean {
        // Same copyleft license family is compatible
        val family1 = getLicenseFamily(license1)
        val family2 = getLicenseFamily(license2)
        
        if (family1 == family2) {
            return areVersionCompatible(license1, license2)
        }
        
        // Different families may not be compatible
        return false
    }
    
    /**
     * Check mixed copyleft and permissive compatibility
     */
    private fun areMixedCompatible(license1: String, license2: String): Boolean {
        val isPermissive1 = License.isPermissive(license1)
        val isPermissive2 = License.isPermissive(license2)
        
        // Permissive can be relicensed under copyleft
        if (isPermissive1 && License.isCopyleft(license2)) {
            return true
        }
        
        // Copyleft cannot be relicensed under permissive
        if (License.isCopyleft(license1) && isPermissive2) {
            return false
        }
        
        return true
    }
    
    /**
     * Check version compatibility within license family
     */
    private fun areVersionCompatible(license1: String, license2: String): Boolean {
        val version1 = extractVersion(license1)
        val version2 = extractVersion(license2)
        
        // Same version is compatible
        if (version1 == version2) return true
        
        // Later versions are usually compatible with earlier
        return version2 > version1
    }
    
    /**
     * Get license family
     */
    private fun getLicenseFamily(license: String): String {
        return when {
            license.startsWith("MIT") -> "MIT"
            license.startsWith("Apache-2") -> "Apache"
            license.startsWith("BSD") -> "BSD"
            license.startsWith("GPL") -> "GPL"
            license.startsWith("LGPL") -> "LGPL"
            license.startsWith("AGPL") -> "AGPL"
            else -> "Other"
        }
    }
    
    /**
     * Extract version number from license
     */
    private fun extractVersion(license: String): Int {
        val match = "\\d+".toRegex().find(license)
        return match?.value?.toIntOrNull() ?: 0
    }
    
    /**
     * Detect license conflicts in a list of dependencies
     */
    fun detectConflicts(dependencies: List<DependencyWithLicense>): List<LicenseConflict> {
        val conflicts = mutableListOf<LicenseConflict>()
        
        for (i in dependencies.indices) {
            for (j in i + 1 until dependencies.size) {
                val dep1 = dependencies[i]
                val dep2 = dependencies[j]
                
                if (!areCompatible(dep1.license, dep2.license)) {
                    conflicts.add(
                        LicenseConflict(
                            dependency1 = dep1,
                            dependency2 = dep2,
                            license1 = dep1.license,
                            license2 = dep2.license
                        )
                    )
                }
            }
        }
        
        return conflicts
    }
}

/**
 * Data class for dependency with license information
 */
data class DependencyWithLicense(
    val name: String,
    val version: String,
    val license: String,
    val ecosystem: String
)

/**
 * Data class for license conflict
 */
data class LicenseConflict(
    val dependency1: DependencyWithLicense,
    val dependency2: DependencyWithLicense,
    val license1: String,
    val license2: String
) {
    val severity: Severity get() = calculateSeverity()
    
    private fun calculateSeverity(): Severity {
        // Copyleft violations are critical
        if (License.isCopyleft(license1) && License.isCopyleft(license2)) {
            return Severity.CRITICAL
        }
        
        // Mixed copyleft/permissive is high
        if (License.isCopyleft(license1) || License.isCopyleft(license2)) {
            return Severity.HIGH
        }
        
        // Other combinations are medium
        return Severity.MEDIUM
    }
    
    fun getSummary(): String {
        return "License conflict: ${dependency1.name} (${license1}) vs ${dependency2.name} (${license2})"
    }
}

/**
 * Severity levels for license issues
 */
enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
