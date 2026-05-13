// OSV Vulnerability Scanner Tool Window Factory
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import io.dyuti.osvplugin.OsVPlugin
import io.dyuti.osvplugin.action.ClearResultsAction
import io.dyuti.osvplugin.action.ExportAction
import io.dyuti.osvplugin.action.ScanAction
import io.dyuti.osvplugin.historical.HistoricalTrendPanel

/**
 * Factory for creating the OSV Tool Window with six tabs:
 *  1. Vulnerabilities — real-time scan results
 *  2. Trends          — historical vulnerability tracking
 *  3. SBOM            — software bill of materials
 *  4. CBOM            — cryptographic bill of materials
 *  5. QBOM            — post-quantum cryptography bill of materials
 *  6. AIBOM           — AI/ML bill of materials
 */
class OsVToolWindowFactory : ToolWindowFactory {
    private var scanPanelRef: OsVToolWindowPanel? = null
    private var trendPanelRef: HistoricalTrendPanel? = null
    private var sbomPanelRef: SbomExportPanel? = null
    private var cbomPanelRef: CbomExportPanel? = null
    private var qbomPanelRef: QbomExportPanel? = null
    private var aibomPanelRef: AibomExportPanel? = null

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

        val cbomPanel = CbomExportPanel(project)
        cbomPanelRef = cbomPanel

        val qbomPanel = QbomExportPanel(project)
        qbomPanelRef = qbomPanel

        val aibomPanel = AibomExportPanel(project)
        aibomPanelRef = aibomPanel

        // Wire scan completion → other panels
        scanPanel.setOnScanCompleted { vulns, deps ->
            trendPanel.onScanCompleted(vulns, deps)
            sbomPanel.setDependencies(scanPanel.getParsedDependencies())
        }

        // Add toolbar actions
        val scanAction = ScanAction(scanPanel)
        val clearAction = ClearResultsAction(scanPanel)
        val exportAction = ExportAction(scanPanel)
        toolWindow.setTitleActions(listOf(scanAction, clearAction, exportAction))

        contentManager.addContent(
            contentFactory.createContent(scanPanel, "Vulnerabilities", false),
        )
        contentManager.addContent(
            contentFactory.createContent(trendPanel, "Trends", false),
        )
        contentManager.addContent(
            contentFactory.createContent(sbomPanel, "SBOM", false),
        )
        contentManager.addContent(
            contentFactory.createContent(cbomPanel, "CBOM", false),
        )
        contentManager.addContent(
            contentFactory.createContent(qbomPanel, "QBOM", false),
        )
        contentManager.addContent(
            contentFactory.createContent(aibomPanel, "AIBOM", false),
        )
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(true)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
