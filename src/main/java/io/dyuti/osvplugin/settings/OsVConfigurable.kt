// OSV Vulnerability Scanner Settings Page
package io.dyuti.osvplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.config.OsVConfig
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.border.TitledBorder

/**
 * Settings configurable for OSV Vulnerability Scanner.
 *
 * Uses [GridBagLayout] with [TitledBorder] sections for professional
 * JetBrains-style settings layout aligned to platform standards.
 */
class OsVConfigurable : Configurable {
    private var config: OsVConfig = OsVConfig()

    // ─── Scan Behaviour ───
    private lateinit var minSeverityCombo: JComboBox<OsVSeverity>
    private lateinit var inspectionEnabledCheck: JCheckBox
    private lateinit var cacheTtlSpinner: JSpinner
    private lateinit var scanDirectCheck: JCheckBox
    private lateinit var scanTransitiveCheck: JCheckBox

    // ─── API & Integrations ───
    private lateinit var rateLimitCheck: JCheckBox
    private lateinit var rateLimitSpinner: JSpinner
    private lateinit var githubAdvisoryCheck: JCheckBox
    private lateinit var githubTokenField: JPasswordField
    private lateinit var licenseScanningCheck: JCheckBox
    private lateinit var focusModeCheck: JCheckBox
    private lateinit var baseBranchField: JTextField
    private lateinit var sarifPathField: JTextField
    private lateinit var osvApiUrlField: JTextField

    // ─── Organisation Management ───
    private lateinit var orgEnabledCheck: JCheckBox
    private lateinit var orgNameField: JTextField

    // ─── Jira Integration ───
    private lateinit var jiraEnabledCheck: JCheckBox
    private lateinit var jiraUrlField: JTextField
    private lateinit var jiraProjectField: JTextField
    private lateinit var jiraEmailField: JTextField
    private lateinit var jiraTokenField: JPasswordField

    // ─── Ignored Packages ───
    private lateinit var ignoredPackagesField: JTextField

    override fun getDisplayName(): String = "OSV Scanner"

    override fun createComponent(): JComponent {
        val root = JPanel(BorderLayout())
        root.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        val content = JPanel(GridBagLayout())
        val gbc =
            GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = Insets(0, 0, 10, 0)
                gridx = 0
            }
        var row = 0

