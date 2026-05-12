// Tests for differential analysis — compare scan snapshots over time
package io.dyuti.osvplugin.diff

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiffAnalyzerTest {
    private fun dep(
        name: String,
        version: String,
        ecosystem: String = "Maven",
    ) = Dependency(name, version, ecosystem, "compile", false)

    private fun vuln(
        id: String,
        pkg: String,
        severity: OsVSeverity,
        cveId: String? = null,
    ) = Vulnerability(
        id = id,
        summary = "Test vuln",
        details = "",
        severity = severity,
        cvssScore = null,
        affectedVersions = emptyList(),
        fixedVersions = emptyList(),
        cveIds = listOfNotNull(cveId),
        references = emptyList(),
        cweIds = emptyList(),
        lineNumber = null,
        affectedFunctions = emptyList(),
    )

    @Test
    fun `identical scans produce no changes`() {
        val deps = listOf(dep("org:test", "1.0"))
        val vulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.HIGH))
        val baseline = DiffAnalyzer.createSnapshot("v1", deps, vulns)
        val current = DiffAnalyzer.createSnapshot("v2", deps, vulns)

        val diff = DiffAnalyzer.compare(baseline, current)
        assertEquals(0, diff.totalChanges)
        assertEquals(1, diff.unchangedCount)
        assertEquals("No changes — scans are identical", diff.summary())
        assertFalse(diff.hasActionableChanges())
    }

    @Test
    fun `detects new vulnerability`() {
        val deps = listOf(dep("org:test", "1.0"))
        val baselineVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.HIGH))
        val currentVulns =
            listOf(
                vuln("OSV-1", "org:test", OsVSeverity.HIGH),
                vuln("OSV-2", "org:test", OsVSeverity.CRITICAL, "CVE-2021-44228"),
            )

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", deps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", deps, currentVulns),
            )

        assertEquals(1, diff.newVulnerabilities.size)
        assertEquals("OSV-2", diff.newVulnerabilities[0].vulnerability.id)
        assertEquals(ChangeType.NEW, diff.newVulnerabilities[0].changeType)
        assertTrue(diff.hasActionableChanges())
    }

    @Test
    fun `detects resolved vulnerability`() {
        val deps = listOf(dep("org:test", "1.0"))
        val baselineVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.HIGH))
        val currentVulns = emptyList<Vulnerability>()

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", deps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", deps, currentVulns),
            )

        assertEquals(1, diff.resolvedVulnerabilities.size)
        assertEquals("OSV-1", diff.resolvedVulnerabilities[0].vulnerability.id)
        assertFalse(diff.hasActionableChanges())
    }

    @Test
    fun `detects severity escalation`() {
        val deps = listOf(dep("org:test", "1.0"))
        val baselineVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.MEDIUM))
        val currentVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.CRITICAL))

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", deps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", deps, currentVulns),
            )

        assertEquals(1, diff.severityChanges.size)
        val change = diff.severityChanges[0]
        assertEquals(OsVSeverity.MEDIUM, change.previousSeverity)
        assertEquals(OsVSeverity.CRITICAL, change.currentSeverity)
        assertTrue(change.isEscalation)
        assertFalse(change.isDeescalation)
        assertTrue(diff.hasActionableChanges())
    }

    @Test
    fun `detects severity deescalation`() {
        val deps = listOf(dep("org:test", "1.0"))
        val baselineVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.CRITICAL))
        val currentVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.LOW))

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", deps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", deps, currentVulns),
            )

        assertEquals(1, diff.severityChanges.size)
        val change = diff.severityChanges[0]
        assertTrue(change.isDeescalation)
        assertFalse(change.isEscalation)
        assertFalse(diff.hasActionableChanges())
    }

    @Test
    fun `detects new package`() {
        val baselineDeps = listOf(dep("org:a", "1.0"))
        val currentDeps = listOf(dep("org:a", "1.0"), dep("org:b", "2.0"))

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", baselineDeps, emptyList()),
                DiffAnalyzer.createSnapshot("v2", currentDeps, emptyList()),
            )

        assertEquals(1, diff.newPackages.size)
        assertEquals("org:b", diff.newPackages[0].name)
        assertEquals(0, diff.removedPackages.size)
    }

    @Test
    fun `detects removed package`() {
        val baselineDeps = listOf(dep("org:a", "1.0"), dep("org:b", "2.0"))
        val currentDeps = listOf(dep("org:a", "1.0"))

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", baselineDeps, emptyList()),
                DiffAnalyzer.createSnapshot("v2", currentDeps, emptyList()),
            )

        assertEquals(1, diff.removedPackages.size)
        assertEquals("org:b", diff.removedPackages[0].name)
    }

    @Test
    fun `multiple changes in one diff`() {
        val baselineDeps = listOf(dep("pkg:a", "1.0"), dep("pkg:b", "2.0"))
        val baselineVulns =
            listOf(
                vuln("OSV-1", "pkg:a", OsVSeverity.HIGH),
                vuln("OSV-2", "pkg:b", OsVSeverity.MEDIUM),
            )

        val currentDeps = listOf(dep("pkg:a", "1.0"), dep("pkg:c", "3.0"))
        val currentVulns =
            listOf(
                vuln("OSV-1", "pkg:a", OsVSeverity.CRITICAL), // escalated
                vuln("OSV-3", "pkg:c", OsVSeverity.LOW), // new
            )

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", baselineDeps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", currentDeps, currentVulns),
            )

        assertEquals(1, diff.severityChanges.size) // OSV-1 escalated
        assertEquals(1, diff.newVulnerabilities.size) // OSV-3
        assertEquals(1, diff.resolvedVulnerabilities.size) // OSV-2 removed
        assertEquals(1, diff.newPackages.size) // pkg:c
        assertEquals(1, diff.removedPackages.size) // pkg:b
        assertEquals(5, diff.totalChanges)
        assertTrue(diff.hasActionableChanges())
    }

    @Test
    fun `filterByMinSeverity removes low severity changes`() {
        val deps = listOf(dep("org:test", "1.0"))
        val baselineVulns = listOf(vuln("OSV-1", "org:test", OsVSeverity.LOW))
        val currentVulns =
            listOf(
                vuln("OSV-1", "org:test", OsVSeverity.LOW),
                vuln("OSV-2", "org:test", OsVSeverity.HIGH),
            )

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", deps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", deps, currentVulns),
            )

        val filtered = diff.filterByMinSeverity(OsVSeverity.HIGH)
        assertEquals(1, filtered.newVulnerabilities.size)
        assertEquals(0, filtered.resolvedVulnerabilities.size)
    }

    @Test
    fun `diff summary includes all change types`() {
        val baselineDeps = listOf(dep("pkg:a", "1.0"))
        val baselineVulns = emptyList<Vulnerability>()
        val currentDeps = listOf(dep("pkg:a", "1.0"), dep("pkg:b", "2.0"))
        val currentVulns = listOf(vuln("OSV-1", "pkg:a", OsVSeverity.HIGH))

        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("v1", baselineDeps, baselineVulns),
                DiffAnalyzer.createSnapshot("v2", currentDeps, currentVulns),
            )

        val summary = diff.summary()
        assertTrue(summary.contains("1 new"))
        assertTrue(summary.contains("1 new packages"))
    }

    @Test
    fun `snapshot preserves metadata`() {
        val snapshot =
            DiffAnalyzer.createSnapshot(
                id = "build-42",
                dependencies = listOf(dep("test", "1.0")),
                vulnerabilities = emptyList(),
            )
        assertEquals("build-42", snapshot.id)
        assertTrue(snapshot.timestamp > 0)
    }

    @Test
    fun `empty scans produce zero changes`() {
        val diff =
            DiffAnalyzer.compare(
                DiffAnalyzer.createSnapshot("empty1", emptyList(), emptyList()),
                DiffAnalyzer.createSnapshot("empty2", emptyList(), emptyList()),
            )
        assertEquals(0, diff.totalChanges)
    }
}
