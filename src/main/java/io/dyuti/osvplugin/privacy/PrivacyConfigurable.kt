// Privacy settings UI panel
package io.dyuti.osvplugin.privacy

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import io.dyuti.osvplugin.config.OsVConfig
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Privacy configuration panel accessible via **Settings → Tools → OSV → Privacy**.
 *
 * ## Features
 *
 * - **Enable privacy mode**: Toggle obfuscation of package names in UI, logs, exports
 * - **Rotate salt**: Generate new salt, invalidating all existing hash mappings
 * - **Clear mappings**: Clear in-memory hash → name cache (restarts offset counting)
 *
 * ## What Changes When Enabled
 *
 * | Location | Without Privacy | With Privacy |
 * |----------|----------------|-------------|
 * | Tool window tree | `org.example:lib` | `a3f7b2d8...` |
 * | Exported SARIF | Real names | Hashed names |
 * | IDE logs | Real names | Hashed names |
 * | OSV API calls | Real names | Real names (unchanged) |
 */
class PrivacyConfigurable : Configurable {
    private val config = service<OsVConfig>()
    private var privacyCheckBox: JCheckBox? = null
    private var rotateButton: JButton? = null
    private var clearButton: JButton? = null
    private var statusLabel: JLabel? = null

    override fun getDisplayName(): String = "Privacy"

    override fun createComponent(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // Privacy mode toggle
        privacyCheckBox = JCheckBox("Enable privacy-preserving mode for package names")
        privacyCheckBox?.apply {
            toolTipText = "Obfuscates package names in UI, exports, and logs using SHA-256 hashing"
        }

        // Status / explanation text
        val explanation =
            JLabel(
                """
                <html>
                When enabled, package names are replaced with deterministic SHA-256 hashes in:<br>
                • Vulnerability tool window tree<br>
                • Exported SARIF reports<br>
                • IDE log files<br><br>
                <b>Note:</b> The OSV API still receives actual package names — this cannot be avoided.
                </html>
                """.trimIndent(),
                SwingConstants.LEFT,
            )

        // Rotate salt button
        rotateButton = JButton("Rotate Privacy Salt")
        rotateButton?.apply {
            toolTipText = "Generate a new salt — invalidates all existing hash mappings"
            addActionListener {
                PrivacyService.getInstance().rotateSalt()
                updateStatusLabel()
            }
        }

        // Clear cache button
        clearButton = JButton("Clear Name Mappings")
        clearButton?.apply {
            toolTipText = "Clear in-memory hash → name cache"
            addActionListener {
                PrivacyService.getInstance().clearMappings()
                updateStatusLabel()
            }
        }

        statusLabel = JLabel("")
        updateStatusLabel()

        // Assemble layout
        panel.add(privacyCheckBox)
        panel.add(Box.createVerticalStrut(12))
        panel.add(explanation)
        panel.add(Box.createVerticalStrut(16))

        val buttonPanel = JPanel()
        buttonPanel.add(rotateButton)
        buttonPanel.add(clearButton)
        panel.add(buttonPanel)
        panel.add(Box.createVerticalStrut(8))
        panel.add(statusLabel)
        panel.add(Box.createVerticalGlue())

        return panel
    }

    override fun isModified(): Boolean = privacyCheckBox?.isSelected != config.privacyPreservingEnabled

    override fun apply() {
        val wasEnabled = config.privacyPreservingEnabled
        config.privacyPreservingEnabled = privacyCheckBox?.isSelected ?: false

        if (config.privacyPreservingEnabled && !wasEnabled) {
            // Just enabled — ensure salt exists
            PrivacyService.getInstance().generateSalt()
        }
    }

    override fun reset() {
        privacyCheckBox?.isSelected = config.privacyPreservingEnabled
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

        statusLabel?.text =
            when {
                config.privacyPreservingEnabled && saltPresent -> {
                    "✅ Privacy mode active — $mappingsCount name(s) mapped"
                }

                config.privacyPreservingEnabled && !saltPresent -> {
                    "⚠ Privacy mode enabled but no salt generated"
                }

                else -> {
                    "Privacy mode disabled"
                }
            }
    }
}
