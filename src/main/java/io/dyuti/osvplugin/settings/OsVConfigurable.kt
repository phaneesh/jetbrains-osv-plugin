// OSV Vulnerability Scanner Settings Page
package io.dyuti.osvplugin.settings

import com.intellij.openapi.options.Configurable
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.config.OsVConfig
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

/**
 * Settings configurable for OSV Vulnerability Scanner
 */
class OsVConfigurable : Configurable {
    private var config: OsVConfig = OsVConfig()

    private lateinit var minimumSeverityCombo: JComboBox<OsVSeverity>
    private lateinit var inspectionEnabledCheckbox: JCheckBox
    private lateinit var cacheTtlSpinner: JSpinner
    private lateinit var rateLimitEnabledCheckbox: JCheckBox
    private lateinit var rateLimitRequestsSpinner: JSpinner
    private lateinit var scanDirectCheckbox: JCheckBox
    private lateinit var scanTransitiveCheckbox: JCheckBox
    private lateinit var githubAdvisoryEnabledCheckbox: JCheckBox
    private lateinit var githubTokenField: JTextField
    private lateinit var licenseScanningEnabledCheckbox: JCheckBox
    private lateinit var focusModeEnabledCheckbox: JCheckBox
    private lateinit var baseBranchField: JTextField
    private lateinit var sarifExportPathField: JTextField
    private lateinit var ignoredPackagesField: JTextField

    // Organization Management
    private lateinit var orgManagementEnabledCheckbox: JCheckBox
    private lateinit var currentOrgField: JTextField

    // Jira Integration
    private lateinit var jiraEnabledCheckbox: JCheckBox
    private lateinit var jiraBaseUrlField: JTextField
    private lateinit var jiraProjectKeyField: JTextField
    private lateinit var jiraEmailField: JTextField
    private lateinit var jiraTokenField: JTextField

    override fun getDisplayName(): String = "OSV Scanner"

    override fun createComponent(): javax.swing.JComponent {
        val panel = JPanel()
        panel.layout = java.awt.GridLayout(0, 2, 10, 10)

        // Minimum Severity
        panel.add(JLabel("Minimum Severity:"))
        minimumSeverityCombo = JComboBox(OsVSeverity.values())
        minimumSeverityCombo.selectedItem = OsVSeverity.MEDIUM
        panel.add(minimumSeverityCombo)

        // Inspection Enabled
        inspectionEnabledCheckbox = JCheckBox("Enable Inspection")
        inspectionEnabledCheckbox.isSelected = true
        panel.add(inspectionEnabledCheckbox)

        // Cache TTL
        panel.add(JLabel("Cache TTL (hours):"))
        cacheTtlSpinner = JSpinner(SpinnerNumberModel(1, 1, 24, 1))
        panel.add(cacheTtlSpinner)

        // Rate Limit
        rateLimitEnabledCheckbox = JCheckBox("Enable Rate Limiting")
        rateLimitEnabledCheckbox.isSelected = true
        panel.add(rateLimitEnabledCheckbox)

        panel.add(JLabel("Requests per hour:"))
        rateLimitRequestsSpinner = JSpinner(SpinnerNumberModel(100, 1, 1000, 1))
        panel.add(rateLimitRequestsSpinner)

        // Scan options
        scanDirectCheckbox = JCheckBox("Scan Direct Dependencies")
        scanDirectCheckbox.isSelected = true
        panel.add(scanDirectCheckbox)

        scanTransitiveCheckbox = JCheckBox("Scan Transitive Dependencies")
        scanTransitiveCheckbox.isSelected = true
        panel.add(scanTransitiveCheckbox)

        // GitHub Advisory
        githubAdvisoryEnabledCheckbox = JCheckBox("Enable GitHub Advisory Integration")
        panel.add(githubAdvisoryEnabledCheckbox)

        panel.add(JLabel("GitHub Token:"))
        githubTokenField = JTextField(20)
        panel.add(githubTokenField)

        // License Scanning
        licenseScanningEnabledCheckbox = JCheckBox("Enable License Scanning")
        panel.add(licenseScanningEnabledCheckbox)

        // Focus Mode
        focusModeEnabledCheckbox = JCheckBox("Focus Mode (Show only critical)")
        panel.add(focusModeEnabledCheckbox)

        // Branch
        panel.add(JLabel("Base Branch:"))
        baseBranchField = JTextField("main", 10)
        panel.add(baseBranchField)

        // SARIF Export
        panel.add(JLabel("SARIF Export Path:"))
        sarifExportPathField = JTextField(20)
        panel.add(sarifExportPathField)

        // Ignored Packages
        panel.add(JLabel("Ignored Packages:"))
        ignoredPackagesField = JTextField(20)
        ignoredPackagesField.toolTipText = "Comma-separated list of package names to ignore"
        panel.add(ignoredPackagesField)

        // ===== Organization Management Section =====
        panel.add(JLabel("--- Organization Management ---"))
        panel.add(JLabel(""))

        orgManagementEnabledCheckbox = JCheckBox("Enable Organization Management")
        panel.add(orgManagementEnabledCheckbox)

        panel.add(JLabel("Current Organization:"))
        currentOrgField = JTextField(20)
        panel.add(currentOrgField)

        // ===== Jira Integration Section =====
        panel.add(JLabel("--- Jira Integration ---"))
        panel.add(JLabel(""))

        jiraEnabledCheckbox = JCheckBox("Enable Jira Integration")
        panel.add(jiraEnabledCheckbox)

        panel.add(JLabel("Jira Base URL:"))
        jiraBaseUrlField = JTextField(20)
        panel.add(jiraBaseUrlField)

        panel.add(JLabel("Jira Project Key:"))
        jiraProjectKeyField = JTextField(10)
        panel.add(jiraProjectKeyField)

        panel.add(JLabel("Jira Email:"))
        jiraEmailField = JTextField(20)
        panel.add(jiraEmailField)

        panel.add(JLabel("Jira API Token:"))
        jiraTokenField = JTextField(20)
        panel.add(jiraTokenField)

        return panel
    }

