// OSV Vulnerability Scanner Tool Window Factory
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import io.dyuti.osvplugin.OsVPlugin
import io.dyuti.osvplugin.historical.HistoricalTrendPanel

/**
 * Factory for creating the OSV Tool Window with three tabs:
 *  1. Vulnerabilities — real-time scan results
 *  2. Trends         — historical vulnerability tracking
 *  3. SBOM           — CycloneDX/SPDX export
 */
class OsVToolWindowFactory : ToolWindowFactory {
    private var scanPanelRef: OsVToolWindowPanel? = null
    private var trendPanelRef: HistoricalTrendPanel? = null
    private var sbomPanelRef: SbomExportPanel? = null

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()

        val scanPanel = OsVToolWindowPanel(project)
        scanPanelRef = scanPanel

        val trendPanel = HistoricalTrendPanel(project)
        trendPanelRef = trendPanel

        val sbomPanel = SbomExportPanel(project)
        sbomPanelRef = sbomPanel

        // Wire scan completion → other panels
        scanPanel.setOnScanCompleted { vulns, deps ->
            trendPanel.onScanCompleted(vulns, deps)
            sbomPanel.setDependencies(scanPanel.getParsedDependencies())
        }

        contentManager.addContent(
            contentFactory.createContent(scanPanel, "Vulnerabilities", false),
        )
        contentManager.addContent(
            contentFactory.createContent(trendPanel, "Trends", false),
        )
        contentManager.addContent(
            contentFactory.createContent(sbomPanel, "SBOM", false),
        )
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(true)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
