// Historical Vulnerability Trends — rendered charts + metrics
package io.dyuti.osvplugin.historical

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.utils.SeverityUtil
import java.awt.*
import java.awt.geom.*
import java.text.SimpleDateFormat
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

/**
 * Tab panel displaying historical vulnerability trends with rendered charts.
 *
 * Layout (vertical stack):
 *  1. Summary header — record count, trend direction, latest snapshot
 *  2. Line chart — total vulnerabilities over time with dates on x-axis
 *  3. Severity bar chart — stacked horizontal bars per severity level
 *  4. Statistics table — rolling-window averages
 *  5. Delta report — change since last scan
 */
class HistoricalTrendPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val repo: HistoricalScanRepository by lazy {
        HistoricalScanRepository(project.basePath?.let { java.io.File(it) } ?: java.io.File("."))
    }

    private val contentPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(12, 12, 12, 12)
        }

    init {
        add(
            JBScrollPane(contentPanel).apply {
                border = JBUI.Borders.empty()
                verticalScrollBar.unitIncrement = 16
            },
            BorderLayout.CENTER,
        )
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
        contentPanel.removeAll()
        val projectPath = project.basePath ?: "unknown"
        val summary = repo.computeSummary(projectPath)

        if (summary.recordCount == 0) {
            contentPanel.add(
                JBLabel("No scan history yet. Run a vulnerability scan to start tracking trends.").apply {
                    border = EmptyBorder(20, 20, 20, 20)
                },
            )
            contentPanel.revalidate()
            contentPanel.repaint()
            return
        }

        // 1. Header
        contentPanel.add(buildHeader(summary))

        // 2. Line chart — total vulnerabilities over time
        val records = summary.allTimeWindow.records
        if (records.size >= 2) {
            val counts = records.map { it.totalVulnerabilities }
            val labels = records.map { formatDateShort(it.timestamp) }
            contentPanel.add(SectionLabel("Vulnerability Trend"))
            contentPanel.add(
                LineChartPanel(counts, labels).apply {
                    preferredSize = Dimension(600, 220)
                    maximumSize = Dimension(Int.MAX_VALUE, 240)
                },
            )
        }

        // 3. Severity distribution — latest scan
        summary.latestRecord?.let { latest ->
            contentPanel.add(SectionLabel("Latest Scan by Severity"))
            val sevData =
                mapOf(
                    OsVSeverity.CRITICAL to latest.countFor(OsVSeverity.CRITICAL),
                    OsVSeverity.HIGH to latest.countFor(OsVSeverity.HIGH),
                    OsVSeverity.MEDIUM to latest.countFor(OsVSeverity.MEDIUM),
                    OsVSeverity.LOW to latest.countFor(OsVSeverity.LOW),
                )
            contentPanel.add(
                SeverityBarChart(sevData).apply {
                    preferredSize = Dimension(600, 160)
                    maximumSize = Dimension(Int.MAX_VALUE, 180)
                },
            )
        }

        // 4. Rolling statistics table
        contentPanel.add(SectionLabel("Rolling Statistics"))
        contentPanel.add(buildStatsTable(summary))

        // 5. Delta since last scan
        summary.latestDelta?.let { delta ->
            contentPanel.add(SectionLabel("Change Since Previous Scan"))
            contentPanel.add(buildDeltaPanel(delta))
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    // ─── Header ──────────────────────────────────────────────────

    private fun buildHeader(summary: TrendSummary): JPanel {
        val panel =
            JPanel(BorderLayout()).apply {
                border =
                    CompoundBorder(
                        EmptyBorder(0, 0, 12, 0),
                        JBUI.Borders.customLine(JBColor.GRAY, 0, 0, 1, 0),
                    )
            }
        val left = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

        val title =
            JBLabel("Vulnerability Trends").apply {
                font = font.deriveFont(Font.BOLD, 18f)
            }
        left.add(title)

        val latest = summary.latestRecord
        if (latest != null) {
            val dir = summary.overallDirection
            val dirText =
                when (dir) {
                    TrendDirection.IMPROVING -> "▼ Improving"
                    TrendDirection.DEGRADING -> "▲ Degrading"
                    TrendDirection.STABLE -> "─ Stable"
                }
            val dirColor =
                when (dir) {
                    TrendDirection.IMPROVING -> Color(0x00, 0xAA, 0x00)
                    TrendDirection.DEGRADING -> Color(0xFF, 0x44, 0x44)
                    TrendDirection.STABLE -> JBColor.GRAY
                }
            left.add(
                JBLabel("$dirText  •  ${summary.recordCount} records").apply {
                    foreground = dirColor
                    font = font.deriveFont(Font.BOLD, 13f)
                },
            )
            left.add(
                JBLabel(
                    "Latest: ${latest.totalVulnerabilities} total  " +
                        "(${latest.countFor(OsVSeverity.CRITICAL)} Critical, " +
                        "${latest.countFor(OsVSeverity.HIGH)} High, " +
                        "${latest.countFor(OsVSeverity.MEDIUM)} Medium, " +
                        "${latest.countFor(OsVSeverity.LOW)} Low)",
                ),
            )
        }
        panel.add(left, BorderLayout.WEST)
        return panel
    }

    // ─── Statistics Table ────────────────────────────────────────

    private fun buildStatsTable(summary: TrendSummary): JTable {
        val model =
            DefaultTableModel(
                arrayOf("Window", "Records", "Avg Total", "Avg Critical", "Avg High"),
                0,
            )

        fun addRow(
            label: String,
            window: TrendWindow,
        ) {
            model.addRow(
                arrayOf(
                    label,
                    window.records.size.toString(),
                    "%.1f".format(window.avgTotalVulnerabilities),
                    "%.1f".format(window.avgCritical),
                    "%.1f".format(window.avgHigh),
                ),
            )
        }
        addRow("Last 7 scans", summary.window7)
        addRow("Last 30 scans", summary.window30)
        addRow("All time", summary.allTimeWindow)
        return JTable(model).apply {
            setShowGrid(false)
            intercellSpacing = Dimension(0, 0)
            rowHeight = 28
            font = JBUI.Fonts.label(12f)
            tableHeader.font = JBUI.Fonts.label(12f).deriveFont(Font.BOLD)
            autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
            isEnabled = false
            border = EmptyBorder(4, 4, 4, 4)
        }
    }

    // ─── Delta Panel ─────────────────────────────────────────────

    private fun buildDeltaPanel(delta: TrendDelta): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = JBColor(0xF0F8FF, 0x1A2634)
                border =
                    CompoundBorder(
                        JBUI.Borders.customLine(JBColor(0xBBDDFF, 0x2A3A54), 1),
                        EmptyBorder(10, 12, 10, 12),
                    )
            }
        val since = HistoricalScanRepository.formatTimestamp(delta.fromTimestamp)
        val changePct = "%.1f".format(delta.totalChangePercent)
        val sign =
            when {
                delta.totalVulnChange > 0 -> "+"
                else -> ""
            }
        panel.add(JBLabel("Change since $since").apply { font = font.deriveFont(Font.BOLD) })
        panel.add(JBLabel("Total: $sign${delta.totalVulnChange} ($changePct%)"))
        val parts = mutableListOf<String>()
        if (delta.criticalChange != 0) parts += "Critical: ${fmtDelta(delta.criticalChange)}"
        if (delta.highChange != 0) parts += "High: ${fmtDelta(delta.highChange)}"
        delta.severityDeltas[OsVSeverity.MEDIUM]?.let { if (it != 0) parts += "Medium: ${fmtDelta(it)}" }
        delta.severityDeltas[OsVSeverity.LOW]?.let { if (it != 0) parts += "Low: ${fmtDelta(it)}" }
        if (parts.isNotEmpty()) panel.add(JBLabel(parts.joinToString("  •  ")))
        return panel
    }

    private fun fmtDelta(n: Int): String = if (n > 0) "+$n" else "$n"

    private fun SectionLabel(text: String): JBLabel =
        JBLabel(text).apply {
            font = font.deriveFont(Font.BOLD, 13f)
            border = EmptyBorder(16, 0, 8, 0)
        }

    private fun formatDateShort(ts: Long): String {
        val fmt = SimpleDateFormat("MMM d")
        return fmt.format(java.util.Date(ts))
    }
}

