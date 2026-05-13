// Privacy settings UI panel
package io.dyuti.osvplugin.privacy

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBScrollPane
import io.dyuti.osvplugin.config.OsVConfig
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.TitledBorder

/**
 * Privacy configuration panel accessible via **Settings → Tools → OSV → Privacy**.
 *
 * Uses [GridBagLayout] with [TitledBorder] sections for a professional
 * JetBrains-style settings layout.
 */
class PrivacyConfigurable : Configurable {
    private val config = service<OsVConfig>()

    private lateinit var privacyCheckBox: JCheckBox
    private lateinit var statusLabel: JLabel

    override fun getDisplayName(): String = "Privacy"

    override fun createComponent(): JComponent {
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

        // ── Privacy Mode ──
        privacyCheckBox =
            JCheckBox(
                "Enable privacy-preserving mode for package names",
            ).apply {
                toolTipText =
                    "Obfuscates package names in UI, exports, and logs using SHA-256 hashing"
            }

        statusLabel = JLabel()
        updateStatusLabel()

        val actions =
            JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(
                    JButton("Rotate salt").apply {
                        toolTipText = "Generate a new salt — invalidates all existing hash mappings"
                        addActionListener {
                            PrivacyService.getInstance().rotateSalt()
                            updateStatusLabel()
                        }
                    },
                )
                add(
                    JButton("Clear name mappings").apply {
                        toolTipText = "Clear in-memory hash → name cache"
                        addActionListener {
                            PrivacyService.getInstance().clearMappings()
                            updateStatusLabel()
                        }
                    },
                )
            }

        val explanation =
            JLabel(
                "<html><small>" +
                    "When enabled, package names are replaced with deterministic SHA-256 hashes.<br>" +
                    "The OSV API still receives actual package names (this cannot be avoided)." +
                    "</small></html>",
            )

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Privacy Mode",
                run {
                    val p = JPanel(GridBagLayout())
                    var r = 0
                    p.spanRow(r++, privacyCheckBox)
                    p.spanRow(r++, explanation)
                    p.spanRow(r++, actions)
                    p.row(r++, JLabel("Status:", JLabel.TRAILING), statusLabel)
                    p
                },
            ),
            gbc,
        )

        // ── Data Providers ──
        val providers =
            JLabel(
                "<html>OSV vulnerability data is provided by <b>OSV.dev</b> (Google).<br>" +
                    "Optional GitHub Advisory data requires a valid GitHub token.</html>",
            )

        gbc.gridy = row++
        content.add(
            sectionPanel(
                "Data Providers",
                run {
                    val p = JPanel(GridBagLayout())
                    p.spanRow(0, providers)
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

    override fun isModified(): Boolean = privacyCheckBox.isSelected != config.privacyPreservingEnabled

    override fun apply() {
        val wasEnabled = config.privacyPreservingEnabled
        config.privacyPreservingEnabled = privacyCheckBox.isSelected

        if (config.privacyPreservingEnabled && !wasEnabled) {
            PrivacyService.getInstance().generateSalt()
        }
        updateStatusLabel()
    }

    override fun reset() {
        privacyCheckBox.isSelected = config.privacyPreservingEnabled
        updateStatusLabel()
    }

    private fun updateStatusLabel() {
        val saltPresent = OsVConfig.getPrivacySalt() != null
        val mappingsCount =
            try {
                PrivacyService.getInstance().let {
                    it.javaClass
                        .getDeclaredField("nameMap")
                        .apply { isAccessible = true }
                        .get(it)
                        .let { m -> (m as? java.util.concurrent.ConcurrentHashMap<*, *>)?.size ?: 0 }
                }
            } catch (_: Exception) {
                0
            }

        statusLabel.text =
            when {
                config.privacyPreservingEnabled && saltPresent -> {
                    "✅ Active — $mappingsCount name(s) mapped"
                }

                config.privacyPreservingEnabled && !saltPresent -> {
                    "⚠ Enabled but no salt generated"
                }

                else -> {
                    "Disabled"
                }
            }
    }
}
