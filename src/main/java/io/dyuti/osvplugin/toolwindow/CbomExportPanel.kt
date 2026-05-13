// CBOM Export Panel — cryptographic asset tree view + export dialog
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBPanel
import io.dyuti.osvplugin.cbom.CbomGenerator
import io.dyuti.osvplugin.cbom.CryptoAsset
import io.dyuti.osvplugin.cbom.CryptoAssetType
import io.dyuti.osvplugin.cbom.CryptoScanner
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * CBOM export panel with crypto asset tree view and file save dialog.
 *
 * Discovers cryptographic assets via static analysis of source files
 * (JCA/JCE APIs, TLS configs, BouncyCastle usage, etc.) and exports
 * a CycloneDX 1.6 CBOM document.
 */
class CbomExportPanel(
    private val project: Project,
) : JBPanel<CbomExportPanel>(BorderLayout()) {
    private val statusLabel = JLabel("Click 'Scan' to discover cryptographic assets")
    private val cbomTree = JTree(DefaultMutableTreeNode("Cryptographic Assets"))
    private val generator = CbomGenerator(appName = project.name, appVersion = "1.1.2")
    private val scanner = CryptoScanner(project)
    private var lastAssets: List<CryptoAsset> = emptyList()

    init {
        // ── North: header + controls ──
        val northPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val headerPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val headerLabel = JLabel("CBOM: ${project.name}")
        headerLabel.font = headerLabel.font.deriveFont(headerLabel.font.size + 2f)
        headerPanel.add(headerLabel)
        northPanel.add(headerPanel, BorderLayout.NORTH)

        val controlPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val scanButton = JButton("Scan Project")
        scanButton.addActionListener { performScan() }
        controlPanel.add(scanButton)

        val exportButton = JButton("Export CBOM...")
        exportButton.addActionListener { showExportDialog() }
        exportButton.isEnabled = false
        exportButton.name = "exportCbomButton"
        controlPanel.add(exportButton)

        northPanel.add(controlPanel, BorderLayout.SOUTH)
        add(northPanel, BorderLayout.NORTH)

        // ── Center: asset tree ──
        cbomTree.isRootVisible = false
        add(JScrollPane(cbomTree), BorderLayout.CENTER)

        // ── South: status bar ──
        add(statusLabel, BorderLayout.SOUTH)
    }

    /** Discovers cryptographic assets and rebuilds the tree. */
    fun performScan() {
        statusLabel.text = "Scanning for cryptographic assets..."
        lastAssets = scanner.scanProject()
        rebuildTree()
        statusLabel.text = "Found ${lastAssets.size} cryptographic asset(s)"
        findExportButton()?.isEnabled = lastAssets.isNotEmpty()
    }

    /** Feed pre-discovered assets (e.g., from test or external source). */
    fun setAssets(assets: List<CryptoAsset>) {
        lastAssets = assets
        rebuildTree()
        statusLabel.text = "Loaded ${assets.size} cryptographic asset(s)"
        findExportButton()?.isEnabled = assets.isNotEmpty()
    }

    /** Expose the underlying scanner for programmatic use. */
    fun getScanner(): CryptoScanner = scanner

    private fun rebuildTree() {
        val root = DefaultMutableTreeNode("Cryptographic Assets")

        // Group by type
        val byType = lastAssets.groupBy { it.type }
        val typeOrder =
            listOf(
                CryptoAssetType.ALGORITHM to "Algorithms",
                CryptoAssetType.PROTOCOL to "Protocols",
                CryptoAssetType.CERTIFICATE to "Certificates",
                CryptoAssetType.RELATED_CRYPTO_MATERIAL to "Related Material",
            )

        typeOrder.forEach { (type, label) ->
            val assetsOfType = byType[type] ?: return@forEach
            if (assetsOfType.isEmpty()) return@forEach

            val typeNode = DefaultMutableTreeNode("$label (${assetsOfType.size})")

            // Group by subtype within type
            assetsOfType.groupBy { it.subtype }.forEach { (subtype, assets) ->
                val subtypeNode = DefaultMutableTreeNode("$subtype (${assets.size})")
                assets.forEach { asset ->
                    val assetNode = DefaultMutableTreeNode("${asset.name} @ ${asset.sourceFile}:${asset.lineNumber}")
                    // Add property nodes
                    if (asset.properties.isNotEmpty()) {
                        asset.properties.forEach { (k, v) ->
                            assetNode.add(DefaultMutableTreeNode("$k: $v"))
                        }
                    }
                    subtypeNode.add(assetNode)
                }
                typeNode.add(subtypeNode)
            }

            root.add(typeNode)
        }

        cbomTree.model = DefaultTreeModel(root)
    }

    private fun showExportDialog() {
        if (lastAssets.isEmpty()) {
            statusLabel.text = "No crypto assets to export. Scan first."
            return
        }

        val chooser = JFileChooser()
        chooser.dialogTitle = "Save CBOM"
        chooser.selectedFile = File(project.basePath ?: ".", "cbom-${project.name}.json")
        chooser.addChoosableFileFilter(
            FileNameExtensionFilter("CycloneDX CBOM (*.json)", "json"),
        )

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".json")) {
                file = File(file.parentFile, file.name + ".json")
            }

            try {
                val content = generator.generate(lastAssets)
                file.writeText(content)

                // VFS refresh on EDT to avoid write-thread errors
                ApplicationManager.getApplication().invokeLater {
                    try {
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    } catch (_: Exception) {
                        // best-effort
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
        return controlPanel?.components?.filterIsInstance<JButton>()?.find { it.name == "exportCbomButton" }
    }
}
