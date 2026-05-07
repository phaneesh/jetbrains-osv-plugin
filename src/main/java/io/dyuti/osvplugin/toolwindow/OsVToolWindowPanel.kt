// OSV Vulnerability Scanner Tool Window Panel
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.Tree
import io.dyuti.osvplugin.api.OsVApiService
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.parser.GradleParser
import io.dyuti.osvplugin.parser.MavenParser
import io.dyuti.osvplugin.parser.NpmParser
import io.dyuti.osvplugin.parser.PipParser
import io.dyuti.osvplugin.utils.CacheManager
import io.dyuti.osvplugin.utils.SeverityUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * Tool Window Panel for OSV Vulnerability Scanner
 */
class OsVToolWindowPanel
    @Suppress("UNUSED_PARAMETER")
    constructor(
        project: Project,
    ) : JBPanel<OsVToolWindowPanel>(BorderLayout()) {
        private val apiService = OsVApiService.getInstance()
        private val cacheManager = CacheManager.getInstance()
        private val project: Project = project

        private val scanButton = JButton("Scan Dependencies")
        private val filterTextField = JBTextField()
        private val vulnerabilityTree = Tree()
        private val statusLabel = JLabel("Ready")

        private val treeModelBuilder = OsVTreeModelBuilder()
        private val parsedDependencies = mutableMapOf<VirtualFile, List<Dependency>>()

        init {
            setupUI()
            setupActions()
            setupTreeListeners()
            updateStatus("OSV Vulnerability Scanner initialized")
        }

        private fun setupUI() {
            // Setup tree model
            vulnerabilityTree.model = treeModelBuilder.getTreeModel()
            vulnerabilityTree.cellRenderer = SeverityTreeCellRenderer()
            // Hide root node for cleaner tree view
            vulnerabilityTree.setRootVisible(false)
            vulnerabilityTree.setShowsRootHandles(true)

            // Setup filter field
            filterTextField.text = "Filter vulnerabilities..."

            // Setup status label
            statusLabel.foreground = Color.GRAY

            // Create tree scroll pane
            val scrollPane = JBScrollPane(vulnerabilityTree)

            // Create button panel
            val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
            buttonPanel.add(scanButton)
            buttonPanel.add(JLabel("Filter:"))
            buttonPanel.add(filterTextField)

            // Create status panel (without progress bar)
            val statusPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
            statusPanel.add(statusLabel)

            // Add components to main panel
            add(buttonPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            add(statusPanel, BorderLayout.SOUTH)
        }

        private fun setupActions() {
            scanButton.addActionListener { _ ->
                performScan()
            }

            filterTextField.document.addDocumentListener(
                object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filterTree()

                    override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filterTree()

                    override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filterTree()
                },
            )

            // Add right-click popup menu for vulnerability actions
            val popupMenu = JPopupMenu()
            val openLinkItem = JMenuItem("Open Vulnerability Link")
            openLinkItem.addActionListener { _ ->
                val selectedNode = vulnerabilityTree.lastSelectedPathComponent as? VulnerabilityTreeNode
                if (selectedNode != null) {
                    openVulnerabilityLink(selectedNode.vulnerability)
                }
            }
            popupMenu.add(openLinkItem)

            val navigateToLineItem = JMenuItem("Navigate to Line")
            navigateToLineItem.addActionListener { _ ->
                val selectedNode = vulnerabilityTree.lastSelectedPathComponent as? VulnerabilityTreeNode
                if (selectedNode != null) {
                    navigateToLine(selectedNode)
                }
            }
            popupMenu.add(navigateToLineItem)

            val copyIdItem = JMenuItem("Copy ID")
            copyIdItem.addActionListener { _ ->
                val selectedNode = vulnerabilityTree.lastSelectedPathComponent as? VulnerabilityTreeNode
                if (selectedNode != null) {
                    val id = selectedNode.vulnerability.id
                    val clipboard =
                        java.awt.Toolkit
                            .getDefaultToolkit()
                            .systemClipboard
                    clipboard.setContents(java.awt.datatransfer.StringSelection(id), null)
                }
            }
            popupMenu.add(copyIdItem)

            val autoFixItem = JMenuItem("Auto Fix Version")
            autoFixItem.addActionListener { _ ->
                val selectedNode = vulnerabilityTree.lastSelectedPathComponent as? VulnerabilityTreeNode
                if (selectedNode != null) {
                    autoFixVulnerability(selectedNode.vulnerability)
                }
            }
            popupMenu.add(autoFixItem)

            vulnerabilityTree.componentPopupMenu = popupMenu
        }

        private fun setupTreeListeners() {
            // Double-click to open vulnerability
            vulnerabilityTree.addMouseListener(
                object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(e: java.awt.event.MouseEvent) {
                        if (e.clickCount == 2) {
                            val selectedNode = vulnerabilityTree.lastSelectedPathComponent
                            if (selectedNode is VulnerabilityTreeNode) {
                                navigateToLine(selectedNode)
                            }
                        }
                    }
                },
            )

            // Enter key to open vulnerability
            vulnerabilityTree.addKeyListener(
                object : java.awt.event.KeyAdapter() {
                    override fun keyPressed(e: java.awt.event.KeyEvent) {
                        if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                            val selectedNode = vulnerabilityTree.lastSelectedPathComponent
                            if (selectedNode is VulnerabilityTreeNode) {
                                navigateToLine(selectedNode)
                            }
                        }
                    }
                },
            )
        }

        private fun performScan() {
            treeModelBuilder.buildModel(emptyMap())
            scanButton.isEnabled = false
            updateStatus("Scanning dependencies...")

            com.intellij.openapi.progress.ProgressManager.getInstance().run(
                object : com.intellij.openapi.progress.Task.Backgroundable(project, "Scanning Dependencies", true) {
                    override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                        indicator.text = "Scanning dependencies..."

                        val moduleFiles = mutableListOf<VirtualFile>()
                        val basePath = project.basePath ?: "."
                        collectModuleFiles(basePath, moduleFiles)

                        val totalFiles = moduleFiles.size
                        if (totalFiles == 0) {
                            updateStatus("No dependency files found (pom.xml, build.gradle, etc.)")
                            return
                        }

                        val vulnerabilitiesByModule = mutableMapOf<VirtualFile, List<Vulnerability>>()

                        for ((index, moduleFile) in moduleFiles.withIndex()) {
                            indicator.checkCanceled()
                            indicator.fraction = (index + 1).toDouble() / totalFiles

                            updateStatus("Scanning ${moduleFile.name}...")

                            try {
                                val dependencies = parseDependencies(moduleFile)
                                parsedDependencies[moduleFile] = dependencies

                                if (dependencies.isNotEmpty()) {
                                    val vulnerabilities = queryVulnerabilities(dependencies)
                                    if (vulnerabilities.isNotEmpty()) {
                                        vulnerabilitiesByModule[moduleFile] = vulnerabilities
                                    }
                                }
                            } catch (e: Exception) {
                                updateStatus("Error scanning ${moduleFile.name}: ${e.message}")
                            }
                        }

                        treeModelBuilder.buildModel(vulnerabilitiesByModule)

                        val totalCount = treeModelBuilder.getVulnerabilityCount()
                        updateStatus("Scan complete: $totalCount vulnerabilities found")
                    }

                    override fun onFinished() {
                        scanButton.isEnabled = true
                    }
                },
            )
        }

        private fun filterTree() {
            // Filter tree based on search text
            val filterText = filterTextField.text.lowercase()
            val root = treeModelBuilder.getTreeModel().root as DefaultMutableTreeNode

            if (filterText.isEmpty()) {
                // Expand all nodes when no filter
                expandAll(vulnerabilityTree, TreePath(root.path), true)
                return
            }

            // Simple filter - expand nodes that match
            for (i in 0 until root.childCount) {
                val moduleNode = root.getChildAt(i) as ModuleTreeNode
                for (j in 0 until moduleNode.childCount) {
                    val severityNode = moduleNode.getChildAt(j) as SeverityGroupTreeNode
                    for (k in 0 until severityNode.childCount) {
                        val vulnNode = severityNode.getChildAt(k) as VulnerabilityTreeNode
                        val displayId =
                            if (vulnNode.vulnerability.cveIds.isNotEmpty()) {
                                vulnNode.vulnerability.cveIds.first()
                            } else {
                                vulnNode.vulnerability.id
                            }
                        val matches =
                            displayId.contains(filterText, ignoreCase = true) ||
                                vulnNode.vulnerability.summary.contains(filterText, ignoreCase = true)

                        if (matches) {
                            // Expand parent nodes to show the matching vulnerability
                            vulnerabilityTree.expandPath(TreePath(moduleNode.path))
                            vulnerabilityTree.expandPath(TreePath(severityNode.path))
                        }
                    }
                }
            }
        }

        private fun expandAll(
            tree: JTree,
            path: TreePath,
            expand: Boolean,
        ) {
            val node = path.lastPathComponent as DefaultMutableTreeNode
            if (node.childCount > 0) {
                for (i in 0 until node.childCount) {
                    val childNode = node.getChildAt(i) as DefaultMutableTreeNode
                    val childPath = path.pathByAddingChild(childNode)
                    expandAll(tree, childPath, expand)
                }
                if (expand) {
                    tree.expandPath(path)
                } else {
                    tree.collapsePath(path)
                }
            }
        }

        private fun updateStatus(message: String) {
            statusLabel.text = message
            // Update tool window status bar
            val toolWindow =
                com.intellij.openapi.wm.ToolWindowManager
                    .getInstance(project)
                    .getToolWindow("OSV Vulnerability Scanner")
            toolWindow?.let {
                // Use status bar to show message
                val statusBar =
                    com.intellij.openapi.wm.WindowManager
                        .getInstance()
                        .getStatusBar(project)
                statusBar?.setInfo(message)
            }
        }

        private fun openVulnerabilityLink(vulnerability: Vulnerability) {
            val id = if (vulnerability.cveIds.isNotEmpty()) vulnerability.cveIds.first() else vulnerability.id
            val url = "https://osv.dev/vulnerability/$id"

            try {
                val desktop = java.awt.Desktop.getDesktop()
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(java.net.URI(url))
                }
            } catch (e: Exception) {
                updateStatus("Failed to open link: ${e.message}")
            }
        }

        private fun navigateToLine(vulnNode: VulnerabilityTreeNode) {
            val lineNumber = vulnNode.lineNumber
            val moduleFile = vulnNode.moduleFile

            if (lineNumber == null || moduleFile == null) {
                updateStatus("Line number not available for this vulnerability")
                return
            }

            if (!moduleFile.isValid) {
                updateStatus("Module file no longer exists")
                return
            }

            try {
                // Open file at line number
                com.intellij.openapi.fileEditor.FileEditorManager
                    .getInstance(project)
                    .openFile(moduleFile, true)

                // Get all editors for the file
                val editors =
                    com.intellij.openapi.fileEditor.FileEditorManager
                        .getInstance(project)
                        .getAllEditors(moduleFile)
                if (editors.isNotEmpty()) {
                    // Try to get TextEditor (has document, caretModel, scrollingModel)
                    val textEditor = editors.filterIsInstance<com.intellij.openapi.fileEditor.TextEditor>().firstOrNull()
                    if (textEditor != null) {
                        val editor = textEditor.editor
                        val document = editor.document
                        if (document != null) {
                            val lineOffset = document.getLineStartOffset(lineNumber - 1) // 0-based index
                            val lineEndOffset = document.getLineEndOffset(lineNumber - 1)

                            // Find the best caret position (start of line or end of line)
                            val caretOffset = if (lineOffset < lineEndOffset) lineOffset else lineOffset - 1

                            editor.caretModel.moveToOffset(caretOffset)
                            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                            updateStatus("Navigated to line $lineNumber")
                        } else {
                            updateStatus("Could not get document for file")
                        }
                    } else {
                        updateStatus("Could not open text editor for file")
                    }
                } else {
                    updateStatus("Could not open file editor")
                }
            } catch (e: Exception) {
                updateStatus("Failed to navigate to line: ${e.message}")
            }
        }

        private fun autoFixVulnerability(vulnerability: Vulnerability) {
            // Find the dependency that matches this vulnerability
            val moduleFileWithDep =
                parsedDependencies.entries.firstOrNull { (_, deps) ->
                    deps.any { it.name == vulnerability.id || vulnerability.id.contains(it.name.split(":").last()) }
                }

            if (moduleFileWithDep == null) {
                updateStatus("Could not find dependency ${vulnerability.id} in any module")
                return
            }

            val (moduleFile, dependencies) = moduleFileWithDep
            val fixVersion = vulnerability.fixedVersions.firstOrNull() ?: "N/A"

            if (fixVersion == "N/A") {
                updateStatus("No fix version available for ${vulnerability.id}")
                return
            }

            try {
                val fileName = moduleFile.name
                val content = moduleFile.inputStream.bufferedReader().use { it.readText() }
                val updatedContent =
                    when {
                        fileName == "pom.xml" -> {
                            updateMavenDependency(content, dependencies, vulnerability.id, fixVersion)
                        }

                        fileName == "build.gradle" || fileName == "build.gradle.kts" -> {
                            updateGradleDependency(
                                content,
                                dependencies,
                                vulnerability.id,
                                fixVersion,
                            )
                        }

                        fileName == "package.json" -> {
                            updateNpmDependency(content, dependencies, vulnerability.id, fixVersion)
                        }

                        fileName == "requirements.txt" -> {
                            updatePipDependency(content, dependencies, vulnerability.id, fixVersion)
                        }

                        else -> {
                            content
                        }
                    }

                if (updatedContent != content) {
                    moduleFile.setBinaryContent(updatedContent.toByteArray())
                    updateStatus("Auto-fixed ${vulnerability.id} to version $fixVersion in ${moduleFile.name}")
                    // Refresh the scan
                    performScan()
                } else {
                    updateStatus("Could not find dependency ${vulnerability.id} in ${moduleFile.name}")
                }
            } catch (e: Exception) {
                updateStatus("Failed to auto-fix ${vulnerability.id}: ${e.message}")
            }
        }

        private fun updateMavenDependency(
            content: String,
            dependencies: List<Dependency>,
            vulnName: String,
            fixVersion: String,
        ): String {
            var updatedContent = content

            // Find the dependency and update version
            val depPattern = """<dependency>.*?</dependency>""".toRegex(RegexOption.DOT_MATCHES_ALL)

            depPattern.findAll(content).forEach { match ->
                val fullMatch = match.value
                val artifactIdMatch = """<artifactId>([^<]+)</artifactId>""".toRegex().find(fullMatch)
                val versionMatch = """<version>([^<]+)</version>""".toRegex().find(fullMatch)

                if (artifactIdMatch != null) {
                    val artifactId = artifactIdMatch.groupValues[1]
                    val groupIdMatch = """<groupId>([^<]+)</groupId>""".toRegex().find(fullMatch)
                    val groupId = groupIdMatch?.groupValues?.get(1) ?: ""

                    // Check if this matches our vulnerability
                    val depName = "$groupId:$artifactId"
                    if (vulnName == depName || vulnName.contains(artifactId, ignoreCase = true)) {
                        val oldVersion = versionMatch?.groupValues?.get(1)
                        if (oldVersion != null) {
                            // Check if it's a property reference like ${property.name}
                            if (oldVersion.contains("\${")) {
                                val propertyName = oldVersion.replace("\${", "").replace("}", "")
                                updatedContent = updateMavenProperty(updatedContent, propertyName, fixVersion)
                            } else {
                                updatedContent = updatedContent.replace(oldVersion, fixVersion)
                            }
                        }
                    }
                }
            }

            return updatedContent
        }

        private fun updateMavenProperty(
            content: String,
            propertyName: String,
            newValue: String,
        ): String {
            // Update property value
            val propPattern = """<$propertyName>([^<]+)</$propertyName>""".toRegex()
            val match = propPattern.find(content)
            if (match != null) {
                val oldValue = match.groupValues[1]
                return content.replace(oldValue, newValue)
            }
            return content
        }

        private fun updateGradleDependency(
            content: String,
            dependencies: List<Dependency>,
            vulnName: String,
            fixVersion: String,
        ): String {
            var updatedContent = content

            val depPattern =
                Regex(
                    """(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(\s*['"]([^:]+):([^:]+):([^'"]+)['"]\s*\)""",
                )

            depPattern.findAll(content).forEach { match ->
                val group = match.groupValues[1]
                val artifactId = match.groupValues[3]
                val version = match.groupValues[4]

                val depName = "$group:$artifactId"
                if (vulnName == depName || vulnName.contains(artifactId, ignoreCase = true)) {
                    updatedContent =
                        updatedContent.replace(
                            "'$group:$artifactId:$version'",
                            "'$group:$artifactId:$fixVersion'",
                        )
                }
            }

            return updatedContent
        }

        private fun updateNpmDependency(
            content: String,
            dependencies: List<Dependency>,
            vulnName: String,
            fixVersion: String,
        ): String {
            var updatedContent = content

            // Try to find the package name
            val depName = vulnName.split(":").lastOrNull() ?: vulnName
            val depPattern = """"$depName"\s*:\s*"[^"]+"""".toRegex()

            depPattern.findAll(content).forEach { match ->
                val oldLine = match.value
                val oldVersionMatch = """"version"\s*:\s*"([^"]+)"""".toRegex().find(oldLine)
                if (oldVersionMatch != null) {
                    val oldVersion = oldVersionMatch.groupValues[1]
                    updatedContent = updatedContent.replace(oldLine, oldLine.replace(oldVersion, fixVersion))
                }
            }

            return updatedContent
        }

        private fun updatePipDependency(
            content: String,
            dependencies: List<Dependency>,
            vulnName: String,
            fixVersion: String,
        ): String {
            var updatedContent = content

            // Try to find the package name
            val depName = vulnName.split(":").lastOrNull() ?: vulnName
            val depPattern = """^$depName==[^\s]+""".toRegex(RegexOption.MULTILINE)

            depPattern.findAll(content).forEach { match ->
                val oldLine = match.value
                val parts = oldLine.split("==")
                if (parts.size == 2) {
                    updatedContent = updatedContent.replace(oldLine, "${parts[0]}==$fixVersion")
                }
            }

            return updatedContent
        }

        /**
         * Collect module files (pom.xml, build.gradle, etc.) from directory
         */
        private fun collectModuleFiles(
            directoryPath: String,
            moduleFiles: MutableList<VirtualFile>,
        ) {
            val directory =
                com.intellij.openapi.vfs.LocalFileSystem
                    .getInstance()
                    .findFileByPath(directoryPath)
            if (directory == null || !directory.isValid) return

            // Check for module files
            for (fileName in listOf("pom.xml", "build.gradle", "build.gradle.kts", "package.json", "requirements.txt")) {
                val moduleFile = directory.findChild(fileName)
                if (moduleFile != null && moduleFile.isValid) {
                    moduleFiles.add(moduleFile)
                }
            }

            // Recursively search subdirectories
            for (child in directory.children) {
                if (child.isDirectory && child.isValid) {
                    collectModuleFiles(child.path, moduleFiles)
                }
            }
        }

        /**
         * Parse dependencies from a module file
         */
        private fun parseDependencies(moduleFile: VirtualFile): List<Dependency> {
            val content = moduleFile.inputStream.bufferedReader().use { it.readText() }
            val fileName = moduleFile.name

            val parsers =
                listOf(
                    MavenParser(),
                    GradleParser(),
                    NpmParser(),
                    PipParser(),
                )

            val dependencies = mutableListOf<Dependency>()
            for (parser in parsers) {
                if (parser.canHandle(fileName)) {
                    try {
                        val parsed = parser.parse(fileName, content)
                        dependencies.addAll(parsed)
                    } catch (e: Exception) {
                        System.err.println("Error parsing dependencies from $fileName: ${e.message}")
                    }
                }
            }

            return dependencies
        }

        /**
         * Query OSV API for vulnerabilities
         */
        private fun queryVulnerabilities(dependencies: List<Dependency>): List<Vulnerability> {
            val vulnerabilities = mutableListOf<Vulnerability>()
            for (dep in dependencies) {
                try {
                    val depVulns =
                        apiService.queryVulnerabilities(
                            dep.name,
                            dep.ecosystem,
                            dep.version,
                        )
                    // Preserve the source dependency's line number so tree double-click
                    // navigates to the exact line where the dependency is declared.
                    // TODO: for transitive deps, navigate to parent dep line.
                    vulnerabilities.addAll(
                        depVulns.map { vuln -> vuln.copy(lineNumber = dep.lineNumber) },
                    )
                } catch (e: Exception) {
                    System.err.println("Error querying vulnerabilities for ${dep.name}: ${e.message}")
                }
            }
            return vulnerabilities
        }
    }