    override fun isModified(): Boolean =
        config.minimumSeverity != minimumSeverityCombo.selectedItem ||
            config.inspectionEnabled != inspectionEnabledCheckbox.isSelected ||
            config.cacheTtl != cacheTtlSpinner.value ||
            config.rateLimitEnabled != rateLimitEnabledCheckbox.isSelected ||
            config.rateLimitRequestsPerHour != rateLimitRequestsSpinner.value ||
            config.scanDirectDependencies != scanDirectCheckbox.isSelected ||
            config.scanTransitiveDependencies != scanTransitiveCheckbox.isSelected ||
            config.githubAdvisoryEnabled != githubAdvisoryEnabledCheckbox.isSelected ||
            config.githubToken != githubTokenField.text ||
            config.licenseScanningEnabled != licenseScanningEnabledCheckbox.isSelected ||
            config.focusModeEnabled != focusModeEnabledCheckbox.isSelected ||
            config.baseBranch != baseBranchField.text ||
            config.sarifExportPath != sarifExportPathField.text ||
            config.ignoredPackages != parseIgnoredPackages(ignoredPackagesField.text) ||
            config.orgManagementEnabled != orgManagementEnabledCheckbox.isSelected ||
            config.currentOrganization != currentOrgField.text ||
            config.jiraEnabled != jiraEnabledCheckbox.isSelected ||
            config.jiraBaseUrl != jiraBaseUrlField.text ||
            config.jiraProjectKey != jiraProjectKeyField.text ||
            config.jiraEmail != jiraEmailField.text ||
            config.jiraToken != jiraTokenField.text

    override fun apply() {
        config.minimumSeverity = minimumSeverityCombo.selectedItem as OsVSeverity
        config.inspectionEnabled = inspectionEnabledCheckbox.isSelected
        config.cacheTtl = cacheTtlSpinner.value as Int
        config.rateLimitEnabled = rateLimitEnabledCheckbox.isSelected
        config.rateLimitRequestsPerHour = rateLimitRequestsSpinner.value as Int
        config.scanDirectDependencies = scanDirectCheckbox.isSelected
        config.scanTransitiveDependencies = scanTransitiveCheckbox.isSelected
        config.githubAdvisoryEnabled = githubAdvisoryEnabledCheckbox.isSelected
        config.githubToken = githubTokenField.text
        config.licenseScanningEnabled = licenseScanningEnabledCheckbox.isSelected
        config.focusModeEnabled = focusModeEnabledCheckbox.isSelected
        config.baseBranch = baseBranchField.text
        config.sarifExportPath = sarifExportPathField.text
        config.ignoredPackages = parseIgnoredPackages(ignoredPackagesField.text)
        config.orgManagementEnabled = orgManagementEnabledCheckbox.isSelected
        config.currentOrganization = currentOrgField.text
        config.jiraEnabled = jiraEnabledCheckbox.isSelected
        config.jiraBaseUrl = jiraBaseUrlField.text
        config.jiraProjectKey = jiraProjectKeyField.text
        config.jiraEmail = jiraEmailField.text
        config.jiraToken = jiraTokenField.text

        // Save configuration
        @Suppress("DEPRECATION")
        com.intellij.openapi.components.ServiceManager
            .getService(OsVConfig::class.java)
            .loadState(config)
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        val savedConfig =
            com.intellij.openapi.components.ServiceManager
                .getService(OsVConfig::class.java)
        config = savedConfig

        minimumSeverityCombo.selectedItem = config.minimumSeverity
        inspectionEnabledCheckbox.isSelected = config.inspectionEnabled
        cacheTtlSpinner.value = config.cacheTtl
        rateLimitEnabledCheckbox.isSelected = config.rateLimitEnabled
        rateLimitRequestsSpinner.value = config.rateLimitRequestsPerHour
        scanDirectCheckbox.isSelected = config.scanDirectDependencies
        scanTransitiveCheckbox.isSelected = config.scanTransitiveDependencies
        githubAdvisoryEnabledCheckbox.isSelected = config.githubAdvisoryEnabled
        githubTokenField.text = config.githubToken ?: ""
        licenseScanningEnabledCheckbox.isSelected = config.licenseScanningEnabled
        focusModeEnabledCheckbox.isSelected = config.focusModeEnabled
        baseBranchField.text = config.baseBranch
        sarifExportPathField.text = config.sarifExportPath ?: ""
        ignoredPackagesField.text = config.ignoredPackages.joinToString(", ")
        orgManagementEnabledCheckbox.isSelected = config.orgManagementEnabled
        currentOrgField.text = config.currentOrganization ?: ""
        jiraEnabledCheckbox.isSelected = config.jiraEnabled
        jiraBaseUrlField.text = config.jiraBaseUrl ?: ""
        jiraProjectKeyField.text = config.jiraProjectKey ?: ""
        jiraEmailField.text = config.jiraEmail ?: ""
        jiraTokenField.text = config.jiraToken ?: ""
    }

    private fun parseIgnoredPackages(text: String): List<String> =
        text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    override fun disposeUIResources() {
        // Clean up resources if needed
    }
}
