// AIBOM Export Panel — AI/ML asset tree view + export dialog
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.Gray
import com.intellij.ui.components.JBPanel
import io.dyuti.osvplugin.aibom.AiAsset
import io.dyuti.osvplugin.aibom.AiAssetType
import io.dyuti.osvplugin.aibom.AiScanner
import io.dyuti.osvplugin.aibom.AibomGenerator
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * AIBOM export panel with AI/ML asset tree view and file save dialog.
 */
class AibomExportPanel(
    private val project: Project,
) : JBPanel<AibomExportPanel>(BorderLayout()) {
    private val statusLabel = JLabel("Click 'Scan' to discover AI/ML assets")
    private val aibomTree = JTree(DefaultMutableTreeNode("AI/ML Assets"))
    private val generator = AibomGenerator(appName = project.name, appVersion = "1.1.2")
    private val scanner = AiScanner(project)
    private var lastAssets: List<AiAsset> = emptyList()

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
        val headerLabel = JLabel("AIBOM: ${project.name}")
        headerLabel.font = headerLabel.font.deriveFont(headerLabel.font.size + 2f)
        headerPanel.add(headerLabel)
        northPanel.add(headerPanel, BorderLayout.NORTH)

        val controlPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        val scanButton = JButton("Scan Project")
        scanButton.addActionListener { performScan() }
        controlPanel.add(scanButton)

        val exportButton = JButton("Export AIBOM...")
        exportButton.addActionListener { showExportDialog() }
        exportButton.isEnabled = false
        exportButton.name = "exportAibomButton"
        controlPanel.add(exportButton)

        northPanel.add(controlPanel, BorderLayout.SOUTH)
        add(northPanel, BorderLayout.NORTH)

        aibomTree.isRootVisible = false
        add(JScrollPane(aibomTree), BorderLayout.CENTER)

        add(statusLabel, BorderLayout.SOUTH)
    }

    fun performScan() {
        statusLabel.text = "Scanning for AI/ML assets..."
        lastAssets = scanner.scanProject()
        rebuildTree()
        val llmCount = lastAssets.count { it.type == AiAssetType.LLM_API }
        val mlCount = lastAssets.count { it.type == AiAssetType.ML_FRAMEWORK }
        val genCount = lastAssets.count { it.type == AiAssetType.AI_GENERATED_CODE }
        statusLabel.text = "Found ${lastAssets.size} asset(s): $llmCount LLM, $mlCount ML, $genCount AI-generated"
        findExportButton()?.isEnabled = lastAssets.isNotEmpty()
    }

    fun setAssets(assets: List<AiAsset>) {
        lastAssets = assets
        rebuildTree()
        statusLabel.text = "Loaded ${assets.size} AI/ML asset(s)"
        findExportButton()?.isEnabled = assets.isNotEmpty()
    }

    fun getScanner(): AiScanner = scanner

    private fun rebuildTree() {
        val root = DefaultMutableTreeNode("AI/ML Assets")

        val byType = lastAssets.groupBy { it.type }
        val typeOrder =
            listOf(
                AiAssetType.LLM_API to "LLM APIs",
                AiAssetType.ML_FRAMEWORK to "ML Frameworks",
                AiAssetType.AI_ORCHESTRATION to "AI Orchestration",
                AiAssetType.VECTOR_DATABASE to "Vector Databases",
                AiAssetType.MODEL_ARTIFACT to "Model Artifacts",
                AiAssetType.AI_GENERATED_CODE to "AI-Generated Code",
                AiAssetType.MLOPS to "MLOps Platforms",
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

        aibomTree.model = DefaultTreeModel(root)
    }

    private fun showExportDialog() {
        if (lastAssets.isEmpty()) {
            statusLabel.text = "No AI/ML assets to export. Scan first."
            return
        }

        val chooser = JFileChooser()
        chooser.dialogTitle = "Save AIBOM"
        chooser.selectedFile = File(project.basePath ?: ".", "aibom-${project.name}.json")
        chooser.addChoosableFileFilter(FileNameExtensionFilter("CycloneDX AIBOM (*.json)", "json"))

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
        return controlPanel?.components?.filterIsInstance<JButton>()?.find { it.name == "exportAibomButton" }
    }
}