// Tree node classes

/**
 * Tree node representing a project module (pom.xml, build.gradle, etc.)
 */
class ModuleTreeNode(
    val moduleFile: VirtualFile,
) : DefaultMutableTreeNode(moduleFile) {
    private var findingCount = -1

    override fun toString(): String = moduleFile.name

    fun getFindingCount(): Int {
        if (findingCount < 0) {
            findingCount = 0
            for (i in 0 until childCount) {
                val severityNode = getChildAt(i) as SeverityGroupTreeNode
                findingCount += severityNode.childCount
            }
        }
        return findingCount
    }
}

/**
 * Tree node representing a vulnerability
 */
class VulnerabilityTreeNode(
    val vulnerability: Vulnerability,
    val moduleFile: VirtualFile?,
) : DefaultMutableTreeNode(vulnerability) {
    private val displayId =
        if (vulnerability.cveIds.isNotEmpty()) {
            vulnerability.cveIds.first()
        } else {
            vulnerability.id
        }

    val lineNumber: Int? = vulnerability.lineNumber

    override fun toString(): String {
        val fixVersion = vulnerability.fixedVersions.firstOrNull() ?: "N/A"
        return "$displayId - ${vulnerability.summary} (Fix: $fixVersion)"
    }
}

/**
 * Tree node representing a severity group
 */
