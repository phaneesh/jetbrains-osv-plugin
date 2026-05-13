// SBOM Export Panel — dependency tree view + format-aware file save dialog
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.sbom.SbomFormat
import io.dyuti.osvplugin.sbom.SbomGenerator
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * SBOM export panel with dependency tree view and file save dialog.
 *
 * Shows parsed dependencies in a navigable tree grouped by source file
 * (pom.xml, build.gradle, etc.) and provides format-aware export with
 * a standard file chooser dialog.
 */
class SbomExportPanel(
    private val project: Project,
) : JBPanel<SbomExportPanel>(BorderLayout()) {
    private val parsedDependencies = mutableMapOf<VirtualFile, List<Dependency>>()
    private val statusLabel = JLabel("Dependencies scanned: 0")
    private val formatCombo = JComboBox(FormatOption.entries.toTypedArray())
    private val sbomTree = JTree(DefaultMutableTreeNode("Dependencies"))
    private val generator = SbomGenerator(appName = project.name)

    init {
        // ── North: header + controls ──
        val northPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val headerPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val headerLabel = JLabel("SBOM: ${project.name}")
        headerLabel.font = headerLabel.font.deriveFont(headerLabel.font.size + 2f)
        headerPanel.add(headerLabel)
        northPanel.add(headerPanel, BorderLayout.NORTH)

        val controlPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(JLabel("Format:"))
        controlPanel.add(formatCombo)
        val exportButton = JButton("Export to File...")
        exportButton.addActionListener { showExportDialog() }
        controlPanel.add(exportButton)
        northPanel.add(controlPanel, BorderLayout.SOUTH)

        add(northPanel, BorderLayout.NORTH)

        // ── Center: dependency tree ──
        sbomTree.isRootVisible = false
        add(JScrollPane(sbomTree), BorderLayout.CENTER)

        // ── South: status bar ──
        add(statusLabel, BorderLayout.SOUTH)
    }

    /** Feed parsed dependencies from the scan panel. */
    fun setDependencies(deps: Map<VirtualFile, List<Dependency>>) {
        parsedDependencies.clear()
        parsedDependencies.putAll(deps)
        rebuildTree()
    }

    private fun rebuildTree() {
        val root = DefaultMutableTreeNode("Project : ${project.name}")

        // Group by source file
        parsedDependencies.forEach { (file, deps) ->
            val fileNode = DefaultMutableTreeNode("${file.name} (${deps.size})")
            deps.forEach { dep ->
                val node = DefaultMutableTreeNode("${dep.name} @ ${dep.version}")
                try {
                    val purl = generator.toPurl(dep)
                    node.add(DefaultMutableTreeNode("PURL: $purl"))
                } catch (_: Exception) {
                }
                node.add(DefaultMutableTreeNode("Ecosystem: ${dep.ecosystem}"))
                node.add(DefaultMutableTreeNode("Scope: ${dep.scope}"))
                node.add(DefaultMutableTreeNode("Transitive: ${dep.transitive}"))
                fileNode.add(node)
            }
            root.add(fileNode)
        }

        sbomTree.model = DefaultTreeModel(root)
        val distinctCount =
            parsedDependencies.values
                .flatten()
                .distinctBy { "${it.name}:${it.version}" }
                .size
        statusLabel.text = "Dependencies: $distinctCount total (${parsedDependencies.size} file(s))"
    }

    private fun showExportDialog() {
        val allDeps =
            parsedDependencies.values
                .flatten()
                .distinctBy { "${it.name}:${it.version}" }
        if (allDeps.isEmpty()) {
            statusLabel.text = "No dependencies to export. Run a scan first."
            return
        }

        val selected = formatCombo.selectedItem as FormatOption
        val chooser = JFileChooser()
        chooser.dialogTitle = "Save SBOM"
        chooser.selectedFile = File(project.basePath ?: ".", "sbom-${project.name}.${selected.extension}")
        chooser.addChoosableFileFilter(
            FileNameExtensionFilter(
                "${selected.displayName} (*.${selected.extension})",
                selected.extension,
            ),
        )

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".${selected.extension}")) {
                file = File(file.parentFile, file.name + ".${selected.extension}")
            }

            try {
                val content = generator.generate(allDeps, selected.format)
                file.writeText(content)

                // VFS refresh on EDT to avoid write-thread errors
                ApplicationManager.getApplication().invokeLater {
                    try {
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    } catch (_: Exception) {
                        // best-effort
                    }
                }

                statusLabel.text = "Exported ${allDeps.size} dep(s) to ${file.absolutePath}"
            } catch (e: Exception) {
                statusLabel.text = "Export failed: ${e.message}"
            }
        }
    }

    private enum class FormatOption(
        val displayName: String,
        val extension: String,
        val format: SbomFormat,
    ) {
        CYCLONEDX_JSON("CycloneDX JSON", "cdx.json", SbomFormat.CYCLONEDX_JSON),
        SPDX_JSON("SPDX JSON", "spdx.json", SbomFormat.SPDX_JSON),
        SPDX_TAGVALUE("SPDX Tag-Value", "spdx.tv", SbomFormat.SPDX_TAGVALUE),
        ;

        override fun toString(): String = displayName
    }
}
