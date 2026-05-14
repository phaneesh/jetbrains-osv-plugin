package io.dyuti.osvplugin.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JSeparator
import javax.swing.JToggleButton

/**
 * Summary panel component that displays finding counts and provides collapse/expand controls for each finding type.
 */
class CurrentFileSummaryPanel(
    private val issuesSelectionChanged: (Boolean) -> Unit,
    private val hotspotsSelectionChanged: (Boolean) -> Unit,
    private val taintsSelectionChanged: (Boolean) -> Unit,
    private val dependencyRisksSelectionChanged: (Boolean) -> Unit,
    private val toggleFilterBtnClicked: (Boolean) -> Unit,
) : JBPanel<CurrentFileSummaryPanel>(FlowLayout(FlowLayout.LEFT, 8, 0)) {
    private val issuesSummaryButton = SummaryButton("Issue", "Issues", issuesSelectionChanged, "Show/hide issues")
    private val hotspotsSummaryButton =
        SummaryButton("Security Hotspot", "Security Hotspots", hotspotsSelectionChanged, "Show/hide Security Hotspots")
    private val taintsSummaryButton =
        SummaryButton("Taint Vulnerability", "Taint Vulnerabilities", taintsSelectionChanged, "Show/hide taint vulnerabilities")
    private val dependencyRisksSummaryButton =
        SummaryButton("Dependency Risk", "Dependency Risks", dependencyRisksSelectionChanged, "Show/hide dependency risks")
    private val toggleFilterBtn = JToggleButton(AllIcons.General.Filter)

    init {
        border = JBUI.Borders.empty(4, 8)

        toggleFilterBtn.apply {
            isFocusPainted = false
            isContentAreaFilled = false
            isOpaque = false
            toolTipText = "Show Filters"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border =
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createLineBorder(JBColor.LIGHT_GRAY, 1, true),
                )
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        if (!toggleFilterBtn.isSelected) {
                            toggleFilterBtn.background = UIUtil.getPanelBackground().brighter()
                            toggleFilterBtn.isOpaque = true
                        }
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        if (!toggleFilterBtn.isSelected) {
                            toggleFilterBtn.isOpaque = false
                            toggleFilterBtn.background = null
                        }
                    }
                },
            )
            addChangeListener {
                toggleFilterBtnClicked(toggleFilterBtn.isSelected)
                toggleFilterBtn.toolTipText = if (toggleFilterBtn.isSelected) "Hide Filters" else "Show Filters"
                toggleFilterBtn.background =
                    if (toggleFilterBtn.isSelected) UIUtil.getListSelectionBackground(true) else UIUtil.getPanelBackground()
            }
        }

        add(issuesSummaryButton)
        add(hotspotsSummaryButton)
        add(taintsSummaryButton)
        add(dependencyRisksSummaryButton)
        add(
            JSeparator(JSeparator.VERTICAL).apply {
                maximumSize = java.awt.Dimension(8, 24)
            },
        )
        add(toggleFilterBtn)
    }

    fun updateIssues(
        count: Int,
        uiModel: SummaryUiModel,
    ) = issuesSummaryButton.update(count, uiModel)

    fun updateHotspots(
        count: Int,
        uiModel: SummaryUiModel,
    ) = hotspotsSummaryButton.update(count, uiModel)

    fun updateTaints(
        count: Int,
        uiModel: SummaryUiModel,
    ) = taintsSummaryButton.update(count, uiModel)

    fun updateDependencyRisks(
        count: Int,
        uiModel: SummaryUiModel,
    ) = dependencyRisksSummaryButton.update(count, uiModel)

    fun setHotspotsEnabled(isEnabled: Boolean) = hotspotsSummaryButton.setEnabled(isEnabled)

    fun setTaintsEnabled(isEnabled: Boolean) = taintsSummaryButton.setEnabled(isEnabled)

    fun setDependencyRisksEnabled(isEnabled: Boolean) = dependencyRisksSummaryButton.setEnabled(isEnabled)

    fun areIssuesEnabled(): Boolean = issuesSummaryButton.isSelected()

    fun areHotspotsEnabled(): Boolean = hotspotsSummaryButton.isSelected()

    fun areTaintsEnabled(): Boolean = taintsSummaryButton.isSelected()

    fun areDependencyRisksEnabled(): Boolean = dependencyRisksSummaryButton.isSelected()
}
