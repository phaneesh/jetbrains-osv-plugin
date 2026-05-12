package io.dyuti.osvplugin.historical

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HistoricalScanRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private val repo by lazy { HistoricalScanRepository(tempDir) }

    private fun mockVuln(
        id: String,
        sev: OsVSeverity,
    ) = Vulnerability(
        id = id,
        cveIds = emptyList(),
        summary = "Summary for $id",
        details = "",
        severity = sev,
        cvssScore = null,
        affectedVersions = emptyList(),
        fixedVersions = emptyList(),
        references = emptyList(),
        cweIds = emptyList(),
    )

    private fun mockDep() =
        Dependency(
            "test:pkg",
            "1.0",
            "Maven",
            "compile",
            false,
        )

    // ─── save / load ────────────────────────────────────────────────

    @Test
    fun `save and load single record`() {
        val dep = mockDep()
        val vuln = mockVuln("OSV-1", OsVSeverity.HIGH)
        val record = repo.saveScan("/test", listOf(vuln), listOf(dep))

        val loaded = repo.loadAllRecords()
        assertEquals(1, loaded.size)
        assertEquals(record.id, loaded[0].id)
        assertEquals(1, loaded[0].totalVulnerabilities)
        assertEquals(1, loaded[0].dependencyCount)
        assertEquals(1, loaded[0].countFor(OsVSeverity.HIGH))
    }

    @Test
    fun `save and load multiple records sorted by timestamp`() {
        val dep = mockDep()
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(dep))
        Thread.sleep(10)
        repo.saveScan("/test", listOf(mockVuln("OSV-2", OsVSeverity.CRITICAL)), listOf(dep))
        Thread.sleep(10)
        repo.saveScan("/test", listOf(mockVuln("OSV-3", OsVSeverity.MEDIUM)), listOf(dep))

        val loaded = repo.loadAllRecords()
        assertEquals(3, loaded.size)
        // Should be sorted oldest first
        assertEquals(OsVSeverity.HIGH, loaded[0].severityCounts.keys.first())
        assertEquals(OsVSeverity.MEDIUM, loaded[2].severityCounts.keys.first())
    }

    @Test
    fun `loadAllRecords returns empty when no history exists`() {
        val loaded = repo.loadAllRecords()
        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `recordCount is zero initially`() {
        assertEquals(0, repo.recordCount())
    }

    @Test
    fun `recordCount after saves`() {
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(mockDep()))
        repo.saveScan("/test", listOf(mockVuln("OSV-2", OsVSeverity.MEDIUM)), listOf(mockDep()))
        assertEquals(2, repo.recordCount())
    }

    // ─── project filtering ─────────────────────────────────────────

    @Test
    fun `loadRecordsForProject filters by path`() {
        val dep = mockDep()
        repo.saveScan("/proj/a", listOf(mockVuln("OSV-A1", OsVSeverity.HIGH)), listOf(dep))
        repo.saveScan("/proj/b", listOf(mockVuln("OSV-B1", OsVSeverity.LOW)), listOf(dep))
        repo.saveScan("/proj/a", listOf(mockVuln("OSV-A2", OsVSeverity.CRITICAL)), listOf(dep))

        val forA = repo.loadRecordsForProject("/proj/a")
        assertEquals(2, forA.size)
        assertTrue(forA.all { it.projectPath == "/proj/a" })

        val forB = repo.loadRecordsForProject("/proj/b")
        assertEquals(1, forB.size)
        assertEquals("/proj/b", forB[0].projectPath)
    }

    // ─── latest N ─────────────────────────────────────────────────

    @Test
    fun `loadLatest returns most recent first`() {
        val dep = mockDep()
        repeat(5) { i ->
            repo.saveScan("/test", listOf(mockVuln("OSV-$i", OsVSeverity.HIGH)), listOf(dep))
            Thread.sleep(5)
        }

        val latest = repo.loadLatest(2)
        assertEquals(2, latest.size)
        assertTrue(latest[0].timestamp >= latest[1].timestamp)
    }

    // ─── delta computation ────────────────────────────────────────

    @Test
    fun `computeLatestDelta returns null with fewer than 2 records`() {
        assertNull(repo.computeLatestDelta())

        repo.saveScan("/test", emptyList<Vulnerability>(), listOf(mockDep()))
        assertNull(repo.computeLatestDelta())
    }

    @Test
    fun `computeLatestDelta with vuln increase shows degradation`() {
        val dep = mockDep()
        repo.saveScan("/test", emptyList<Vulnerability>(), listOf(dep))
        Thread.sleep(10)
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(dep))

        val delta = repo.computeLatestDelta()!!
        assertEquals(1, delta.totalVulnChange)
        assertTrue(delta.isDegrading)
        assertFalse(delta.isImproving)
    }

    @Test
    fun `computeLatestDelta with vuln decrease shows improvement`() {
        val dep = mockDep()
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(dep))
        Thread.sleep(10)
        repo.saveScan("/test", emptyList<Vulnerability>(), listOf(dep))

        val delta = repo.computeLatestDelta()!!
        assertEquals(-1, delta.totalVulnChange)
        assertTrue(delta.isImproving)
        assertFalse(delta.isDegrading)
    }

    @Test
    fun `computeLatestDelta tracks severity-level changes`() {
        val dep = mockDep()
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(dep))
        Thread.sleep(10)
        repo.saveScan(
            "/test",
            listOf(mockVuln("OSV-1", OsVSeverity.CRITICAL), mockVuln("OSV-2", OsVSeverity.LOW)),
            listOf(dep),
        )

        val delta = repo.computeLatestDelta()!!
        assertEquals(1, delta.totalVulnChange)
        assertEquals(1, delta.criticalChange)
        assertEquals(-1, delta.highChange)
    }

    // ─── trend summary ────────────────────────────────────────────

    @Test
    fun `computeSummary with empty history`() {
        val summary = repo.computeSummary("/test")
        assertEquals(0, summary.recordCount)
        assertNull(summary.latestRecord)
        assertNull(summary.oldestRecord)
        assertNull(summary.latestDelta)
    }

    @Test
    fun `computeSummary computes windows correctly`() {
        val dep = mockDep()
        repeat(10) { i ->
            val count = if (i < 5) 2 else 5
            repo.saveScan(
                "/test",
                List(count) { idx -> mockVuln("OSV-$i-$idx", OsVSeverity.HIGH) },
                listOf(dep),
            )
            Thread.sleep(5)
        }

        val summary = repo.computeSummary("/test")
        assertEquals(10, summary.recordCount)
        assertNotNull(summary.latestRecord)
        assertNotNull(summary.latestDelta)
        assertEquals(10, summary.allTimeWindow.records.size)
        assertEquals(7, summary.window7.records.size)
        assertEquals(10, summary.window30.records.size) // only 10 records total
    }

    // ─── trim / clear ─────────────────────────────────────────────

    @Test
    fun `trimToMostRecent removes old records`() {
        val dep = mockDep()
        repeat(10) { i ->
            repo.saveScan("/test", listOf(mockVuln("OSV-$i", OsVSeverity.HIGH)), listOf(dep))
            Thread.sleep(3)
        }
        assertEquals(10, repo.recordCount())

        repo.trimToMostRecent(5)
        assertEquals(5, repo.recordCount())

        val loaded = repo.loadAllRecords()
        assertEquals(5, loaded.size)
        // The remaining records should be the newest (highest timestamps)
        assertTrue(loaded.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
    }

    @Test
    fun `trimToMostRecent is no-op when N larger than count`() {
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(mockDep()))
        repo.trimToMostRecent(100)
        assertEquals(1, repo.recordCount())
    }

    @Test
    fun `clearAll removes everything`() {
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(mockDep()))
        repo.saveScan("/test", listOf(mockVuln("OSV-2", OsVSeverity.MEDIUM)), listOf(mockDep()))
        assertEquals(2, repo.recordCount())

        repo.clearAll()
        assertEquals(0, repo.recordCount())
        assertTrue(repo.loadAllRecords().isEmpty())
    }

    @Test
    fun `corrupted json files are skipped gracefully`() {
        repo.saveScan("/test", listOf(mockVuln("OSV-1", OsVSeverity.HIGH)), listOf(mockDep()))
        // Write a corrupted file
        File(tempDir, "scan-history/corrupted.json").writeText("not json")

        val loaded = repo.loadAllRecords()
        assertEquals(1, loaded.size)
        assertEquals("OSV-1", mockVulnIdFromRecord(loaded[0]))
    }

    private fun mockVulnIdFromRecord(r: HistoricalScanRecord): String {
        // Not easily accessible since record only stores counts, not IDs.
        // Just verify structure.
        return if (r.totalVulnerabilities > 0) "OSV-1" else ""
    }

    // ─── edge cases ───────────────────────────────────────────────

    @Test
    fun `fromScanResult computes correct severity counts`() {
        val vulns =
            listOf(
                mockVuln("OSV-1", OsVSeverity.CRITICAL),
                mockVuln("OSV-2", OsVSeverity.CRITICAL),
                mockVuln("OSV-3", OsVSeverity.HIGH),
                mockVuln("OSV-4", OsVSeverity.MEDIUM),
            )
        val dep = mockDep()
        val record = HistoricalScanRecord.fromScanResult("/test", vulns, listOf(dep))

        assertEquals(4, record.totalVulnerabilities)
        assertEquals(2, record.countFor(OsVSeverity.CRITICAL))
        assertEquals(1, record.countFor(OsVSeverity.HIGH))
        assertEquals(1, record.countFor(OsVSeverity.MEDIUM))
        assertEquals(0, record.countFor(OsVSeverity.LOW))
        assertEquals(4, record.totalBySeverity)
    }

    @Test
    fun `trend window handles empty records`() {
        val window = TrendWindow(emptyList(), 7)
        assertEquals(0.0, window.avgTotalVulnerabilities, 0.001)
        assertEquals(0, window.peakTotal)
        assertEquals(TrendDirection.STABLE, window.direction)
    }

    @Test
    fun `trend window computes averages correctly`() {
        val records =
            listOf(
                HistoricalScanRecord("1", 1L, "/test", 10, mapOf(OsVSeverity.HIGH to 10), 5),
                HistoricalScanRecord("2", 2L, "/test", 20, mapOf(OsVSeverity.HIGH to 10, OsVSeverity.CRITICAL to 10), 5),
            )
        val window = TrendWindow(records, 7)
        assertEquals(15.0, window.avgTotalVulnerabilities, 0.001)
        assertEquals(20, window.peakTotal)
        assertEquals(10, window.minTotal)
        assertEquals(5.0, window.avgCritical, 0.001) // (0 + 10) / 2 = 5
        assertEquals(10.0, window.avgHigh, 0.001) // 10 + 10 / 2
    }
}
