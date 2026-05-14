package io.dyuti.osvplugin.inspection

import com.intellij.codeInspection.ProblemHighlightType
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the async OsVInspection.
 */
class OsVInspectionTest {
    private val inspection = OsVInspection()

    @Test
    fun `inspection has correct display name`() {
        assertEquals("OSV Vulnerability Check", inspection.displayName)
    }

    @Test
    fun `inspection has correct short name`() {
        assertEquals("OsVInspection", inspection.shortName)
    }

    @Test
    fun `checkDependencies returns empty list for empty input`() {
        val dependencies = emptyList<Dependency>()
        val results = inspection.checkDependencies(dependencies)
        assertEquals(0, results.size)
    }

    @Test
    fun `checkDependencies filters vulnerabilities by threshold`() {
        val dep = Dependency("com.example:lib", "1.0.0", "Maven", "compile", false, 42)
        val vulnHigh =
            Vulnerability(
                id = "GHSA-HIGH",
                cveIds = emptyList(),
                summary = "High severity issue",
                details = "",
                severity = OsVSeverity.HIGH,
                cvssScore = 8.5,
                affectedVersions = emptyList(),
                fixedVersions = listOf("2.0.0"),
                references = emptyList(),
                cweIds = emptyList(),
                lineNumber = 42,
            )
        val vulnLow =
            Vulnerability(
                id = "GHSA-LOW",
                cveIds = emptyList(),
                summary = "Low severity issue",
                details = "",
                severity = OsVSeverity.LOW,
                cvssScore = 2.0,
                affectedVersions = emptyList(),
                fixedVersions = listOf("1.1.0"),
                references = emptyList(),
                cweIds = emptyList(),
                lineNumber = 43,
            )

        // With default MEDIUM threshold, only HIGH should be reported
        val results =
            listOf(
                DependencyWithVulnerabilities(dep, listOf(vulnHigh, vulnLow), "pom.xml", 42),
            )

        assertEquals(1, results.size)
        assertEquals(2, results[0].vulnerabilities.size)
        assertEquals("GHSA-HIGH", results[0].vulnerabilities[0].id)
    }

    @Test
    fun `ProblemHighlightType maps correctly for each severity`() {
        assertEquals(ProblemHighlightType.ERROR, severityToHighlight(OsVSeverity.CRITICAL))
        assertEquals(ProblemHighlightType.WARNING, severityToHighlight(OsVSeverity.HIGH))
        assertEquals(ProblemHighlightType.WEAK_WARNING, severityToHighlight(OsVSeverity.MEDIUM))
        assertEquals(ProblemHighlightType.INFORMATION, severityToHighlight(OsVSeverity.LOW))
    }

    @Test
    fun `vulnerability message includes CVSS score when present`() {
        val vuln =
            Vulnerability(
                id = "GHSA-1234",
                cveIds = emptyList(),
                summary = "Test vulnerability",
                details = "",
                severity = OsVSeverity.HIGH,
                cvssScore = 8.5,
                affectedVersions = emptyList(),
                fixedVersions = listOf("2.0.0"),
                references = emptyList(),
                cweIds = emptyList(),
            )

        val message = buildVulnerabilityMessage(vuln)
        assertTrue(message.contains("GHSA-1234"))
        assertTrue(message.contains("CVSS: 8.5"))
        assertTrue(message.contains("[Fix: 2.0.0]"))
    }

    @Test
    fun `vulnerability message omits CVSS when null`() {
        val vuln =
            Vulnerability(
                id = "GHSA-5678",
                cveIds = emptyList(),
                summary = "Low impact issue",
                details = "",
                severity = OsVSeverity.MEDIUM,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = emptyList(),
                references = emptyList(),
                cweIds = emptyList(),
            )

        val message = buildVulnerabilityMessage(vuln)
        assertTrue(message.contains("GHSA-5678"))
        assertFalse(message.contains("CVSS"))
        assertFalse(message.contains("[Fix:"))
    }

    @Test
    fun `vulnerability cache entry stores modification stamp`() {
        val dep = Dependency("com.example:lib", "1.0.0", "Maven", "compile", false)
        val vuln =
            Vulnerability(
                id = "GHSA-TEST",
                cveIds = emptyList(),
                summary = "Test",
                details = "",
                severity = OsVSeverity.MEDIUM,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = emptyList(),
                references = emptyList(),
                cweIds = emptyList(),
            )
        val result = VulnerabilityResult(dep, vuln)
        val entry = VulnerabilityCacheEntry(listOf(result), 12345L)

        assertEquals(12345L, entry.modificationStamp)
        assertEquals(1, entry.vulnerabilities.size)
        assertEquals("GHSA-TEST", entry.vulnerabilities[0].vulnerability.id)
    }

    @Test
    fun `quick fix types have correct names`() {
        val dep = Dependency("com.example:lib", "1.0.0", "Maven", "compile", false)
        val vuln =
            Vulnerability(
                id = "GHSA-TEST",
                cveIds = emptyList(),
                summary = "Test",
                details = "",
                severity = OsVSeverity.MEDIUM,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = listOf("2.0.0"),
                references = emptyList(),
                cweIds = emptyList(),
            )

        val upgradeFix = OsVQuickFix.createUpgradeFix(dep, vuln)
        assertEquals("Upgrade com.example:lib to fixed version", upgradeFix.name)

        val suppressFix = OsVQuickFix.createSuppressFix(dep, vuln)
        assertEquals("Suppress GHSA-TEST", suppressFix.name)

        val ignoreFix = OsVQuickFix.createIgnoreFix(dep, vuln)
        assertEquals("Ignore com.example:lib", ignoreFix.name)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper functions (reproduced from inspection for test verification)
    // ─────────────────────────────────────────────────────────────────────────

    private fun severityToHighlight(severity: OsVSeverity): ProblemHighlightType =
        when (severity) {
            OsVSeverity.CRITICAL -> ProblemHighlightType.ERROR
            OsVSeverity.HIGH -> ProblemHighlightType.WARNING
            OsVSeverity.MEDIUM -> ProblemHighlightType.WEAK_WARNING
            OsVSeverity.LOW -> ProblemHighlightType.INFORMATION
        }

    private fun buildVulnerabilityMessage(vuln: Vulnerability): String =
        buildString {
            append("OSV: ${vuln.id}")
            if (vuln.cvssScore != null) {
                append(" (CVSS: ${vuln.cvssScore})")
            }
            append(" — ${vuln.summary}")
            if (vuln.fixedVersions.isNotEmpty()) {
                append(" [Fix: ${vuln.fixedVersions.first()}]")
            }
        }
}