class SeverityGroupTreeNode(
    val severity: OsVSeverity,
    val groupName: String,
) : DefaultMutableTreeNode(groupName) {
    override fun toString(): String = "$groupName ($childCount vulnerabilit${if (childCount == 0) "y" else "ies"})"
}

/**
 * Tree model builder for vulnerabilities organized by module and severity
 */
class OsVTreeModelBuilder {
    var model: DefaultTreeModel
    private var root: DefaultMutableTreeNode

    init {
        root = DefaultMutableTreeNode("OSV Vulnerabilities")
        model = DefaultTreeModel(root)
    }

    fun getTreeModel(): DefaultTreeModel = model

    /**
     * Build tree structure from vulnerabilities grouped by module
     */
    fun buildModel(vulnerabilitiesByModule: Map<VirtualFile, List<Vulnerability>>) {
        root.removeAllChildren()
        model.reload(root)

        // Track seen module paths to prevent duplicates
        val seenModulePaths = mutableSetOf<String>()

        // Get sorted module files by path
        val sortedModules = vulnerabilitiesByModule.keys.sortedBy { it.path }

        for (moduleFile in sortedModules) {
            val vulnerabilities = vulnerabilitiesByModule[moduleFile] ?: continue
            val modulePath = moduleFile.path

            // Skip if we've already added this module (use path as unique identifier)
            if (modulePath in seenModulePaths) {
                continue
            }
            seenModulePaths.add(modulePath)

            // Create module node (uses file path as unique identifier)
            val moduleNode = ModuleTreeNode(moduleFile)

            // Group vulnerabilities by severity
            val vulnerabilitiesBySeverity = vulnerabilities.groupBy { it.severity }

            // Add severity groups in order: CRITICAL, HIGH, MEDIUM, LOW
            val severityOrder =
                listOf(
                    OsVSeverity.CRITICAL to "Critical",
                    OsVSeverity.HIGH to "High",
                    OsVSeverity.MEDIUM to "Medium",
                    OsVSeverity.LOW to "Low",
                )

            for ((severity, groupName) in severityOrder) {
                val sevVulns = vulnerabilitiesBySeverity[severity] ?: continue

                // Create severity group node
                val severityNode = SeverityGroupTreeNode(severity, groupName)

                // Add vulnerability nodes to severity group
                for (vuln in sevVulns) {
                    val vulnNode = VulnerabilityTreeNode(vuln, moduleFile)
                    severityNode.add(vulnNode)
                }

                // Only add severity group if it has vulnerabilities
                if (sevVulns.isNotEmpty()) {
                    moduleNode.add(severityNode)
                }
            }

            // Only add module node if it has vulnerabilities
            if (moduleNode.childCount > 0) {
                root.add(moduleNode)
            }
        }

        model.reload(root)
    }

