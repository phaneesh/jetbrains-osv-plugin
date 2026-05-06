// SPDX License Scanner Integration - Settings UI
@file:Suppress("DEPRECATION")

package io.dyuti.osvplugin.license

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

/**
 * License policy configurable - Full UI implementation
 */
class LicensePolicyConfigurable : Configurable {
    private val policy = ServiceManager.getService(LicensePolicyConfig::class.java)

    private val allowedLicensesField =
        com.intellij.ui.components
            .JBTextField()
    private val blockedLicensesField =
        com.intellij.ui.components
            .JBTextField()
    private val copyleftAllowListField =
        com.intellij.ui.components
            .JBTextField()
    private val strictModeCheckbox = JBCheckBox("Enable strict mode (only allow explicitly approved licenses)")

    override fun getDisplayName(): String = "License Policy"

    override fun createComponent(): JComponent? {
        // Create main panel with layout
        val panel =
            JBPanel<JBPanel<*>>(java.awt.BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
            }

        // Create form panel
        val formPanel =
            JBPanel<JBPanel<*>>(java.awt.GridLayout(0, 2, 10, 10)).apply {
                border =
                    javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createTitledBorder("License Configuration"),
                        javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10),
                    )
            }

        // Add allowed licenses field
        formPanel.add(JBLabel("Allowed Licenses (comma-separated):"))
        allowedLicensesField.text = policy.allowedLicenses.joinToString(", ")
        formPanel.add(
            JBScrollPane(allowedLicensesField).apply {
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = java.awt.Dimension(300, 60)
            },
        )

        // Add blocked licenses field
        formPanel.add(JBLabel("Blocked Licenses (comma-separated):"))
        blockedLicensesField.text = policy.blockedLicenses.joinToString(", ")
        formPanel.add(
            JBScrollPane(blockedLicensesField).apply {
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = java.awt.Dimension(300, 60)
            },
        )

        // Add copyleft allow list field
        formPanel.add(JBLabel("Copyleft Allow List (comma-separated):"))
        copyleftAllowListField.text = policy.copyleftAllowList.joinToString(", ")
        formPanel.add(
            JBScrollPane(copyleftAllowListField).apply {
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = java.awt.Dimension(300, 60)
            },
        )

        // Add strict mode checkbox
        formPanel.add(JBLabel(" "))
        strictModeCheckbox.isSelected = true // Default to strict mode
        formPanel.add(strictModeCheckbox)

        // Add info panel
        val infoPanel =
            JBPanel<JBPanel<*>>(java.awt.BorderLayout()).apply {
                border =
                    javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createTitledBorder("Information"),
                        javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10),
                    )
            }

        val infoLabel =
            JBLabel(
                "<html>" +
                    "<p>Configure allowed and blocked licenses for your project.</p>" +
                    "<p><b>Allowed Licenses:</b> Only these licenses will be considered compliant (if list is non-empty).</p>" +
                    "<p><b>Blocked Licenses:</b> Any of these licenses will be considered non-compliant.</p>" +
                    "<p><b>Copyleft Allow List:</b> Specific copyleft licenses that are explicitly allowed.</p>" +
                    "<p><b>Strict Mode:</b> When enabled, only explicitly allowed licenses are considered compliant.</p>" +
                    "</html>",
            ).apply {
                foreground = JBColor.BLUE
            }
        infoPanel.add(infoLabel, java.awt.BorderLayout.CENTER)

        // Add panels to main panel
        panel.add(formPanel, java.awt.BorderLayout.CENTER)
        panel.add(infoPanel, java.awt.BorderLayout.SOUTH)

        return panel
    }

    override fun isModified(): Boolean {
        val currentAllowed =
            allowedLicensesField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        val currentBlocked =
            blockedLicensesField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        val currentCopyleft =
            copyleftAllowListField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        return policy.allowedLicenses != currentAllowed ||
            policy.blockedLicenses != currentBlocked ||
            policy.copyleftAllowList != currentCopyleft ||
            policy.strictMode != strictModeCheckbox.isSelected
    }

    override fun apply() {
        policy.allowedLicenses =
            allowedLicensesField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        policy.blockedLicenses =
            blockedLicensesField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        policy.copyleftAllowList =
            copyleftAllowListField.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        policy.strictMode = strictModeCheckbox.isSelected
    }

    override fun reset() {
        allowedLicensesField.text = policy.allowedLicenses.joinToString(", ")
        blockedLicensesField.text = policy.blockedLicenses.joinToString(", ")
        copyleftAllowListField.text = policy.copyleftAllowList.joinToString(", ")
        strictModeCheckbox.isSelected = policy.strictMode
    }

    override fun disposeUIResources() {
        // Cleanup resources
    }
}
