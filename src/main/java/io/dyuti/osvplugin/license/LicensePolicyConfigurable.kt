// SPDX License Scanner Integration — Settings UI (Checkbox Catalog)
package io.dyuti.osvplugin.license

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * License policy settings with an **elaborately enumerated** multi-select
 * checkbox catalog.  Every category is a titled panel with:
 *   • Two-column checkbox grid
 *   • Inline risk notes as tool-tips
 *   • “Select All / Clear” quick actions per category
 *   • Category-level descriptions for legal context
 *
 * State is stored as a plain `List<String>` of SPDX ids (backward-compatible
 * with existing [LicensePolicyConfig]).
 */
class LicensePolicyConfigurable : Configurable {
    private val policy = ApplicationManager.getApplication().getService(LicensePolicyConfig::class.java)

    /** Maps SPDX id → checkbox widget (mutable during UI lifetime). */
    private val checkBoxes = mutableMapOf<String, JBCheckBox>()

    private lateinit var strictCheck: JCheckBox
    private lateinit var contentPanel: JPanel

    override fun getDisplayName(): String = "License Policy"

    override fun createComponent(): JComponent {
        val root = JPanel(BorderLayout())
        root.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        contentPanel = JPanel(GridBagLayout())
        val gbc =
            GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = JBUI.insetsBottom(10)
                gridx = 0
            }
        var row = 0

        // ── Overview banner ──
        gbc.gridy = row++
        contentPanel.add(
            JLabel(
                "<html><b>License Approval Catalogue</b><br>" +
                    "<small>Checked licenses are treated as <b>approved</b>.  All others require legal review.</small></html>",
            ),
            gbc,
        )

        // ── One panel per category ──
        LicenseCatalog.Category.values().forEach { cat ->
            gbc.gridy = row++
            contentPanel.add(buildCategoryPanel(cat), gbc)
        }

        // ── Enforcement ──
        strictCheck =
            JCheckBox(
                "Enable strict mode (only checked licenses are compliant — everything else is flagged)",
            ).apply {
                toolTipText = "When enabled, any license NOT selected below triggers a non-compliance warning"
            }

        gbc.gridy = row++
        contentPanel.add(
            sectionPanel(
                "Enforcement",
                JPanel(BorderLayout()).apply { add(strictCheck, BorderLayout.WEST) },
            ),
            gbc,
        )

        // ── Glue ──
        gbc.gridy = row
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        contentPanel.add(JPanel(), gbc)

        root.add(contentPanel, BorderLayout.NORTH)
        return JBScrollPane(root).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
    }

    /** Builds one titled category panel with header + two-column checkbox grid. */
    private fun buildCategoryPanel(category: LicenseCatalog.Category): JPanel {
        val licenses = LicenseCatalog.byCategory[category] ?: emptyList()
        val catPanel = JPanel(BorderLayout(0, 6))
        catPanel.border =
            BorderFactory.createCompoundBorder(
                TitledBorder(category.displayName),
                BorderFactory.createEmptyBorder(4, 8, 8, 8),
            )

        // Header row: description + Select All / Clear links
        val header = JPanel(BorderLayout())
        header.add(
            JLabel("<html><small>${category.description}</small></html>").apply {
                border = BorderFactory.createEmptyBorder(0, 0, 4, 0)
            },
            BorderLayout.CENTER,
        )
        val linkBox =
            JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(makeLinkLabel("Select All") { setCategory(category, true) })
                add(JLabel("  ·  "))
                add(makeLinkLabel("Clear") { setCategory(category, false) })
            }
        header.add(linkBox, BorderLayout.EAST)
        catPanel.add(header, BorderLayout.NORTH)

        // Checkbox grid: 2 columns to stay compact
        val grid = JPanel(GridLayout(0, 2, 12, 2))
        licenses.forEach { entry ->
            val cb =
                JBCheckBox(wrapLabel(entry)).apply {
                    toolTipText = "<html><b>${entry.spdxId}</b><br>${entry.riskNote}</html>"
                    mnemonic = 0 // disable accidental alt-key interpretation
                }
            checkBoxes[entry.spdxId] = cb
            grid.add(cb)
        }
        // If odd number of licenses, fill last cell with horizontal glue so grid stays aligned
        if (licenses.size % 2 != 0) {
            grid.add(Box.createHorizontalGlue())
        }
        catPanel.add(grid, BorderLayout.CENTER)
        return catPanel
    }

    /** Wraps a long display name so the checkbox label doesn't explode layout. */
    private fun wrapLabel(entry: LicenseCatalog.LicenseEntry): String {
        val short = entry.displayName
        // Truncate aggressively only if absurdly long (none in catalog are)
        return if (short.length > 45) short.take(42) + "…" else short
    }

    /** Creates a blue hyperlink-style label that executes [action] on click. */
    private fun makeLinkLabel(
        text: String,
        action: () -> Unit,
    ): JLabel =
        JLabel("<html><a href='#'>$text</a></html>").apply {
            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        action()
                    }
                },
            )
        }

    /** Checks or unchecks every license within a single category. */
    private fun setCategory(
        category: LicenseCatalog.Category,
        checked: Boolean,
    ) {
        val ids = LicenseCatalog.byCategory[category]?.map { it.spdxId } ?: return
        ids.forEach { checkBoxes[it]?.isSelected = checked }
    }

    /** Wraps [inner] in a panel with a titled border. */
    private fun sectionPanel(
        title: String,
        inner: JComponent,
    ): JPanel {
        val p = JPanel(BorderLayout())
        p.border =
            BorderFactory.createCompoundBorder(
                TitledBorder(title),
                BorderFactory.createEmptyBorder(4, 8, 8, 8),
            )
        p.add(inner, BorderLayout.CENTER)
        return p
    }

    // ─────────────── Configurable contract ───────────────

    override fun isModified(): Boolean {
        val currentAllowed = selectedIds()
        return policy.allowedLicenses.toSet() != currentAllowed ||
            policy.strictMode != strictCheck.isSelected
    }

    override fun apply() {
        policy.allowedLicenses = selectedIds().toList().sorted()
        policy.strictMode = strictCheck.isSelected
    }

    override fun reset() {
        val allowedSet =
            policy.allowedLicenses
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        checkBoxes.forEach { (id, cb) ->
            cb.isSelected = id in allowedSet
        }
        strictCheck.isSelected = policy.strictMode
    }

    override fun disposeUIResources() {
        checkBoxes.clear()
    }

    /** Collects all currently-checked SPDX ids from the checkbox grid. */
    private fun selectedIds(): Set<String> =
        checkBoxes
            .filter { (_, cb) -> cb.isSelected }
            .map { (id, _) -> id }
            .toSet()
}
