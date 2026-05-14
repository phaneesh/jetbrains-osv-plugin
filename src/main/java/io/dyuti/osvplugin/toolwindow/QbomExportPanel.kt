// QBOM Export Panel — post-quantum crypto asset tree view + export dialog
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.Gray
import com.intellij.ui.components.JBPanel
import io.dyuti.osvplugin.qbom.QbomGenerator
import io.dyuti.osvplugin.qbom.QuantumAsset
import io.dyuti.osvplugin.qbom.QuantumAssetType
import io.dyuti.osvplugin.qbom.QuantumScanner
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * QBOM export panel with post-quantum crypto asset tree view and file save dialog.
 */
class QbomExportPanel(
    private val project: Project,
) : JBPanel<QbomExportPanel>(BorderLayout()) {
    private val statusLabel = JLabel("Click 'Scan' to discover PQC assets")
    private val qbomTree = JTree(DefaultMutableTreeNode("Post-Quantum Assets"))
    private val generator = QbomGenerator(appName = project.name, appVersion = "1.1.2")
    private val scanner = QuantumScanner(project)
    private var lastAssets: List<QuantumAsset> = emptyList()

    init {
        background =
            if (com.intellij.ui.JBColor
                    .isBright()
            ) {
                Gray._245
            } else {
                Gray._26
            }
        isOpaque = true

        val northPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val headerPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val headerLabel = JLabel("QBOM: ${project.name}")
        headerLabel.font = headerLabel.font.deriveFont(headerLabel.font.size + 2f)
        headerPanel.add(headerLabel)
        northPanel.add(headerPanel, BorderLayout.NORTH)

        val controlPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val scanButton = JButton("Scan Project")
        scanButton.addActionListener { performScan() }
        controlPanel.add(scanButton)

        val exportButton = JButton("Export QBOM...")
        exportButton.addActionListener { showExportDialog() }
        exportButton.isEnabled = false
        exportButton.name = "exportQbomButton"
        controlPanel.add(exportButton)

        northPanel.add(controlPanel, BorderLayout.SOUTH)
        add(northPanel, BorderLayout.NORTH)

        qbomTree.isRootVisible = false
        add(JScrollPane(qbomTree), BorderLayout.CENTER)

        add(statusLabel, BorderLayout.SOUTH)
    }

    fun performScan() {
        statusLabel.text = "Scanning for PQC assets..."
        lastAssets = scanner.scanProject()
        rebuildTree()
        val pqcCount = lastAssets.count { it.type == QuantumAssetType.PQC_ALGORITHM }
        val vulnCount = lastAssets.count { it.type == QuantumAssetType.QUANTUM_VULNERABLE }
        statusLabel.text = "Found ${lastAssets.size} asset(s): $pqcCount PQC, $vulnCount vulnerable"
        findExportButton()?.isEnabled = lastAssets.isNotEmpty()
    }

    fun setAssets(assets: List<QuantumAsset>) {
        lastAssets = assets
        rebuildTree()
        statusLabel.text = "Loaded ${assets.size} PQC asset(s)"
        findExportButton()?.isEnabled = assets.isNotEmpty()
    }

    fun getScanner(): QuantumScanner = scanner

    private fun rebuildTree() {
        val root = DefaultMutableTreeNode("Post-Quantum Assets")

        val byType = lastAssets.groupBy { it.type }
        val typeOrder =
            listOf(
                QuantumAssetType.PQC_ALGORITHM to "PQC Algorithms",
                QuantumAssetType.HYBRID_KEY_EXCHANGE to "Hybrid Key Exchange",
                QuantumAssetType.QUANTUM_VULNERABLE to "Quantum Vulnerable",
                QuantumAssetType.PQC_LIBRARY to "PQC Libraries",
                QuantumAssetType.PQC_PROTOCOL to "PQC Protocols",
                QuantumAssetType.PQC_STANDARD to "PQC Standards",
                QuantumAssetType.PQC_POLICY to "PQC Policies",
            )

        typeOrder.forEach { (type, label) ->
            val assetsOfType = byType[type] ?: return@forEach
            if (assetsOfType.isEmpty()) return@forEach
            val typeNode = DefaultMutableTreeNode("$label (${assetsOfType.size})")
            assetsOfType.groupBy { it.subtype }.forEach { (subtype, assets) ->
                val subtypeNode = DefaultMutableTreeNode("$subtype (${assets.size})")
                assets.forEach { asset ->
                    val node = DefaultMutableTreeNode("${asset.name} @ ${asset.sourceFile}:${asset.lineNumber}")
                    if (asset.properties.isNotEmpty()) {
                        asset.properties.forEach { (k, v) ->
                            node.add(DefaultMutableTreeNode("$k: $v"))
                        }
                    }
                    subtypeNode.add(node)
                }
                typeNode.add(subtypeNode)
            }
            root.add(typeNode)
        }

        qbomTree.model = DefaultTreeModel(root)
    }

    private fun showExportDialog() {
        if (lastAssets.isEmpty()) {
            statusLabel.text = "No PQC assets to export. Scan first."
            return
        }

        val chooser = JFileChooser()
        chooser.dialogTitle = "Save QBOM"
        chooser.selectedFile = File(project.basePath ?: ".", "qbom-${project.name}.json")
        chooser.addChoosableFileFilter(FileNameExtensionFilter("CycloneDX QBOM (*.json)", "json"))

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".json")) {
                file = File(file.parentFile, file.name + ".json")
            }
            try {
                val content = generator.generate(lastAssets)
                file.writeText(content)
                ApplicationManager.getApplication().invokeLater {
                    try {
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    } catch (_: Exception) {
                    }
                }
                statusLabel.text = "Exported ${lastAssets.size} asset(s) to ${file.absolutePath}"
            } catch (e: Exception) {
                statusLabel.text = "Export failed: ${e.message}"
            }
        }
    }

    private fun findExportButton(): JButton? {
        val north = getComponent(0) as? JBPanel<*>
        val controlPanel = north?.getComponent(1) as? JBPanel<*>
        return controlPanel?.components?.filterIsInstance<JButton>()?.find { it.name == "exportQbomButton" }
    }
}
