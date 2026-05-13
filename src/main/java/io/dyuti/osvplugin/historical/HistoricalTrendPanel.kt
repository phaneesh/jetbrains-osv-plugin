// Historical Vulnerability Trends — professional card-based layout with rich charts
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
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * Professional trend panel with card-based layout, rich charts, and zero truncation.
 *
 * Layout:
 *  ┌─ Metric Cards (horizontal scroll) ──────┐
 *  │ Total  │ Critical │ High │ Medium │ Low  │
 *  ├─ Line Chart Card ───────────────────────┤
 *  │  Area chart with grid, labels, legend   │
 *  ├─ Severity Donut Chart Card ─────────────┤
 *  │  Color-coded donut with legend          │
 *  ├─ Statistics Table Card ─────────────────┤
 *  │  Rolling window averages                │
 *  └─ Delta Change Card ─────────────────────┘
 */
class HistoricalTrendPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val repo: HistoricalScanRepository by lazy {
        HistoricalScanRepository(project.basePath?.let { java.io.File(it) } ?: java.io.File("."))
    }

    private val contentPanel =
        JPanel(GridBagLayout()).apply {
            background = panelBackground()
            border = EmptyBorder(16, 16, 16, 16)
        }

    private val scrollPane =
        JBScrollPane(contentPanel).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

    init {
        isOpaque = false
        background = panelBackground()
        add(scrollPane, BorderLayout.CENTER)
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
                emptyStatePanel(),
                gridConstraints(0, fill = GridBagConstraints.BOTH, weighty = 1.0),
            )
            contentPanel.revalidate()
            contentPanel.repaint()
            return
        }

        var row = 0

        // 1. Metric cards row
        contentPanel.add(
            buildMetricCards(summary),
            gridConstraints(row++, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0),
        )

        // 2. Line chart card
        val records = summary.allTimeWindow.records
        if (records.size >= 2) {
            val counts = records.map { it.totalVulnerabilities }
            val labels = records.map { formatDateShort(it.timestamp) }
            contentPanel.add(
                cardPanel(
                    "Vulnerability Trend Over Time",
                    EnhancedLineChart(counts, labels),
                    minHeight = 280,
                ),
                gridConstraints(row++, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0),
            )
        }

        // 3. Severity distribution donut chart
        summary.latestRecord?.let { latest ->
            val sevData =
                mapOf(
                    OsVSeverity.CRITICAL to latest.countFor(OsVSeverity.CRITICAL),
                    OsVSeverity.HIGH to latest.countFor(OsVSeverity.HIGH),
                    OsVSeverity.MEDIUM to latest.countFor(OsVSeverity.MEDIUM),
                    OsVSeverity.LOW to latest.countFor(OsVSeverity.LOW),
                )
            if (sevData.values.any { it > 0 }) {
                contentPanel.add(
                    cardPanel(
                        "Severity Distribution (Latest Scan)",
                        DonutChart(sevData),
                        minHeight = 220,
                    ),
                    gridConstraints(row++, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0),
                )
            }
        }

        // 4. Statistics table
        contentPanel.add(
            cardPanel(
                "Rolling Statistics",
                buildStatsTable(summary),
                minHeight = 160,
            ),
            gridConstraints(row++, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0),
        )

        // 5. Delta since last scan
        summary.latestDelta?.let { delta ->
            contentPanel.add(
                cardPanel(
                    "Change Since Previous Scan",
                    buildDeltaPanel(delta),
                    minHeight = 120,
                ),
                gridConstraints(row++, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0),
            )
        }

        // Bottom spacer to push everything up
        contentPanel.add(
            Box.createVerticalGlue(),
            gridConstraints(row, fill = GridBagConstraints.VERTICAL, weighty = 1.0),
        )

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    // ═══════════════════════════════════════════════════════════════
    //  Metric Cards
    // ═══════════════════════════════════════════════════════════════

    private fun buildMetricCards(summary: TrendSummary): JPanel {
        val panel =
            JPanel(FlowLayout(FlowLayout.LEFT, 12, 0)).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }

        val latest = summary.latestRecord ?: return panel

        val cards =
            listOf(
                MetricCard("Total Vulnerabilities", latest.totalVulnerabilities.toString(), Color(0x33, 0x99, 0xFF)),
                MetricCard("Critical", latest.countFor(OsVSeverity.CRITICAL).toString(), SeverityUtil.getColor(OsVSeverity.CRITICAL)),
                MetricCard("High", latest.countFor(OsVSeverity.HIGH).toString(), SeverityUtil.getColor(OsVSeverity.HIGH)),
                MetricCard("Medium", latest.countFor(OsVSeverity.MEDIUM).toString(), SeverityUtil.getColor(OsVSeverity.MEDIUM)),
                MetricCard("Low", latest.countFor(OsVSeverity.LOW).toString(), SeverityUtil.getColor(OsVSeverity.LOW)),
            )

        cards.forEach { panel.add(it) }
        return panel
    }

    // ═══════════════════════════════════════════════════════════════
    //  Statistics Table
    // ═══════════════════════════════════════════════════════════════

    private fun buildStatsTable(summary: TrendSummary): JTable {
        val model =
            DefaultTableModel(
                arrayOf("Window", "Records", "Avg Total", "Avg Critical", "Avg High", "Peak", "Trend"),
                0,
            )

        fun addRow(
            label: String,
            window: TrendWindow,
        ) {
            val direction = window.direction
            val trendIcon =
                when (direction) {
                    TrendDirection.IMPROVING -> "▼ Improving"
                    TrendDirection.DEGRADING -> "▲ Degrading"
                    TrendDirection.STABLE -> "─ Stable"
                }
            model.addRow(
                arrayOf(
                    label,
                    window.records.size.toString(),
                    "%.1f".format(window.avgTotalVulnerabilities),
                    "%.1f".format(window.avgCritical),
                    "%.1f".format(window.avgHigh),
                    window.peakTotal.toString(),
                    trendIcon,
                ),
            )
            // Store color for renderer
        }

        addRow("Last 7 scans", summary.window7)
        addRow("Last 30 scans", summary.window30)
        addRow("All time", summary.allTimeWindow)

        val table =
            JTable(model).apply {
                setShowGrid(false)
                intercellSpacing = Dimension(12, 6)
                rowHeight = 36
                font = JBUI.Fonts.label(13f)
                tableHeader.font = JBUI.Fonts.label(13f).deriveFont(Font.BOLD)
                tableHeader.preferredSize = Dimension(100, 36)
                autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
                isEnabled = false
                fillsViewportHeight = false
                border = EmptyBorder(4, 4, 4, 4)
                gridColor = if (JBColor.isBright()) Color(0xE8E8E8) else Color(0x2A2A2A)
                showHorizontalLines = true
                showVerticalLines = false
            }

        // Custom renderer for alternating rows and trend colors
        table.setDefaultRenderer(
            Any::class.java,
            object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int,
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    val bg =
                        if (row % 2 == 0) {
                            if (JBColor.isBright()) Color(0xFA, 0xFA, 0xFA) else Color(0x1E, 0x1E, 0x1E)
                        } else {
                            if (JBColor.isBright()) Color.WHITE else Color(0x25, 0x25, 0x25)
                        }
                    comp.background = bg
                    comp.foreground = JBColor.foreground()
                    (comp as JLabel).isOpaque = true

                    // Align numbers right
                    if (column >= 2) {
                        horizontalAlignment = SwingConstants.RIGHT
                    } else {
                        horizontalAlignment = SwingConstants.LEFT
                    }

                    // Bold the trend column
                    if (column == 6) {
                        font = font.deriveFont(Font.BOLD)
                        val text = value?.toString() ?: ""
                        foreground =
                            when {
                                text.contains("Improving") -> JBColor(Color(0x00, 0xAA, 0x00), Color(0x4C, 0xAF, 0x50))
                                text.contains("Degrading") -> JBColor(Color(0xFF, 0x44, 0x44), Color(0xF4, 0x43, 0x36))
                                else -> JBColor.GRAY
                            }
                    }
                    return comp
                }
            },
        )

        // Column widths
        val colModel = table.columnModel
        colModel.getColumn(0).preferredWidth = 140
        colModel.getColumn(1).preferredWidth = 80
        colModel.getColumn(2).preferredWidth = 90
        colModel.getColumn(3).preferredWidth = 110
        colModel.getColumn(4).preferredWidth = 90
        colModel.getColumn(5).preferredWidth = 70
        colModel.getColumn(6).preferredWidth = 120

        return table
    }

    // ═══════════════════════════════════════════════════════════════
    //  Delta Panel
    // ═══════════════════════════════════════════════════════════════

    private fun buildDeltaPanel(delta: TrendDelta): JPanel {
        val panel =
            JPanel(FlowLayout(FlowLayout.LEFT, 16, 8)).apply {
                isOpaque = false
            }

        val since = HistoricalScanRepository.formatTimestamp(delta.fromTimestamp)
        val changePct = "%.1f".format(delta.totalChangePercent)
        val sign = if (delta.totalVulnChange > 0) "+" else ""

        // Total change badge
        val totalColor =
            when {
                delta.isImproving -> JBColor(Color(0xE8, 0xF5, 0xE9), Color(0x1B, 0x5E, 0x20))
                delta.isDegrading -> JBColor(Color(0xFF, 0xEB, 0xEE), Color(0x5C, 0x12, 0x12))
                else -> JBColor(Color(0xF5, 0xF5, 0xF5), Color(0x33, 0x33, 0x33))
            }
        val totalTextColor =
            when {
                delta.isImproving -> JBColor(Color(0x2E, 0x7D, 0x32), Color(0x81, 0xC7, 0x84))
                delta.isDegrading -> JBColor(Color(0xC6, 0x28, 0x28), Color(0xEF, 0x53, 0x50))
                else -> JBColor.GRAY
            }

        panel.add(
            Badge(
                label = "Total: $sign${delta.totalVulnChange} ($changePct%)",
                bg = totalColor,
                fg = totalTextColor,
                icon =
                    when {
                        delta.isImproving -> "▼"
                        delta.isDegrading -> "▲"
                        else -> "─"
                    },
            ),
        )

        // Severity change badges
        val severityBadges = mutableListOf<Pair<String, Int>>()
        if (delta.criticalChange != 0) severityBadges.add("Critical" to delta.criticalChange)
        if (delta.highChange != 0) severityBadges.add("High" to delta.highChange)
        delta.severityDeltas[OsVSeverity.MEDIUM]?.let { if (it != 0) severityBadges.add("Medium" to it) }
        delta.severityDeltas[OsVSeverity.LOW]?.let { if (it != 0) severityBadges.add("Low" to it) }

        severityBadges.forEach { (label, value) ->
            val vSign = if (value > 0) "+" else ""
            val color =
                when {
                    value < 0 -> Color(0x00, 0xAA, 0x00)
                    value > 0 -> Color(0xFF, 0x44, 0x44)
                    else -> JBColor.GRAY
                }
            panel.add(Badge(label = "$label: $vSign$value", bg = panelBackground(), fg = color))
        }

        panel.add(
            JLabel("(since $since)").apply {
                foreground = JBColor.GRAY
                font = JBUI.Fonts.label(11f)
            },
        )

        return panel
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════

    private fun emptyStatePanel(): JPanel =
        JPanel(BorderLayout()).apply {
            isOpaque = false
            val label =
                JBLabel("No scan history yet. Run a vulnerability scan to start tracking trends.").apply {
                    horizontalAlignment = SwingConstants.CENTER
                    foreground = JBColor.GRAY
                    font = JBUI.Fonts.label(14f)
                }
            add(label, BorderLayout.CENTER)
            border = EmptyBorder(60, 20, 60, 20)
        }

    private fun cardPanel(
        title: String,
        content: JComponent,
        minHeight: Int,
    ): JPanel {
        val card =
            JPanel(BorderLayout()).apply {
                border = cardBorder()
                background = cardBackground()
                isOpaque = true
                preferredSize = Dimension(600, minHeight)
                minimumSize = Dimension(300, minHeight)
            }

        // Title bar
        val titleBar =
            JPanel(BorderLayout()).apply {
                background = cardBackground()
                isOpaque = true
                border = EmptyBorder(12, 16, 8, 16)
            }
        titleBar.add(
            JBLabel(title).apply {
                font = JBUI.Fonts.label(14f).deriveFont(Font.BOLD)
                foreground = JBColor.foreground()
            },
            BorderLayout.WEST,
        )
        card.add(titleBar, BorderLayout.NORTH)

        // Content
        content.background = cardBackground()
        content.isOpaque = true
        content.border = EmptyBorder(4, 16, 16, 16)
        card.add(content, BorderLayout.CENTER)

        return card
    }

    private fun panelBackground(): Color = if (JBColor.isBright()) Color(0xF5, 0xF5, 0xF5) else Color(0x1A, 0x1A, 0x1A)

    private fun cardBackground(): Color = if (JBColor.isBright()) Color.WHITE else Color(0x22, 0x22, 0x22)

    private fun cardBorder(): CompoundBorder {
        val outer = EmptyBorder(0, 0, 12, 0)
        val inner =
            JBUI.Borders.customLine(
                if (JBColor.isBright()) Color(0xE0, 0xE0, 0xE0) else Color(0x33, 0x33, 0x33),
                1,
            )
        return CompoundBorder(outer, inner)
    }

    private fun gridConstraints(
        row: Int,
        fill: Int = GridBagConstraints.HORIZONTAL,
        weightx: Double = 0.0,
        weighty: Double = 0.0,
    ): GridBagConstraints =
        GridBagConstraints().apply {
            gridx = 0
            gridy = row
            this.fill = fill
            this.weightx = weightx
            this.weighty = weighty
            insets = JBUI.insets(0)
            anchor = GridBagConstraints.NORTH
        }

    private fun formatDateShort(ts: Long): String {
        val fmt = SimpleDateFormat("MMM d")
        return fmt.format(java.util.Date(ts))
    }
}

