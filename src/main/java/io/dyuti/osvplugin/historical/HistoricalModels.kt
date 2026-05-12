// Historical Trending Data Models
package io.dyuti.osvplugin.historical

import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * A lightweight summary of a scan result for historical tracking.
 *
 * Unlike [io.dyuti.osvplugin.diff.ScanSnapshot], this stores only aggregated
 * counts — not individual vulnerability details — keeping storage small.
 */
data class HistoricalScanRecord(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val projectPath: String,
    val totalVulnerabilities: Int,
    val severityCounts: Map<OsVSeverity, Int>,
    val dependencyCount: Int,
    val scanDurationMs: Long? = null,
) {
    /** Severity-safe accessor (returns 0 for missing keys). */
    fun countFor(severity: OsVSeverity): Int = severityCounts[severity] ?: 0

    /** Total across all known severity levels. */
    val totalBySeverity: Int
        get() = OsVSeverity.entries.sumOf { severityCounts[it] ?: 0 }

    companion object {
        /** Factory that creates a record from a raw scan result. */
        fun fromScanResult(
            projectPath: String,
            vulnerabilities: List<io.dyuti.osvplugin.api.model.Vulnerability>,
            dependencies: List<io.dyuti.osvplugin.api.model.Dependency>,
            durationMs: Long? = null,
        ): HistoricalScanRecord =
            HistoricalScanRecord(
                id =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                timestamp = System.currentTimeMillis(),
                projectPath = projectPath,
                totalVulnerabilities = vulnerabilities.size,
                severityCounts = vulnerabilities.groupingBy { it.severity }.eachCount(),
                dependencyCount = dependencies.size,
                scanDurationMs = durationMs,
            )
    }
}

/**
 * A computed trend between two consecutive historical records.
 */
data class TrendDelta(
    val fromTimestamp: Long,
    val toTimestamp: Long,
    val totalVulnChange: Int,
    val criticalChange: Int,
    val highChange: Int,
    val severityDeltas: Map<OsVSeverity, Int>,
) {
    /** Percentage change in total vulnerabilities. */
    val totalChangePercent: Double
        get() =
            if (previousTotal == 0) {
                if (totalVulnChange > 0) 100.0 else 0.0
            } else {
                (totalVulnChange.toDouble() / previousTotal) * 100
            }

    /** Approximate previous total derived from change (for display). */
    val previousTotal: Int
        get() = (severityDeltas.values.sum() - totalVulnChange).coerceAtLeast(0)

    /** True if the situation improved (fewer vulns). */
    val isImproving: Boolean = totalVulnChange < 0

    /** True if things got worse. */
    val isDegrading: Boolean = totalVulnChange > 0
}

/**
 * Rolling window statistics for a set of historical records.
 */
data class TrendWindow(
    val records: List<HistoricalScanRecord>,
    val windowSize: Int,
) {
    /** Average total vulnerabilities in this window. */
    val avgTotalVulnerabilities: Double
        get() = if (records.isEmpty()) 0.0 else records.map { it.totalVulnerabilities }.average()

    /** Average critical count. */
    val avgCritical: Double
        get() = if (records.isEmpty()) 0.0 else records.map { it.countFor(OsVSeverity.CRITICAL) }.average()

    /** Average high count. */
    val avgHigh: Double
        get() = if (records.isEmpty()) 0.0 else records.map { it.countFor(OsVSeverity.HIGH) }.average()

    /** Peak (max) total vulnerabilities. */
    val peakTotal: Int
        get() = records.maxOfOrNull { it.totalVulnerabilities } ?: 0

    /** Minimum total vulnerabilities. */
    val minTotal: Int
        get() = records.minOfOrNull { it.totalVulnerabilities } ?: 0

    /** Overall trend direction across this window. */
    val direction: TrendDirection
        get() =
            when {
                records.size < 2 -> {
                    TrendDirection.STABLE
                }

                else -> {
                    val first = records.first().totalVulnerabilities
                    val last = records.last().totalVulnerabilities
                    when {
                        last < first -> TrendDirection.IMPROVING
                        last > first -> TrendDirection.DEGRADING
                        else -> TrendDirection.STABLE
                    }
                }
            }
}

enum class TrendDirection {
    IMPROVING,
    DEGRADING,
    STABLE,
}

/**
 * Summary of the complete historical dataset for display in UI.
 */
data class TrendSummary(
    val recordCount: Int,
    val latestRecord: HistoricalScanRecord?,
    val oldestRecord: HistoricalScanRecord?,
    val overallDirection: TrendDirection,
    val latestDelta: TrendDelta?,
    val window7: TrendWindow,
    val window30: TrendWindow,
    val allTimeWindow: TrendWindow,
)