        // ── Scan Behaviour ──
        minSeverityCombo = JComboBox(OsVSeverity.values())
        inspectionEnabledCheck = JCheckBox("Enable real-time vulnerability inspection in editor")
        cacheTtlSpinner = JSpinner(SpinnerNumberModel(24, 1, 168, 1))
        scanDirectCheck = JCheckBox("Scan direct dependencies")
        scanTransitiveCheck = JCheckBox("Scan transitive dependencies")

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Scan Behaviour",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.row(r++, JLabel("Minimum severity:"), minSeverityCombo)
                    p.spanRow(r++, inspectionEnabledCheck)
                    p.row(r++, JLabel("Cache TTL (hours):"), cacheTtlSpinner)
                    p.spanRow(r++, scanDirectCheck)
                    p.spanRow(r++, scanTransitiveCheck)
                    p
                },
            ),
            gbc,
        )

        // ── API & Integrations ──
        rateLimitCheck = JCheckBox("Enable API rate limiting")
        rateLimitSpinner = JSpinner(SpinnerNumberModel(1000, 1, 10000, 100))
        githubAdvisoryCheck = JCheckBox("Enable GitHub Advisory integration")
        githubTokenField =
            JBPasswordField().apply {
                columns = 25
                toolTipText = "GitHub personal access token"
            }
        licenseScanningCheck = JCheckBox("Enable license scanning")
        focusModeCheck = JCheckBox("Focus mode (show only critical / high)")
        baseBranchField = JTextField("main", 15)
        sarifPathField = JTextField(25).apply { toolTipText = "Directory path for SARIF export" }
        osvApiUrlField = JTextField("https://api.osv.dev/v1/query", 30)

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "API & Integrations",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.spanRow(r++, rateLimitCheck)
                    p.row(r++, JLabel("Requests per hour:"), rateLimitSpinner)
                    p.spanRow(r++, githubAdvisoryCheck)
                    p.row(r++, JLabel("GitHub token:"), githubTokenField)
                    p.spanRow(r++, licenseScanningCheck)
                    p.spanRow(r++, focusModeCheck)
                    p.row(r++, JLabel("Base branch:"), baseBranchField)
                    p.row(r++, JLabel("SARIF export path:"), sarifPathField)
                    p.row(r++, JLabel("OSV API URL:"), osvApiUrlField)
                    p
                },
            ),
            gbc,
        )

        // ── Organisation Management ──
        orgEnabledCheck = JCheckBox("Enable organisation management")
        orgNameField = JTextField(20).apply { toolTipText = "Organisation name" }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Organisation Management",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.spanRow(r++, orgEnabledCheck)
                    p.row(r++, JLabel("Organisation:"), orgNameField)
                    p
                },
            ),
            gbc,
        )

        // ── Jira Integration ──
        jiraEnabledCheck = JCheckBox("Enable Jira integration")
        jiraUrlField = JTextField(25).apply { toolTipText = "https://jira.company.com" }
        jiraProjectField = JTextField(10).apply { toolTipText = "Project key, e.g. PROJ" }
        jiraEmailField = JTextField(20).apply { toolTipText = "user@company.com" }
        jiraTokenField =
            JBPasswordField().apply {
                columns = 25
                toolTipText = "Jira API token"
            }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Jira Integration",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.spanRow(r++, jiraEnabledCheck)
                    p.row(r++, JLabel("Base URL:"), jiraUrlField)
                    p.row(r++, JLabel("Project key:"), jiraProjectField)
                    p.row(r++, JLabel("Email:"), jiraEmailField)
                    p.row(r++, JLabel("API token:"), jiraTokenField)
                    p
                },
            ),
            gbc,
        )

        // ── Ignored Packages ──
        ignoredPackagesField =
            JTextField(30).apply {
                toolTipText = "Comma-separated list of package names to ignore"
            }
        val ignoredNote =
            JLabel(
                "<html><small>" +
                    "Separate package names with commas (e.g. org.example:lib, com.test:app)" +
                    "</small></html>",
            )

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Ignored Packages",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.row(r++, JLabel("Packages to ignore:"), ignoredPackagesField)
                    p.spanRow(r++, ignoredNote)
                    p
                },
            ),
            gbc,
        )

        // Glue at bottom
        gbc.gridy = row
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        content.add(JPanel(), gbc)

        root.add(content, BorderLayout.NORTH)
        return com.intellij.ui.components.JBScrollPane(root).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
    }

    /** Adds a row with [label]–[control] pair to [this] panel at the given row index. */
    private fun JPanel.row(
        row: Int,
        label: JLabel,
        control: JComponent,
    ) {
        val c = GridBagConstraints()
        c.gridy = row
        c.insets = Insets(4, 0, 4, 8)
        // Label
        c.gridx = 0
        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.WEST
        add(label, c)
        // Control
        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        c.anchor = GridBagConstraints.WEST
        add(control, c)
    }

    /** Adds a component that spans both columns (e.g. a checkbox). */
    private fun JPanel.spanRow(
        row: Int,
        control: JComponent,
    ) {
        val c = GridBagConstraints()
        c.gridy = row
        c.gridx = 0
        c.gridwidth = 2
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(4, 0, 4, 0)
        c.anchor = GridBagConstraints.WEST
        add(control, c)
    }

    private fun sectionPanel(
        title: String,
        panel: JPanel,
    ): JPanel {
        panel.border =
            BorderFactory.createCompoundBorder(
                TitledBorder(title),
                BorderFactory.createEmptyBorder(4, 8, 8, 8),
            )
        return panel
    }

    override fun isModified(): Boolean {
        val currentGithubToken = OsVConfig.getGithubToken() ?: ""
        val fieldGithubToken = String(githubTokenField.password)
        val currentJiraToken = OsVConfig.getJiraToken() ?: ""
        val fieldJiraToken = String(jiraTokenField.password)

        return config.minimumSeverity != minSeverityCombo.selectedItem ||
            config.inspectionEnabled != inspectionEnabledCheck.isSelected ||
            config.cacheTtl != cacheTtlSpinner.value ||
            config.rateLimitEnabled != rateLimitCheck.isSelected ||
            config.rateLimitRequestsPerHour != rateLimitSpinner.value ||
            config.scanDirectDependencies != scanDirectCheck.isSelected ||
            config.scanTransitiveDependencies != scanTransitiveCheck.isSelected ||
            config.githubAdvisoryEnabled != githubAdvisoryCheck.isSelected ||
            currentGithubToken != fieldGithubToken ||
            config.licenseScanningEnabled != licenseScanningCheck.isSelected ||
            config.focusModeEnabled != focusModeCheck.isSelected ||
            config.baseBranch != baseBranchField.text ||
            config.sarifExportPath != sarifPathField.text.takeIf { it.isNotEmpty() } ||
            config.osvApiUrl != osvApiUrlField.text ||
            config.ignoredPackages != parseIgnoredPackages(ignoredPackagesField.text) ||
            config.orgManagementEnabled != orgEnabledCheck.isSelected ||
            config.currentOrganization != orgNameField.text.takeIf { it.isNotEmpty() } ||
            config.jiraEnabled != jiraEnabledCheck.isSelected ||
            config.jiraBaseUrl != jiraUrlField.text.takeIf { it.isNotEmpty() } ||
            config.jiraProjectKey != jiraProjectField.text.takeIf { it.isNotEmpty() } ||
            config.jiraEmail != jiraEmailField.text.takeIf { it.isNotEmpty() } ||
            currentJiraToken != fieldJiraToken
    }

    override fun apply() {
        config.minimumSeverity = minSeverityCombo.selectedItem as OsVSeverity
        config.inspectionEnabled = inspectionEnabledCheck.isSelected
        config.cacheTtl = cacheTtlSpinner.value as Int
        config.rateLimitEnabled = rateLimitCheck.isSelected
        config.rateLimitRequestsPerHour = rateLimitSpinner.value as Int
        config.scanDirectDependencies = scanDirectCheck.isSelected
        config.scanTransitiveDependencies = scanTransitiveCheck.isSelected
        config.githubAdvisoryEnabled = githubAdvisoryCheck.isSelected
        OsVConfig.setGithubToken(String(githubTokenField.password).takeIf { it.isNotEmpty() })
        config.licenseScanningEnabled = licenseScanningCheck.isSelected
        config.focusModeEnabled = focusModeCheck.isSelected
        config.baseBranch = baseBranchField.text
        config.sarifExportPath = sarifPathField.text.takeIf { it.isNotEmpty() }
        config.osvApiUrl = osvApiUrlField.text
        config.ignoredPackages = parseIgnoredPackages(ignoredPackagesField.text)
        config.orgManagementEnabled = orgEnabledCheck.isSelected
        config.currentOrganization = orgNameField.text.takeIf { it.isNotEmpty() }
        config.jiraEnabled = jiraEnabledCheck.isSelected
        config.jiraBaseUrl = jiraUrlField.text.takeIf { it.isNotEmpty() }
        config.jiraProjectKey = jiraProjectField.text.takeIf { it.isNotEmpty() }
        config.jiraEmail = jiraEmailField.text.takeIf { it.isNotEmpty() }
        OsVConfig.setJiraToken(String(jiraTokenField.password).takeIf { it.isNotEmpty() })

        ApplicationManager
            .getApplication()
            .getService(OsVConfig::class.java)
            .loadState(config)
    }

    @Suppress("DuplicatedCode")
    override fun reset() {
        val saved =
            ApplicationManager
                .getApplication()
                .getService(OsVConfig::class.java)
        config = saved

        minSeverityCombo.selectedItem = config.minimumSeverity
        inspectionEnabledCheck.isSelected = config.inspectionEnabled
        cacheTtlSpinner.value = config.cacheTtl
        scanDirectCheck.isSelected = config.scanDirectDependencies
        scanTransitiveCheck.isSelected = config.scanTransitiveDependencies
        rateLimitCheck.isSelected = config.rateLimitEnabled
        rateLimitSpinner.value = config.rateLimitRequestsPerHour
        githubAdvisoryCheck.isSelected = config.githubAdvisoryEnabled
        githubTokenField.setText(OsVConfig.getGithubToken() ?: "")
        licenseScanningCheck.isSelected = config.licenseScanningEnabled
        focusModeCheck.isSelected = config.focusModeEnabled
        baseBranchField.text = config.baseBranch
        sarifPathField.text = config.sarifExportPath ?: ""
        osvApiUrlField.text = config.osvApiUrl
        ignoredPackagesField.text = config.ignoredPackages.joinToString(", ")
        orgEnabledCheck.isSelected = config.orgManagementEnabled
        orgNameField.text = config.currentOrganization ?: ""
        jiraEnabledCheck.isSelected = config.jiraEnabled
        jiraUrlField.text = config.jiraBaseUrl ?: ""
        jiraProjectField.text = config.jiraProjectKey ?: ""
        jiraEmailField.text = config.jiraEmail ?: ""
        jiraTokenField.setText(OsVConfig.getJiraToken() ?: "")
    }

    private fun parseIgnoredPackages(text: String): List<String> =
        text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    override fun disposeUIResources() {
        // No external resources to clean up
    }
}