    /**
     * Get total number of vulnerabilities
     */
    fun getVulnerabilityCount(): Int {
        var count = 0
        for (i in 0 until root.childCount) {
            val moduleNode = root.getChildAt(i) as ModuleTreeNode
            for (j in 0 until moduleNode.childCount) {
                val severityNode = moduleNode.getChildAt(j) as SeverityGroupTreeNode
                count += severityNode.childCount
            }
        }
        return count
    }

    /**
     * Find vulnerability node by CVE/OSV ID
     */
    fun findVulnerabilityNodeById(id: String): VulnerabilityTreeNode? {
        for (i in 0 until root.childCount) {
            val moduleNode = root.getChildAt(i) as ModuleTreeNode
            for (j in 0 until moduleNode.childCount) {
                val severityNode = moduleNode.getChildAt(j) as SeverityGroupTreeNode
                for (k in 0 until severityNode.childCount) {
                    val vulnNode = severityNode.getChildAt(k) as VulnerabilityTreeNode
                    val displayId =
                        if (vulnNode.vulnerability.cveIds.isNotEmpty()) {
                            vulnNode.vulnerability.cveIds.first()
                        } else {
                            vulnNode.vulnerability.id
                        }
                    if (displayId == id) {
                        return vulnNode
                    }
                }
            }
        }
        return null
    }
}