// ═══════════════════════════════════════════════════════════════
//  MetricCard — reusable metric badge
// ═══════════════════════════════════════════════════════════════

private class MetricCard(
    label: String,
    value: String,
    color: Color,
) : JPanel(BorderLayout()) {
    init {
        isOpaque = true
        background = cardBg()
        border =
            CompoundBorder(
                JBUI.Borders.customLine(
                    if (JBColor.isBright()) Color(0xE0, 0xE0, 0xE0) else Color(0x33, 0x33, 0x33),
                    1,
                ),
                EmptyBorder(12, 16, 12, 16),
            )
        minimumSize = Dimension(120, 72)
        preferredSize = Dimension(140, 72)
        maximumSize = Dimension(200, 72)

        val top =
            JLabel(label).apply {
                font = JBUI.Fonts.label(11f)
                foreground = JBColor.GRAY
            }
        add(top, BorderLayout.NORTH)

        val bottom =
            JLabel(value).apply {
                font = JBUI.Fonts.label(22f).deriveFont(Font.BOLD)
                foreground = color
            }
        add(bottom, BorderLayout.CENTER)
    }

    private fun cardBg(): Color = if (JBColor.isBright()) Color.WHITE else Color(0x22, 0x22, 0x22)
}

// ═══════════════════════════════════════════════════════════════
//  Badge — small labeled pill
// ═══════════════════════════════════════════════════════════════

