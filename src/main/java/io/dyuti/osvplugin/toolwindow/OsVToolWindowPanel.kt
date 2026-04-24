// OSV Vulnerability Scanner Tool Window Panel
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import io.dyuti.osvplugin.api.OsVApiService
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.utils.CacheManager
import io.dyuti.osvplugin.utils.SeverityUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Tool Window Panel for OSV Vulnerability Scanner
 */
class OsVToolWindowPanel @Suppress("UNUSED_PARAMETER") constructor(project: Project) : JBPanel<OsVToolWindowPanel>(BorderLayout()) {
    private val apiService = OsVApiService.getInstance()
    private val cacheManager = CacheManager.getInstance()
    
    private val scanButton = JButton("Scan Dependencies")
    private val filterTextField = JBTextField()
    private val vulnerabilityTable = JTable()
    private val statusLabel = JLabel("Ready")
    
    init {
        setupUI()
        setupActions()
        updateStatus("OSV Vulnerability Scanner initialized")
    }
    
    private fun setupUI() {
        // Create table model
        val columnNames = arrayOf("ID", "Summary", "Severity", "Affected Version", "Fixed Version")
        val tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        vulnerabilityTable.model = tableModel
        
        // Setup filter field
        filterTextField.text = "Filter vulnerabilities..."
        
        // Setup status label
        statusLabel.foreground = Color.GRAY
        
        // Create table scroll pane
        val scrollPane = JBScrollPane(vulnerabilityTable)
        
        // Create button panel
        val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(scanButton)
        buttonPanel.add(JLabel("Filter:"))
        buttonPanel.add(filterTextField)
        
        // Create status panel
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
        
        filterTextField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filterTable()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filterTable()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filterTable()
        })
    }
    
    private fun performScan() {
        updateStatus("Scanning dependencies...")
        
        @Suppress("UNUSED_VARIABLE")
        val testDependencies = listOf(
            Dependency("com.example", "1.0.0", "Maven", "compile", false)
        )
        
        scanButton.isEnabled = false
        updateStatus("Scan complete (placeholder - implement full scan)")
        scanButton.isEnabled = true
    }
    
    private fun filterTable() {
        // Implement filtering logic
        val filterText = filterTextField.text.lowercase()
        
        val tableModel = vulnerabilityTable.model as DefaultTableModel
        val rowsToRemove = mutableListOf<Int>()
        
        for (i in 0 until tableModel.rowCount) {
            val id = tableModel.getValueAt(i, 0).toString()
            val summary = tableModel.getValueAt(i, 1).toString()
            
            if (!id.lowercase().contains(filterText) && 
                !summary.lowercase().contains(filterText)) {
                rowsToRemove.add(i)
            }
        }
        
        // Remove rows (in reverse order to avoid index shifting)
        for (i in rowsToRemove.reversed()) {
            tableModel.removeRow(i)
        }
    }
    
    private fun updateStatus(message: String) {
        statusLabel.text = message
    }
    
    private fun addVulnerability(vulnerability: Vulnerability) {
        val tableModel = vulnerabilityTable.model as DefaultTableModel
        
        val severityIcon = SeverityUtil.getSeverityIcon(vulnerability.severity)
        
        tableModel.addRow(
            arrayOf(
                vulnerability.id,
                vulnerability.summary.take(50) + (if (vulnerability.summary.length > 50) "..." else ""),
                "$severityIcon ${vulnerability.severity}",
                vulnerability.affectedVersions.joinToString(", "),
                vulnerability.fixedVersions.joinToString(", ")
            )
        )
    }
}