/**
 * Custom tree cell renderer to show icons for severity groups
 */
class SeverityTreeCellRenderer : ColoredTreeCellRenderer() {
    private var tooltipText: String? = null

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        tooltipText = null
        when (value) {
            is SeverityGroupTreeNode -> {
                icon =
                    io.dyuti.osvplugin.utils.SeverityUtil
                        .getSeverityIcon(value.severity)
                tooltipText = "${value.severity} severity vulnerabilities"
                append(
                    "${value.groupName} (${value.childCount} vulnerabilit${if (value.childCount == 0) "y" else "ies"})",
                    if (sel) SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES else SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES,
                )
                if (!sel) {
                    foreground =
                        io.dyuti.osvplugin.utils.SeverityUtil
                            .getColor(value.severity)
                }
            }

            is VulnerabilityTreeNode -> {
                val vulnerability = value.vulnerability
                icon =
                    io.dyuti.osvplugin.utils.SeverityUtil
                        .getSeverityIcon(vulnerability.severity)

                val displayId =
                    if (vulnerability.cveIds.isNotEmpty()) {
                        vulnerability.cveIds.first()
                    } else {
                        vulnerability.id
                    }

                // Add severity prefix to tooltip
                tooltipText = "${vulnerability.severity} severity - $displayId"

                // Render: CVE ID - Summary (Fix: version)
                append(displayId, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                append(" ")
                append(vulnerability.summary, SimpleTextAttributes.REGULAR_ATTRIBUTES)

                // Add fix version
                val fixVersion = vulnerability.fixedVersions.firstOrNull() ?: "N/A"
                append(" ", SimpleTextAttributes.GRAY_ATTRIBUTES)
                append("(Fix: $fixVersion)", SimpleTextAttributes.GRAY_ATTRIBUTES)

                // Add line coordinates
                val lineNumber = value.lineNumber
                if (lineNumber != null && lineNumber > 0) {
                    append(" ", SimpleTextAttributes.GRAY_ATTRIBUTES)
                    append("(Line: $lineNumber)", SimpleTextAttributes.GRAY_ATTRIBUTES)
                }
            }

            is ModuleTreeNode -> {
                icon = value.moduleFile.fileType.icon
                tooltipText = "${value.moduleFile.fileType.displayName} file"
                append(value.moduleFile.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)

                // Show finding count
                val findingCount = value.getFindingCount()
                append(
                    " ",
                    SimpleTextAttributes.GRAY_ATTRIBUTES,
                )
                append(
                    "($findingCount vulnerabilit${if (findingCount == 0) "y" else "ies"})",
                    SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES,
                )
            }
        }
    }
}
