package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.sbom.SbomExporter
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTextField

/**
 * A standalone SBOM export panel that can be embedded in the tool window.
 *
 * Avoids nested-class issues in the main tool window panel.
 */
class SbomExportPanel(
    private val project: Project,
) : JBPanel<SbomExportPanel>(BorderLayout()) {
    val exportButton = JButton("Export SBOM")
    private val statusLabel = JLabel("Dependencies scanned: 0")
    private val parsedDependencies = mutableMapOf<VirtualFile, List<Dependency>>()

    init {
        val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(exportButton)
        buttonPanel.add(statusLabel)
        add(buttonPanel, BorderLayout.CENTER)

        exportButton.addActionListener { performExport() }
    }

    /** Feed parsed dependencies from the scan panel. */
    fun setDependencies(deps: Map<VirtualFile, List<Dependency>>) {
        parsedDependencies.clear()
        parsedDependencies.putAll(deps)
        statusLabel.text = "Dependencies scanned: ${parsedDependencies.values.flatten().size}"
    }

    fun updateStatus(text: String) {
        statusLabel.text = text
    }

    private fun performExport() {
        val allDeps = parsedDependencies.values.flatten()
        if (allDeps.isEmpty()) {
            updateStatus("No dependencies. Run a scan first.")
            return
        }
        try {
            val exporter = SbomExporter(project)
            val files = exporter.exportAll(allDeps)
            val sb = StringBuilder("SBOM exported: ")
            files.entries.sortedBy { it.key.name }.forEach { (fmt, file) ->
                sb.append("${fmt.name}=${file.name} ")
            }
            updateStatus(sb.toString().trim())
        } catch (e: Exception) {
            updateStatus("Export failed: ${e.message}")
        }
    }
}
