// Tests for policy enforcement — pure functions, no IntelliJ dependencies
package io.dyuti.osvplugin.policy

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.risk.RiskAssessment
import io.dyuti.osvplugin.risk.RiskLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PolicyEvaluatorTest {
    private fun vuln(
        severity: OsVSeverity,
        cvss: Double? = null,
        cveIds: List<String> = emptyList(),
    ) = Vulnerability(
        id = "OSV-TEST",
        summary = "Test vulnerability",
        details = "Test details",
        severity = severity,
        cvssScore = cvss,
        affectedVersions = emptyList(),
        fixedVersions = emptyList(),
        cveIds = cveIds,
        references = emptyList(),
        cweIds = emptyList(),
        lineNumber = null,
        affectedFunctions = emptyList(),
    )

    private fun risk(
        cveId: String,
        isKev: Boolean = false,
    ) = RiskAssessment(
        cveId = cveId,
        cvssScore = null,
        epssScore = null,
        epssPercentile = null,
        isCisaKev = isKev,
        hasPublicExploit = false,
        compositeScore = 50.0,
        riskLevel = RiskLevel.MEDIUM,
    )

    @Test
    fun `passes when no policies configured`() {
        val config = PolicyConfig()
        val result = PolicyEvaluator.evaluate("org:test", "MIT", emptyList(), emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `fails when severity exceeds threshold`() {
        val config = PolicyConfig(maxSeverity = OsVSeverity.HIGH, maxSeverityMode = EnforcementMode.FAIL)
        val vulns = listOf(vuln(OsVSeverity.CRITICAL))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)

        assertTrue(result is PolicyResult.Fail)
        val fail = result as PolicyResult.Fail
        assertEquals("max-severity", fail.violations.first().rule)
        assertTrue(
            fail.violations
                .first()
                .message
                .contains("CRITICAL"),
        )
    }

    @Test
    fun `warns when severity exceeds threshold in warn mode`() {
        val config = PolicyConfig(maxSeverity = OsVSeverity.MEDIUM, maxSeverityMode = EnforcementMode.WARN)
        val vulns = listOf(vuln(OsVSeverity.HIGH))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)

        assertTrue(result is PolicyResult.Warning)
        val warn = result as PolicyResult.Warning
        assertEquals("max-severity", warn.violations.first().rule)
    }

    @Test
    fun `ignores severity when mode is ignore`() {
        val config = PolicyConfig(maxSeverity = OsVSeverity.LOW, maxSeverityMode = EnforcementMode.IGNORE)
        val vulns = listOf(vuln(OsVSeverity.CRITICAL))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `fails when cvss exceeds threshold`() {
        val config = PolicyConfig(maxCvssScore = 5.0, maxCvssMode = EnforcementMode.FAIL)
        val vulns = listOf(vuln(OsVSeverity.CRITICAL, cvss = 9.0))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)

        assertTrue(result is PolicyResult.Fail)
        assertTrue(
            (result as PolicyResult.Fail)
                .violations
                .first()
                .message
                .contains("9.0"),
        )
    }

    @Test
    fun `passes when cvss below threshold`() {
        val config = PolicyConfig(maxCvssScore = 7.0, maxCvssMode = EnforcementMode.FAIL)
        val vulns = listOf(vuln(OsVSeverity.MEDIUM, cvss = 5.0))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `fails when cisa kev detected`() {
        val config = PolicyConfig(blockCisaKev = true, blockCisaKevMode = EnforcementMode.FAIL)
        val risks = mapOf("CVE-2021-44228" to risk("CVE-2021-44228", isKev = true))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", emptyList(), risks, config)

        assertTrue(result is PolicyResult.Fail)
        assertTrue(
            (result as PolicyResult.Fail)
                .violations
                .first()
                .message
                .contains("CISA Known Exploited"),
        )
    }

    @Test
    fun `passes when cisa kev not detected`() {
        val config = PolicyConfig(blockCisaKev = true, blockCisaKevMode = EnforcementMode.FAIL)
        val risks = mapOf("CVE-2021-44228" to risk("CVE-2021-44228", isKev = false))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", emptyList(), risks, config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `warns on forbidden license`() {
        val config = PolicyConfig(forbiddenLicenses = listOf("GPL-3.0"), forbiddenLicenseMode = EnforcementMode.WARN)
        val result = PolicyEvaluator.evaluate("org:test", "GPL-3.0", emptyList(), emptyMap(), config)

        assertTrue(result is PolicyResult.Warning)
        assertTrue(
            (result as PolicyResult.Warning)
                .violations
                .first()
                .message
                .contains("GPL-3.0"),
        )
    }

    @Test
    fun `fails on forbidden license in fail mode`() {
        val config = PolicyConfig(forbiddenLicenses = listOf("GPL-3.0"), forbiddenLicenseMode = EnforcementMode.FAIL)
        val result = PolicyEvaluator.evaluate("org:test", "GPL-3.0", emptyList(), emptyMap(), config)
        assertTrue(result is PolicyResult.Fail)
    }

    @Test
    fun `ignores package matching glob`() {
        val config =
            PolicyConfig(
                maxSeverity = OsVSeverity.LOW,
                maxSeverityMode = EnforcementMode.FAIL,
                ignorePackages = listOf("com.internal:*"),
            )
        val vulns = listOf(vuln(OsVSeverity.CRITICAL))
        val result = PolicyEvaluator.evaluate("com.internal:secret", "MIT", vulns, emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `does not ignore non-matching package`() {
        val config =
            PolicyConfig(
                maxSeverity = OsVSeverity.LOW,
                maxSeverityMode = EnforcementMode.FAIL,
                ignorePackages = listOf("com.internal:*"),
            )
        val vulns = listOf(vuln(OsVSeverity.CRITICAL))
        val result = PolicyEvaluator.evaluate("org.test:lib", "MIT", vulns, emptyMap(), config)
        assertTrue(result is PolicyResult.Fail)
    }

    @Test
    fun `mixed violations return fail when any is fail`() {
        val config =
            PolicyConfig(
                maxSeverity = OsVSeverity.LOW,
                maxSeverityMode = EnforcementMode.WARN,
                blockCisaKev = true,
                blockCisaKevMode = EnforcementMode.FAIL,
            )
        val vulns = listOf(vuln(OsVSeverity.CRITICAL))
        val risks = mapOf("CVE-2021-44228" to risk("CVE-2021-44228", isKev = true))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, risks, config)

        assertTrue(result is PolicyResult.Fail)
        val fail = result as PolicyResult.Fail
        assertEquals(2, fail.violations.size)
    }

    @Test
    fun `batch evaluation evaluates all dependencies`() {
        val config = PolicyConfig(maxSeverity = OsVSeverity.LOW, maxSeverityMode = EnforcementMode.FAIL)
        val deps =
            listOf(
                DependencyContext("good-pkg", "MIT", emptyList(), emptyMap()),
                DependencyContext("bad-pkg", "MIT", listOf(vuln(OsVSeverity.CRITICAL)), emptyMap()),
            )
        val results = PolicyEvaluator.evaluateAll(deps, config)
        assertTrue(results["good-pkg"] is PolicyResult.Pass)
        assertTrue(results["bad-pkg"] is PolicyResult.Fail)
    }

    @Test
    fun `null license does not trigger license violation`() {
        val config = PolicyConfig(forbiddenLicenses = listOf("GPL-3.0"), forbiddenLicenseMode = EnforcementMode.FAIL)
        val result = PolicyEvaluator.evaluate("org:test", null, emptyList(), emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }

    @Test
    fun `severity threshold exact match passes`() {
        val config = PolicyConfig(maxSeverity = OsVSeverity.HIGH, maxSeverityMode = EnforcementMode.FAIL)
        val vulns = listOf(vuln(OsVSeverity.HIGH))
        val result = PolicyEvaluator.evaluate("org:test", "MIT", vulns, emptyMap(), config)
        assertTrue(result is PolicyResult.Pass)
    }
}