// ═══════════════════════════════════════════════════════════════
//  Custom Chart Components
// ═══════════════════════════════════════════════════════════════

/**
 * Renders a simple anti-aliased line chart with labelled axes.
 */
private class LineChartPanel(
    private val values: List<Int>,
    private val labels: List<String>,
) : JPanel(BorderLayout()) {
    init {
        isOpaque = false
        border = EmptyBorder(8, 8, 8, 8)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width.toFloat()
        val h = height.toFloat()
        val padLeft = 48f
        val padRight = 16f
        val padTop = 16f
        val padBottom = 32f
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        if (values.isEmpty() || chartW <= 0 || chartH <= 0) return

        val max = values.maxOrNull()!!.coerceAtLeast(1)
        val min = values.minOrNull()!!.coerceAtLeast(0)
        val range = (max - min).coerceAtLeast(1)

        // Grid lines
        val gridColor = if (JBColor.isBright()) Color(0xE0E0E0) else Color(0x3A3A3A)
        g2.color = gridColor
        for (i in 0..4) {
            val y = padTop + chartH * i / 4f
            g2.draw(Line2D.Float(padLeft, y, w - padRight, y))
        }

        // Data area
        val fill = Path2D.Float()
        val line = Path2D.Float()
        values.forEachIndexed { i, v ->
            val x = padLeft + chartW * i / (values.size - 1).coerceAtLeast(1)
            val y = padTop + chartH * (1f - (v - min).toFloat() / range)
            if (i == 0) {
                fill.moveTo(x, y)
                line.moveTo(x, y)
            } else {
                fill.lineTo(x, y)
                line.lineTo(x, y)
            }
        }
        // Close fill path
        fill.lineTo(padLeft + chartW, padTop + chartH)
        fill.lineTo(padLeft, padTop + chartH)
        fill.closePath()
        g2.color = Color(0x33, 0x99, 0xFF, if (JBColor.isBright()) 40 else 60)
        g2.fill(fill)

        // Line
        g2.color = Color(0x33, 0x99, 0xFF)
        g2.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(line)

        // Dots
        values.forEachIndexed { i, v ->
            val x = padLeft + chartW * i / (values.size - 1).coerceAtLeast(1)
            val y = padTop + chartH * (1f - (v - min).toFloat() / range)
            g2.color = Color.WHITE
            g2.fill(Ellipse2D.Float(x - 3.5f, y - 3.5f, 7f, 7f))
            g2.color = Color(0x33, 0x99, 0xFF)
            g2.fill(Ellipse2D.Float(x - 2.5f, y - 2.5f, 5f, 5f))
        }

        // Y-axis labels
        g2.color = JBColor.GRAY
        g2.font = JBUI.Fonts.label(10f)
        val fm = g2.fontMetrics
        for (i in 0..4) {
            val labelVal = (min + range * (4 - i) / 4f).toInt()
            val y = padTop + chartH * i / 4f + fm.ascent / 2f
            val label = labelVal.toString()
            g2.drawString(label, padLeft - fm.stringWidth(label) - 6f, y)
        }

        // X-axis labels (show every nth label to avoid overlap)
        val step = ((labels.size / 6).coerceAtLeast(1)).coerceAtMost(labels.size - 1)
        for (i in labels.indices step step.coerceAtLeast(1)) {
            val x = padLeft + chartW * i / (labels.size - 1).coerceAtLeast(1)
            val label = labels[i]
            val y = h - padBottom + fm.ascent + 4f
            g2.drawString(label, x - fm.stringWidth(label) / 2f, y)
        }
    }
}

