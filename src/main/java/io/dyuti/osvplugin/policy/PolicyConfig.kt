// Policy enforcement configuration models
package io.dyuti.osvplugin.policy

import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * Enforceable policy rules for dependency compliance.
 *
 * Organizations use policies to block bad dependencies at the CI/CD gate or
 * within the IDE before they are committed. Policies check:
 * - Severity thresholds (no CRITICAL vulnerabilities allowed)
 * - CVSS score caps (reject anything above 7.0)
 * - CISA KEV status (block known exploited vulnerabilities)
 * - Malicious package presence (block typosquats, backdoors)
 * - License compatibility (block GPL-3.0, etc.)
 * - Dependency age (block packages unmaintained > 2 years)
 *
 * ## Enforcement Modes
 *
 * - [FAIL]: Block commit / build. Shows error.
 * - [WARN]: Allow but show warning. Non-blocking.
 * - [IGNORE]: Don't check this policy at all.
 */
enum class EnforcementMode {
    FAIL, // Block — dependency must be fixed
    WARN, // Non-blocking — shows warning only
    IGNORE, // Skip this policy entirely
}

/**
 * Complete policy configuration for a project or organization.
 *
 * All policies default to [EnforcementMode.IGNORE] (opt-in).
 */
data class PolicyConfig(
    /** Maximum allowed severity. Anything higher is a violation. */
    val maxSeverity: OsVSeverity? = null,
    val maxSeverityMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Maximum CVSS score (0–10). Anything higher is a violation. */
    val maxCvssScore: Double? = null,
    val maxCvssMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Block CISA KEV entries (known exploited in the wild). */
    val blockCisaKev: Boolean = false,
    val blockCisaKevMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Block malicious packages (typosquatting, backdoor, etc.). */
    val blockMalicious: Boolean = false,
    val blockMaliciousMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Explicitly forbidden SPDX license identifiers. */
    val forbiddenLicenses: List<String> = emptyList(),
    val forbiddenLicenseMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Require explicit approval for these ecosystems. */
    val requiresApprovalFor: List<String> = emptyList(), // ["Maven", "npm"]
    /** Max dependency age in days (null = no limit). */
    val maxDependencyAgeDays: Int? = null,
    val maxDependencyAgeMode: EnforcementMode = EnforcementMode.IGNORE,
    /** Glob patterns for packages to ignore (e.g. internal packages). */
    val ignorePackages: List<String> = emptyList(), // ["com.mycompany:*"]
) {
    /**
     * Check if a package name matches any ignore pattern.
     * Supports glob-style wildcards: `*` matches any sequence.
     */
    fun isIgnored(packageName: String): Boolean =
        ignorePackages.any { pattern ->
            globMatch(pattern, packageName)
        }
}

/**
 * Result of evaluating a dependency against a policy.
 */
sealed class PolicyResult {
    /** All applicable policies passed (or were set to IGNORE). */
    data object Pass : PolicyResult()

    /** One or more WARN-mode policies were violated. */
    data class Warning(
        val violations: List<PolicyViolation>,
    ) : PolicyResult()

    /** One or more FAIL-mode policies were violated. Must be fixed before commit. */
    data class Fail(
        val violations: List<PolicyViolation>,
    ) : PolicyResult()
}

/**
 * A single policy violation with human-readable explanation.
 */
data class PolicyViolation(
    val rule: String, // e.g. "max-severity"
    val message: String, // e.g. "CRITICAL vulnerability found: CVE-2021-44228"
    val severity: OsVSeverity, // The severity of this violation itself
    val mode: EnforcementMode, // FAIL or WARN
)

/**
 * Simple glob matcher supporting `*` wildcards.
 */
private fun globMatch(
    pattern: String,
    text: String,
): Boolean {
    val regex =
        pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .toRegex()
    return regex.matches(text)
}
