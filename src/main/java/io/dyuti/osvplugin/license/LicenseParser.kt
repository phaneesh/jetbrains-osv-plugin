// SPDX License Scanner Integration - Parser
package io.dyuti.osvplugin.license

import io.dyuti.osvplugin.api.model.Dependency

/**
 * License parser for various package ecosystems.
 *
 * Delegates to [LicenseRegistryService] for real registry lookups,
 * falling back to heuristic name-based detection only when network
 * queries fail.
 */
object LicenseParser {
    private val registryService by lazy { LicenseRegistryService.getInstance() }

    /**
     * Parse license from dependency metadata.
     *
     * 1. Query the appropriate package registry (Maven Central, NPM, PyPI).
     * 2. If the registry lookup fails, fall back to name-based guessing.
     * 3. Normalize SPDX license expressions (e.g. splits "MIT OR Apache-2.0").
     */
    fun parseLicense(dependency: Dependency): String {
        // 1. Try real registry lookup
        val registryLicense = registryService.fetchLicense(dependency)
        if (registryLicense != "UNKNOWN") {
            return normalizeSpdx(registryLicense)
        }

        // 2. Fall back to name-based heuristic
        val guessed = guessLicenseFromName(dependency.name)
        if (guessed != "UNKNOWN") {
            return guessed
        }

        // 3. Return UNKNOWN if everything failed
        return "UNKNOWN"
    }

    /**
     * Normalize an SPDX expression.
     *
     * - Splits disjunctive (OR) expressions and returns the first
     *   permissive alternative (e.g. "MIT OR Apache-2.0" -> "MIT").
     * - Trims whitespace.
     */
    fun normalizeSpdx(spdxExpr: String): String {
        val trimmed = spdxExpr.trim()

        // Handle "OR" / "or" expressions: pick first alternative
        if (" OR " in trimmed || " or " in trimmed) {
            val alternatives = trimmed.split(" OR ", " or ")
            for (alt in alternatives) {
                val candidate = alt.trim()
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }
        }

        return trimmed
    }

    /**
     * Guess license from common patterns in the package name.
     * Used only when registry lookup fails.
     */
    private fun guessLicenseFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            "mit" in lower || "mit-license" in lower -> "MIT"
            "apache" in lower -> "Apache-2.0"
            "gpl" in lower -> detectGplVersion(lower)
            "lgpl" in lower -> "LGPL-3.0-only"
            "bsd" in lower -> "BSD-3-Clause"
            else -> "UNKNOWN"
        }
    }

    /**
     * Detect GPL version from name
     */
    private fun detectGplVersion(name: String): String =
        when {
            "gpl2" in name || "gpl-2" in name -> "GPL-2.0-only"
            "gpl3" in name || "gpl-3" in name -> "GPL-3.0-only"
            else -> "GPL-3.0-or-later"
        }

    /**
     * Parse license from common file contents
     */
    fun parseLicenseFromFile(content: String): List<String> {
        val licenses = mutableListOf<String>()

        // Try to find SPDX license expressions
        val spdxPattern = "SPDX-License-Identifier:\\s*([^\\n]+)".toRegex()
        val match = spdxPattern.find(content)
        if (match != null) {
            licenses.add(match.groupValues[1].trim())
        }

        // Look for license headers
        val licenseHeaders =
            listOf(
                "MIT License" to "MIT",
                "Apache License, Version 2.0" to "Apache-2.0",
                "GNU General Public License" to "GPL-3.0-or-later",
                "GNU Lesser General Public License" to "LGPL-3.0-only",
                "BSD License" to "BSD-3-Clause",
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
        val licensePattern = """"license"\s*:\s*"([^"]+)"""".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }

    /**
     * Extract license from package-lock.json
     */
    fun parseLockfileLicense(
        content: String,
        packageName: String,
    ): String {
        // Extract license from lockfile for specific package
        val pattern =
            """"$packageName"\s*\{[^}]*"license"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }

    /**
     * Extract license from requirements.txt (pip)
     */
    fun parsePipLicense(content: String): String {
        // Look for license specifier
        val licensePattern = "#\\s*License:\\s*(.+)\\n".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1)?.trim() ?: "UNKNOWN"
    }

    /**
     * Extract license from setup.py (pip)
     */
    fun parseSetupPyLicense(content: String): String {
        val licensePattern = """license\s*=\s*["']([^"']+)["']""".toRegex()
        val match = licensePattern.find(content)
        return match?.groupValues?.getOrNull(1) ?: "UNKNOWN"
    }
}