/**
 * Horizontal stacked bar chart for severity distribution.
 */
private class SeverityBarChart(
    private val data: Map<OsVSeverity, Int>,
) : JPanel(BorderLayout()) {
    init {
        isOpaque = false
        border = EmptyBorder(8, 8, 8, 8)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        val w = width.toFloat()
        val h = height.toFloat()
        val padLeft = 80f
        val padRight = 48f
        val padTop = 8f
        val padBottom = 8f
        val barH = ((h - padTop - padBottom) / data.size).coerceAtLeast(20f)
        val maxCount = data.values.maxOrNull()!!.coerceAtLeast(1)
        val barMaxW = w - padLeft - padRight

        val fm = g2.fontMetrics
        g2.font = JBUI.Fonts.label(12f)

        data.entries.sortedByDescending { it.value }.forEachIndexed { idx, (severity, count) ->
            val y = padTop + idx * barH
            val barW = barMaxW * count / maxCount
            val color = SeverityUtil.getColor(severity)
            val darkColor = color.darker()

            // Label
            g2.color = JBColor.foreground()
            g2.drawString(severity.name, 4f, y + barH / 2f + fm.ascent / 2f - 2f)

            // Bar background (subtle)
            g2.color = if (JBColor.isBright()) Color(0xF0F0F0) else Color(0x2A2A2A)
            g2.fill(RoundRectangle2D.Float(padLeft, y + 2f, barMaxW, barH - 4f, 6f, 6f))

            // Bar fill
            g2.color = color
            g2.fill(RoundRectangle2D.Float(padLeft, y + 2f, barW, barH - 4f, 6f, 6f))

            // Value label at end of bar
            g2.color = JBColor.foreground()
            g2.drawString(count.toString(), padLeft + barW + 8f, y + barH / 2f + fm.ascent / 2f - 2f)
        }
    }
}
