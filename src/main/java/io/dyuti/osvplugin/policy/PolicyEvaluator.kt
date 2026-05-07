// Policy evaluation engine — checks dependencies against organizational rules
package io.dyuti.osvplugin.policy

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.risk.RiskAssessment
import io.dyuti.osvplugin.risk.RiskLevel

/**
 * Evaluates dependencies against a [PolicyConfig] and produces a [PolicyResult].
 *
 * The evaluator runs all applicable policies against a dependency + its
 * vulnerability data, then returns a combined result:
 * - [PolicyResult.Pass] if no violations
 * - [PolicyResult.Warning] if only WARN-mode violations
 * - [PolicyResult.Fail] if any FAIL-mode violation exists
 *
 * ## Usage
 *
 * ```kotlin
 * val config = PolicyConfig(
 *     maxSeverity = OsVSeverity.HIGH,
 *     maxSeverityMode = EnforcementMode.FAIL,
 *     blockCisaKev = true,
 *     blockCisaKevMode = EnforcementMode.FAIL,
 *     forbiddenLicenses = listOf("GPL-3.0", "AGPL-3.0"),
 *     forbiddenLicenseMode = EnforcementMode.WARN,
 * )
 *
 * val result = PolicyEvaluator.evaluate(dependency, vulnerabilities, riskAssessments, config)
 * when (result) {
 *     is PolicyResult.Pass -> allowBuild()
 *     is PolicyResult.Warning -> showWarnings(result.violations)
 *     is PolicyResult.Fail -> blockBuild(result.violations)
 * }
 * ```
 */
class PolicyEvaluator {
    companion object {
        /**
         * Evaluate a single dependency against all policies.
         *
         * @param packageName The dependency name (e.g. `org.example:lib`)
         * @param license The detected SPDX license identifier (may be null)
         * @param vulnerabilities Vulnerabilities found for this dependency
         * @param riskAssessments Risk assessments by CVE (from [RiskScoringService])
         * @param config The policy configuration to enforce
         * @return Combined [PolicyResult]
         */
        @JvmStatic
        fun evaluate(
            packageName: String,
            license: String?,
            vulnerabilities: List<Vulnerability>,
            riskAssessments: Map<String, RiskAssessment>,
            config: PolicyConfig,
        ): PolicyResult {
            if (config.isIgnored(packageName)) {
                return PolicyResult.Pass
            }

            val violations = mutableListOf<PolicyViolation>()

            // 1. Max severity check
            if (config.maxSeverityMode != EnforcementMode.IGNORE && config.maxSeverity != null) {
                val maxVulnSeverity = vulnerabilities.map { it.severity }.maxByOrNull { it.ordinal }
                if (maxVulnSeverity != null && maxVulnSeverity.ordinal < config.maxSeverity.ordinal) {
                    violations.add(
                        PolicyViolation(
                            rule = "max-severity",
                            message = "Severity $maxVulnSeverity exceeds maximum allowed ${config.maxSeverity} in $packageName",
                            severity = maxVulnSeverity,
                            mode = config.maxSeverityMode,
                        ),
                    )
                }
            }

            // 2. Max CVSS score check
            if (config.maxCvssMode != EnforcementMode.IGNORE && config.maxCvssScore != null) {
                val maxCvss = vulnerabilities.mapNotNull { it.cvssScore }.maxOrNull()
                if (maxCvss != null && maxCvss > config.maxCvssScore) {
                    val cvssSeverity =
                        when {
                            maxCvss >= 9.0 -> OsVSeverity.CRITICAL
                            maxCvss >= 7.0 -> OsVSeverity.HIGH
                            maxCvss >= 4.0 -> OsVSeverity.MEDIUM
                            else -> OsVSeverity.LOW
                        }
                    violations.add(
                        PolicyViolation(
                            rule = "max-cvss",
                            message = "CVSS score %.1f exceeds maximum %.1f in $packageName".format(maxCvss, config.maxCvssScore),
                            severity = cvssSeverity,
                            mode = config.maxCvssMode,
                        ),
                    )
                }
            }

            // 3. CISA KEV check
            if (config.blockCisaKevMode != EnforcementMode.IGNORE && config.blockCisaKev) {
                val kevVulns = riskAssessments.values.filter { it.isCisaKev }
                if (kevVulns.isNotEmpty()) {
                    kevVulns.forEach { assessment ->
                        violations.add(
                            PolicyViolation(
                                rule = "cisa-kev",
                                message = "CISA Known Exploited Vulnerability: ${assessment.cveId} in $packageName",
                                severity = OsVSeverity.CRITICAL,
                                mode = config.blockCisaKevMode,
                            ),
                        )
                    }
                }
            }

            // 4. License check
            if (config.forbiddenLicenseMode != EnforcementMode.IGNORE &&
                config.forbiddenLicenses.isNotEmpty() &&
                license != null
            ) {
                val normalizedLicense = license.trim().uppercase()
                val matched =
                    config.forbiddenLicenses.find { forbidden ->
                        normalizedLicense.contains(forbidden.uppercase()) ||
                            forbidden.uppercase().contains(normalizedLicense)
                    }
                if (matched != null) {
                    violations.add(
                        PolicyViolation(
                            rule = "forbidden-license",
                            message = "Forbidden license '$license' (matches '$matched') in $packageName",
                            severity = OsVSeverity.MEDIUM,
                            mode = config.forbiddenLicenseMode,
                        ),
                    )
                }
            }

            // 5. Requires approval check
            // This is a metadata marker — it doesn't produce a violation but
            // flags the dependency for manual review. Handled by the caller.

            return when {
                violations.any { it.mode == EnforcementMode.FAIL } -> PolicyResult.Fail(violations)
                violations.isNotEmpty() -> PolicyResult.Warning(violations)
                else -> PolicyResult.Pass
            }
        }

        /**
         * Evaluate multiple dependencies in batch.
         */
        @JvmStatic
        fun evaluateAll(
            dependencies: List<DependencyContext>,
            config: PolicyConfig,
        ): Map<String, PolicyResult> =
            dependencies.associate { ctx ->
                ctx.packageName to
                    evaluate(
                        ctx.packageName,
                        ctx.license,
                        ctx.vulnerabilities,
                        ctx.riskAssessments,
                        config,
                    )
            }
    }
}

/**
 * Context object for batch policy evaluation.
 */
data class DependencyContext(
    val packageName: String,
    val license: String?,
    val vulnerabilities: List<Vulnerability>,
    val riskAssessments: Map<String, RiskAssessment>,
)
