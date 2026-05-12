package io.dyuti.osvplugin.historical

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingConstants

/**
 * A tab panel that displays historical vulnerability trends for the project.
 *
 * Shows a simple ASCII spark-line + rolling-window stats.
 *
 * @param project The current IntelliJ project
 */
class HistoricalTrendPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val repo: HistoricalScanRepository by lazy {
        HistoricalScanRepository(project.basePath?.let { java.io.File(it) } ?: java.io.File("."))
    }

    // UI widgets
    private val trendingArea =
        JTextArea().apply {
            isEditable = false
            font = monospacedFont()
            border = JBUI.Borders.empty(8)
            background = Color(0x2B, 0x2B, 0x2B)
            foreground = Color.LIGHT_GRAY
        }
    private val summaryLabel =
        JBLabel("", SwingConstants.LEFT).apply {
            border = JBUI.Borders.empty(8)
        }
    private val toolbarActions =
        JPanel(BorderLayout()).apply {
            add(summaryLabel, BorderLayout.CENTER)
        }

    init {
        add(toolbarActions, BorderLayout.NORTH)
        add(JBScrollPane(trendingArea), BorderLayout.CENTER)
        border = BorderFactory.createEmptyBorder()
        refresh()
    }

    /** Triggered after each scan — capture a new record. */
    fun onScanCompleted(
        vulnerabilities: List<Vulnerability>,
        dependencies: List<Dependency>,
    ) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                repo.saveScan(
                    project.basePath ?: "unknown",
                    vulnerabilities,
                    dependencies,
                )
            },
            "Saving scan history",
            false,
            project,
        )
        refresh()
    }

    /** Re-render the trend display. */
    fun refresh() {
        val projectPath = project.basePath ?: "unknown"
        val summary = repo.computeSummary(projectPath)

        if (summary.recordCount == 0) {
            summaryLabel.text = "No scan history yet. Run a vulnerability scan to start tracking trends."
            trendingArea.text = ""
            return
        }

        val sb = StringBuilder()
        buildHeader(summary, sb)
        buildSparkline(summary, sb)
        buildStatTable(summary, sb)
        buildDeltaReport(summary, sb)

        summaryLabel.text = null
        trendingArea.text = sb.toString()
        trendingArea.caretPosition = 0
    }

    // ─── rendering helpers ────────────────────────────────────────

    private fun buildHeader(
        summary: TrendSummary,
        sb: StringBuilder,
    ) {
        val latest = summary.latestRecord
        sb.appendLine("┌─────────────────────────────────────────────────────────────┐")
        sb.appendLine("│  📊 VULNERABILITY TRENDS                                    │")
        sb.appendLine("├─────────────────────────────────────────────────────────────┤")
        if (latest != null) {
            val dir = summary.overallDirection
            val arrow =
                when (dir) {
                    TrendDirection.IMPROVING -> "▼ Improving"
                    TrendDirection.DEGRADING -> "▲ Degrading"
                    TrendDirection.STABLE -> "─ Stable"
                }
            sb.appendLine(
                "│  Records: ${summary.recordCount}  |  Direction: $arrow".padEnd(62) + "│",
            )
            sb.appendLine(
                "│  Latest: ${latest.totalVulnerabilities} total  (${latest.countFor(
                    OsVSeverity.CRITICAL,
                )} C, ${latest.countFor(
                    OsVSeverity.HIGH,
                )} H, ${latest.countFor(OsVSeverity.MEDIUM)} M, ${latest.countFor(OsVSeverity.LOW)} L)".padEnd(62) +
                    "│",
            )
        }
        sb.appendLine("└─────────────────────────────────────────────────────────────┘")
        sb.appendLine()
    }

    private fun buildSparkline(
        summary: TrendSummary,
        sb: StringBuilder,
    ) {
        val records = summary.allTimeWindow.records
        if (records.size < 2) {
            sb.appendLine("(At least 2 scans required for spark-line)")
            sb.appendLine()
            return
        }

        val totalVals = records.map { it.totalVulnerabilities }
        val max = totalVals.maxOrNull() ?: 1
        val min = totalVals.minOrNull() ?: 0
        val range = (max - min).coerceAtLeast(1)

        val sparkChars = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
        val spark =
            totalVals.joinToString("") { v ->
                val idx = ((v - min) * (sparkChars.size - 1) / range).coerceIn(0..7)
                sparkChars[idx]
            }

        sb.appendLine("  Total Vulnerabilities Over Time")
        sb.appendLine("  $spark")
        sb.appendLine("  Min: $min  Max: $max")
        sb.appendLine()
    }

    private fun buildStatTable(
        summary: TrendSummary,
        sb: StringBuilder,
    ) {
        val w = summary.allTimeWindow
        sb.appendLine("  ┌────────────┬───────┬─────────────┬─────────────┬───────────┐")
        sb.appendLine("  │ Window     │ N     │ Avg Total   │ Avg Critical│ Avg High  │")
        sb.appendLine("  ├────────────┼───────┼─────────────┼─────────────┼───────────┤")
        buildTableRow(sb, "7-day ", summary.window7)
        buildTableRow(sb, "30-day", summary.window30)
        buildTableRow(sb, "All   ", summary.allTimeWindow)
        sb.appendLine("  └────────────┴───────┴─────────────┴─────────────┴───────────┘")
        sb.appendLine()
    }

    private fun buildTableRow(
        sb: StringBuilder,
        label: String,
        window: TrendWindow,
    ) {
        sb.appendLine(
            "  │ $label    │ ${padLeft(
                window.records.size,
                5,
            )} │ ${padLeft(
                window.avgTotalVulnerabilities,
                11,
                1,
            )} │ ${padLeft(window.avgCritical, 11, 1)} │ ${padLeft(window.avgHigh, 9, 1)} │",
        )
    }

    private fun buildDeltaReport(
        summary: TrendSummary,
        sb: StringBuilder,
    ) {
        val delta = summary.latestDelta ?: return
        val since = HistoricalScanRepository.formatTimestamp(delta.fromTimestamp)
        val changePct = String.format("%.1f", delta.totalChangePercent)
        val sign =
            when {
                delta.totalVulnChange > 0 -> "+"
                delta.totalVulnChange < 0 -> ""
                else -> ""
            }
        sb.appendLine("  📈 Change Since Last Scan ($since)")
        sb.appendLine("     Total: $sign${delta.totalVulnChange} ($changePct%)")
        if (delta.criticalChange != 0) {
            sb.appendLine("     Critical: ${if (delta.criticalChange > 0) "+" else ""}${delta.criticalChange}")
        }
        if (delta.highChange != 0) {
            sb.appendLine("     High: ${if (delta.highChange > 0) "+" else ""}${delta.highChange}")
        }
        if (delta.severityDeltas[OsVSeverity.MEDIUM] ?: 0 != 0) {
            val v = delta.severityDeltas[OsVSeverity.MEDIUM] ?: 0
            sb.appendLine("     Medium: ${if (v > 0) "+" else ""}$v")
        }
        if (delta.severityDeltas[OsVSeverity.LOW] ?: 0 != 0) {
            val v = delta.severityDeltas[OsVSeverity.LOW] ?: 0
            sb.appendLine("     Low: ${if (v > 0) "+" else ""}$v")
        }
        sb.appendLine()
    }

    // ─── helpers ──────────────────────────────────────────────────

    private fun monospacedFont(): java.awt.Font = JBUI.Fonts.create("Monospaced", 12)

    private fun padLeft(
        n: Int,
        w: Int,
    ): String = n.toString().padStart(w)

    private fun padLeft(
        n: Double,
        w: Int,
        decimals: Int = 1,
    ): String = String.format("%.${decimals}f", n).padStart(w)
}
