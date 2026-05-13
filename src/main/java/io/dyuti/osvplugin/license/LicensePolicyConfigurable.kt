// SPDX License Scanner Integration - Settings UI
package io.dyuti.osvplugin.license

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.TitledBorder

/**
 * License policy configurable with JetBrains-standard layout
 * using [GridBagLayout] and [TitledBorder] sections.
 */
class LicensePolicyConfigurable : Configurable {
    private val policy = ApplicationManager.getApplication().getService(LicensePolicyConfig::class.java)

    private lateinit var allowedField: JBTextArea
    private lateinit var blockedField: JBTextArea
    private lateinit var copyleftField: JBTextArea
    private lateinit var strictCheck: JCheckBox

    override fun getDisplayName(): String = "License Policy"

    override fun createComponent(): JComponent? {
        val root = JPanel(BorderLayout())
        root.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        val content = JPanel(GridBagLayout())
        val gbc =
            GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = Insets(0, 0, 12, 0)
                gridx = 0
            }
        var row = 0

        // ── Allowed Licenses ──
        allowedField =
            JBTextArea(3, 30).apply {
                lineWrap = true
                wrapStyleWord = true
                toolTipText = "MIT, BSD-3-Clause, Apache-2.0, ..."
            }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Allowed Licenses",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.row(r++, JLabel("Comma-separated list:", JLabel.TRAILING), wrap(allowedField))
                    p.spanRow(
                        r++,
                        JLabel(
                            "<html><small>When strict mode is enabled, only these licenses are considered compliant.</small></html>",
                        ),
                    )
                    p
                },
            ),
            gbc,
        )

        // ── Blocked Licenses ──
        blockedField =
            JBTextArea(2, 30).apply {
                lineWrap = true
                wrapStyleWord = true
                toolTipText = "GPL-2.0-only, Proprietary, ..."
            }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Blocked Licenses",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.row(r++, JLabel("Comma-separated list:", JLabel.TRAILING), wrap(blockedField))
                    p.spanRow(
                        r++,
                        JLabel(
                            "<html><small>Dependencies with any of these licenses will be flagged as non-compliant.</small></html>",
                        ),
                    )
                    p
                },
            ),
            gbc,
        )

        // ── Copyleft Allow List ──
        copyleftField =
            JBTextArea(2, 30).apply {
                lineWrap = true
                wrapStyleWord = true
                toolTipText = "GPL-3.0-or-later, MPL-2.0, ..."
            }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Copyleft Allow List",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.row(r++, JLabel("Comma-separated list:", JLabel.TRAILING), wrap(copyleftField))
                    p.spanRow(
                        r++,
                        JLabel(
                            "<html><small>Specific copyleft licenses that are <b>explicitly</b> permitted for this project.</small></html>",
                        ),
                    )
                    p
                },
            ),
            gbc,
        )

        // ── Enforcement ──
        strictCheck =
            JCheckBox("Enable strict mode (only allow explicitly approved licenses)").apply {
                toolTipText = "When enabled, any license not in the allowed list is treated as non-compliant"
            }

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Enforcement",
                run {
                    val p = JPanel(GridBagLayout())
                    p.spanRow(0, strictCheck)
                    p
                },
            ),
            gbc,
        )

        // Glue
        gbc.gridy = row
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        content.add(JPanel(), gbc)

        root.add(content, BorderLayout.NORTH)
        return JBScrollPane(root).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
    }

    /** Wraps a text area in a lightweight scroll pane. */
    private fun wrap(area: JBTextArea): JComponent =
        JBScrollPane(area).apply {
            preferredSize = java.awt.Dimension(300, area.preferredSize.height.coerceIn(40, 120))
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
        c.gridx = 0
        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.EAST
        add(label, c)
        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        c.anchor = GridBagConstraints.WEST
        add(control, c)
    }

    /** Adds a component that spans both columns (e.g. a checkbox or note). */
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
        val currentAllowed = parseList(allowedField.text)
        val currentBlocked = parseList(blockedField.text)
        val currentCopyleft = parseList(copyleftField.text)

        return policy.allowedLicenses != currentAllowed ||
            policy.blockedLicenses != currentBlocked ||
            policy.copyleftAllowList != currentCopyleft ||
            policy.strictMode != strictCheck.isSelected
    }

    override fun apply() {
        policy.allowedLicenses = parseList(allowedField.text)
        policy.blockedLicenses = parseList(blockedField.text)
        policy.copyleftAllowList = parseList(copyleftField.text)
        policy.strictMode = strictCheck.isSelected
    }

    override fun reset() {
        allowedField.text = policy.allowedLicenses.joinToString(", ")
        blockedField.text = policy.blockedLicenses.joinToString(", ")
        copyleftField.text = policy.copyleftAllowList.joinToString(", ")
        strictCheck.isSelected = policy.strictMode
    }

    override fun disposeUIResources() {
        // No external resources
    }

    private fun parseList(text: String): List<String> =
        text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
