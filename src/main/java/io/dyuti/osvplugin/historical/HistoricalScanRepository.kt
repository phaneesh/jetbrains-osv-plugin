// Historical scan persistence and trend computation
package io.dyuti.osvplugin.historical

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Persists [HistoricalScanRecord]s to disk as JSON and computes rolling trends.
 *
 * Stores records in `{baseDir}/scan-history/` as one JSON file per record.
 * No IntelliJ dependencies — fully unit-testable.
 */
class HistoricalScanRepository(
    private val baseDir: File,
) {
    private val historyDir: File by lazy {
        File(baseDir, "scan-history").also { it.mkdirs() }
    }

    private val json = HistoricalJsonSerializer

    /** Persist a new scan record to disk. */
    fun saveRecord(record: HistoricalScanRecord) {
        val file = File(historyDir, "${record.id}.json")
        file.writeText(json.serialize(record))
    }

    /** Convenience: save from scan results. */
    fun saveScan(
        projectPath: String,
        vulnerabilities: List<Vulnerability>,
        dependencies: List<Dependency>,
        durationMs: Long? = null,
    ): HistoricalScanRecord {
        val record = HistoricalScanRecord.fromScanResult(projectPath, vulnerabilities, dependencies, durationMs)
        saveRecord(record)
        return record
    }

    /** Load all persisted records, sorted by timestamp (oldest first). */
    fun loadAllRecords(): List<HistoricalScanRecord> {
        if (!historyDir.exists()) return emptyList()
        return historyDir
            .listFiles { f -> f.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    json.deserialize(file.readText())
                } catch (_: Exception) {
                    null
                }
            }?.sortedBy { it.timestamp }
            ?: emptyList()
    }

    /** Load records for a specific project (by path prefix match). */
    fun loadRecordsForProject(projectPath: String): List<HistoricalScanRecord> = loadAllRecords().filter { it.projectPath == projectPath }

    /** Load latest N records (newest first). */
    fun loadLatest(n: Int): List<HistoricalScanRecord> = loadAllRecords().asReversed().take(n)

    /** Compute the delta between the most recent two records. */
    fun computeLatestDelta(): TrendDelta? {
        val records = loadAllRecords()
        if (records.size < 2) return null
        return computeDelta(records[records.size - 2], records.last())
    }

    /**
     * Compute a full [TrendSummary] for the given project.
     */
    fun computeSummary(projectPath: String): TrendSummary {
        val records = loadRecordsForProject(projectPath)
        val latest = records.lastOrNull()
        val oldest = records.firstOrNull()
        val latestDelta = computeLatestDelta()

        return TrendSummary(
            recordCount = records.size,
            latestRecord = latest,
            oldestRecord = oldest,
            overallDirection = computeOverallDirection(records),
            latestDelta = latestDelta,
            window7 = TrendWindow(records.takeLast(7), 7),
            window30 = TrendWindow(records.takeLast(30), 30),
            allTimeWindow = TrendWindow(records, records.size),
        )
    }

    /** Trim storage to retain only the most recent N records. */
    fun trimToMostRecent(n: Int) {
        val all = loadAllRecords()
        if (all.size <= n) return

        val toDelete = all.take(all.size - n).map { it.id }
        toDelete.forEach { id ->
            File(historyDir, "$id.json").delete()
        }
    }

    /** Clear all historical records. */
    fun clearAll() {
        historyDir.listFiles()?.forEach { it.delete() }
    }

    /** Total number of stored records. */
    fun recordCount(): Int = historyDir.listFiles()?.count { it.extension == "json" } ?: 0

    companion object {
        /** Format timestamp for human-readable display. */
        private val DISPLAY_FORMATTER =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())

        fun formatTimestamp(ts: Long): String = DISPLAY_FORMATTER.format(Instant.ofEpochMilli(ts))

        /** Private delta computation. */
        internal fun computeDelta(
            prev: HistoricalScanRecord,
            curr: HistoricalScanRecord,
        ): TrendDelta {
            val deltas =
                OsVSeverity.entries.associateWith { sev ->
                    curr.countFor(sev) - prev.countFor(sev)
                }
            return TrendDelta(
                fromTimestamp = prev.timestamp,
                toTimestamp = curr.timestamp,
                totalVulnChange = curr.totalVulnerabilities - prev.totalVulnerabilities,
                criticalChange = deltas[OsVSeverity.CRITICAL] ?: 0,
                highChange = deltas[OsVSeverity.HIGH] ?: 0,
                severityDeltas = deltas,
            )
        }

        private fun computeOverallDirection(records: List<HistoricalScanRecord>): TrendDirection {
            if (records.size < 2) return TrendDirection.STABLE
            val first = records.first().totalVulnerabilities
            val last = records.last().totalVulnerabilities
            return when {
                last < first -> TrendDirection.IMPROVING
                last > first -> TrendDirection.DEGRADING
                else -> TrendDirection.STABLE
            }
        }
    }
}