private class Badge(
    label: String,
    bg: Color,
    fg: Color,
    icon: String = "",
) : JPanel(BorderLayout()) {
    init {
        isOpaque = true
        background = bg
        border =
            CompoundBorder(
                JBUI.Borders.customLine(
                    if (JBColor.isBright()) Color(0xD0, 0xD0, 0xD0) else Color(0x44, 0x44, 0x44),
                    1,
                ),
                EmptyBorder(6, 10, 6, 10),
            )

        val text = if (icon.isNotEmpty()) "$icon  $label" else label
        add(
            JLabel(text).apply {
                font = JBUI.Fonts.label(12f).deriveFont(Font.BOLD)
                foreground = fg
            },
            BorderLayout.CENTER,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Enhanced Line Chart — area fill, grid, labels, legend
// ═══════════════════════════════════════════════════════════════

private class EnhancedLineChart(
    private val values: List<Int>,
    private val labels: List<String>,
) : JPanel(BorderLayout()) {
    init {
        isOpaque = false
        preferredSize = Dimension(600, 240)
        minimumSize = Dimension(300, 180)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        val w = width.toFloat()
        val h = height.toFloat()
        val padLeft = 52f
        val padRight = 20f
        val padTop = 24f
        val padBottom = 42f
        val chartW = (w - padLeft - padRight).coerceAtLeast(100f)
        val chartH = (h - padTop - padBottom).coerceAtLeast(60f)

        if (values.isEmpty() || chartW <= 0 || chartH <= 0) return

        val max = values.maxOrNull()!!.coerceAtLeast(1)
        val min = values.minOrNull()!!.coerceAtLeast(0)
        val range = (max - min).coerceAtLeast(1).toFloat()

        val isDark = !JBColor.isBright()

        // Background fill
        g2.color = if (isDark) Color(0x1E, 0x1E, 0x1E) else Color(0xFA, 0xFA, 0xFA)
        g2.fillRect(0, 0, width, height)

        // Grid lines (horizontal)
        val gridColor = if (isDark) Color(0x3A, 0x3A, 0x3A) else Color(0xE0, 0xE0, 0xE0)
        g2.color = gridColor
        g2.stroke = BasicStroke(0.8f)
        val gridSteps = 5
        for (i in 0..gridSteps) {
            val y = padTop + chartH * i / gridSteps
            g2.draw(Line2D.Float(padLeft, y, w - padRight, y))
        }

        // Build path
        val fillPath = Path2D.Float()
        val linePath = Path2D.Float()
        val points = mutableListOf<Pair<Float, Float>>()

        values.forEachIndexed { i, v ->
            val x = padLeft + chartW * i / (values.size - 1).coerceAtLeast(1)
            val y = padTop + chartH * (1f - (v - min) / range)
            points.add(x to y)
            if (i == 0) {
                fillPath.moveTo(x, y)
                linePath.moveTo(x, y)
            } else {
                fillPath.lineTo(x, y)
                linePath.lineTo(x, y)
            }
        }

        // Close fill path to bottom
        fillPath.lineTo(padLeft + chartW, padTop + chartH)
        fillPath.lineTo(padLeft, padTop + chartH)
        fillPath.closePath()

        // Area fill (gradient-like)
        val fillAlpha = if (isDark) 50 else 35
        g2.color = Color(0x33, 0x99, 0xFF, fillAlpha)
        g2.fill(fillPath)

        // Main line
        g2.color = Color(0x33, 0x99, 0xFF)
        g2.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(linePath)

        // Data points with glow
        points.forEach { (x, y) ->
            // Outer glow
            g2.color = Color(0x33, 0x99, 0xFF, 60)
            g2.fill(Ellipse2D.Float(x - 6f, y - 6f, 12f, 12f))
            // White border
            g2.color = Color.WHITE
            g2.fill(Ellipse2D.Float(x - 4f, y - 4f, 8f, 8f))
            // Inner fill
            g2.color = Color(0x33, 0x99, 0xFF)
            g2.fill(Ellipse2D.Float(x - 2.5f, y - 2.5f, 5f, 5f))
        }

        // Y-axis labels
        g2.color = JBColor.GRAY
        g2.font = JBUI.Fonts.label(10f)
        val fm = g2.fontMetrics
        for (i in 0..gridSteps) {
            val labelVal = (min + range * (gridSteps - i) / gridSteps).toInt()
            val y = padTop + chartH * i / gridSteps + fm.ascent / 2f - 2f
            val label = labelVal.toString()
            g2.drawString(label, padLeft - fm.stringWidth(label) - 8f, y)
        }

        // X-axis labels (smart spacing to avoid overlap)
        val maxLabels = (chartW / 50).toInt().coerceAtLeast(2)
        val step =
            when {
                labels.size <= maxLabels -> 1
                else -> (labels.size + maxLabels - 1) / maxLabels
            }
        for (i in labels.indices step step) {
            val x = padLeft + chartW * i / (labels.size - 1).coerceAtLeast(1)
            val label = labels[i]
            val labelWidth = fm.stringWidth(label)
            val drawX = (x - labelWidth / 2f).coerceIn(padLeft, w - padRight - labelWidth)
            g2.drawString(label, drawX, h - padBottom + fm.ascent + 4f)
        }

        // Chart border
        g2.color = gridColor
        g2.stroke = BasicStroke(1f)
        g2.draw(Rectangle2D.Float(padLeft, padTop, chartW, chartH))
    }
}

// ═══════════════════════════════════════════════════════════════
//  Donut Chart — compact severity distribution with legend
// ═══════════════════════════════════════════════════════════════

private class DonutChart(
    private val data: Map<OsVSeverity, Int>,
) : JPanel(BorderLayout()) {
    init {
        isOpaque = false
        preferredSize = Dimension(600, 200)
        minimumSize = Dimension(300, 160)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        val w = width.toFloat()
        val h = height.toFloat()
        val isDark = !JBColor.isBright()

        g2.color = if (isDark) Color(0x1E, 0x1E, 0x1E) else Color(0xFA, 0xFA, 0xFA)
        g2.fillRect(0, 0, width, height)

        val total = data.values.sum().coerceAtLeast(1)
        val sortedEntries = data.entries.sortedByDescending { it.value }

        // Determine chart dimensions
        val chartSize = minOf(w * 0.35f, h * 0.85f)
        val centerX = padLeft + chartSize / 2f
        val centerY = h / 2f
        val outerR = chartSize / 2f - 8f
        val innerR = outerR * 0.55f

        // Draw donut segments
        var startAngle = 90.0 // Start from top (Swing angles go clockwise from 3 o'clock, negative is CCW)
        val segArcs = mutableListOf<Triple<Double, Double, OsVSeverity>>()

        sortedEntries.forEach { (severity, count) ->
            val extent = -360.0 * count / total
            val color = SeverityUtil.getColor(severity)

            // Draw arc
            g2.color = color
            val arc =
                Arc2D.Float(
                    centerX - outerR,
                    centerY - outerR,
                    outerR * 2f,
                    outerR * 2f,
                    startAngle.toFloat(),
                    extent.toFloat(),
                    Arc2D.PIE,
                )
            g2.fill(arc)

            segArcs.add(Triple(startAngle, extent, severity))
            startAngle += extent
        }

        // Cut out center to make donut
        g2.color = if (isDark) Color(0x1E, 0x1E, 0x1E) else Color(0xFA, 0xFA, 0xFA)
        g2.fill(Ellipse2D.Float(centerX - innerR, centerY - innerR, innerR * 2f, innerR * 2f))

        // Center label (total)
        g2.color = JBColor.foreground()
        g2.font = JBUI.Fonts.label(18f).deriveFont(Font.BOLD)
        val fmTotal = g2.fontMetrics
        val totalStr = total.toString()
        g2.drawString(totalStr, centerX - fmTotal.stringWidth(totalStr) / 2f, centerY + fmTotal.ascent / 2f - 2f)

        g2.font = JBUI.Fonts.label(10f)
        g2.color = JBColor.GRAY
        val totalLabel = "Total"
        g2.drawString(totalLabel, centerX - fmTotal.stringWidth(totalLabel) / 2f, centerY - fmTotal.ascent / 2f + 2f)

        // Legend on the right
        val legendX = centerX + outerR + 32f
        val legendY = centerY - (sortedEntries.size * 28f) / 2f
        g2.font = JBUI.Fonts.label(12f)
        val fm = g2.fontMetrics

        sortedEntries.forEachIndexed { idx, (severity, count) ->
            val y = legendY + idx * 28f
            val color = SeverityUtil.getColor(severity)

            // Color swatch
            g2.color = color
            g2.fill(RoundRectangle2D.Float(legendX, y - 2f, 14f, 14f, 3f, 3f))

            // Label
            g2.color = JBColor.foreground()
            val pct = (100.0 * count / total).toInt()
            val label = "${severity.name}: $count ($pct%)"
            g2.drawString(label, legendX + 22f, y + fm.ascent / 2f + 2f)
        }
    }

    companion object {
        private const val padLeft = 20f
    }
}
