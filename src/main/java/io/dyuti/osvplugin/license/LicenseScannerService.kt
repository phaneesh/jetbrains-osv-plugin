// SPDX License Scanner Integration - Service
package io.dyuti.osvplugin.license

import io.dyuti.osvplugin.api.model.Dependency

/**
 * License scanning service
 */
class LicenseScanner {
    
    private val conflictDetector = LicenseConflictDetector
    
    /**
     * Scan dependencies for license information
     */
    fun scanDependencies(dependencies: List<Dependency>): List<DependencyWithLicense> {
        return dependencies.map { dep ->
            val license = LicenseParser.parseLicense(dep)
            DependencyWithLicense(
                name = dep.name,
                version = dep.version,
                license = license,
                ecosystem = dep.ecosystem
            )
        }
    }
    
    /**
     * Check for license conflicts in dependencies
     */
    fun checkLicenseConflicts(dependencies: List<DependencyWithLicense>): List<LicenseConflict> {
        return conflictDetector.detectConflicts(dependencies)
    }
    
    /**
     * Check if a specific dependency has a compatible license
     */
    fun isLicenseCompatible(
        license: String,
        allowedLicenses: List<String>
    ): Boolean {
        if (allowedLicenses.isEmpty()) return true
        return allowedLicenses.contains(license)
    }
    
    /**
     * Get summary of license analysis
     */
    fun getLicenseSummary(dependencies: List<DependencyWithLicense>): LicenseSummary {
        val total = dependencies.size
        val knownLicenses = dependencies.filter { it.license != "UNKNOWN" }.size
        val unknownLicenses = total - knownLicenses
        val copyleftCount = dependencies.count { License.isCopyleft(it.license) }
        val permissiveCount = dependencies.count { License.isPermissive(it.license) }
        
        return LicenseSummary(
            totalDependencies = total,
            knownLicenses = knownLicenses,
            unknownLicenses = unknownLicenses,
            copyleftCount = copyleftCount,
            permissiveCount = permissiveCount,
            licenseDistribution = calculateLicenseDistribution(dependencies)
        )
    }
    
    /**
     * Calculate license distribution
     */
    private fun calculateLicenseDistribution(
        dependencies: List<DependencyWithLicense>
    ): Map<String, Int> {
        return dependencies.groupBy { it.license }
            .mapValues { it.value.size }
    }
}

/**
 * Data class for license summary
 */
data class LicenseSummary(
    val totalDependencies: Int,
    val knownLicenses: Int,
    val unknownLicenses: Int,
    val copyleftCount: Int,
    val permissiveCount: Int,
    val licenseDistribution: Map<String, Int>
) {
    val unknownPercentage: Double
        get() = if (totalDependencies > 0) {
            (unknownLicenses.toDouble() / totalDependencies) * 100
        } else 0.0
}
