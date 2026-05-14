package io.dyuti.osvplugin.toolwindow

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*

open class RoundedPanelWithBackgroundColor(
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    private val cornerAngle: Float = 20f,
) : JPanel() {
    init {
        isOpaque = false
        cursor = Cursor.getDefaultCursor()
        updateBackgroundColor(backgroundColor)
        updateBorderColor(borderColor)
    }

    fun updateBackgroundColor(newBackgroundColor: Color?) {
        background = newBackgroundColor
    }

    fun updateBorderColor(newBorderColor: Color?) {
        newBorderColor?.let {
            val customBorder = IdeBorderFactory.createRoundedBorder(cornerAngle.toInt(), 1)
            customBorder.setColor(it)
            border = customBorder
        }
    }

    override fun paintComponent(g: Graphics) {
        GraphicsUtil.setupRoundedBorderAntialiasing(g)
        val g2 = g as Graphics2D
        val rect = Rectangle(size)
        JBInsets.removeFrom(rect, insets)
        val rectangle2d =
            RoundRectangle2D.Float(
                rect.x.toFloat() + 0.5f,
                rect.y.toFloat() + 0.5f,
                rect.width.toFloat() - 1f,
                rect.height.toFloat() - 1f,
                cornerAngle,
                cornerAngle,
            )
        val fillColor = background ?: UIUtil.getPanelBackground()
        g2.color = fillColor
        g2.fill(rectangle2d)
    }
}

/**
 * A summary button component for displaying finding counts and controlling visibility.
 *
 * Features:
 * - Click to toggle collapse/expand state
 * - Displays count (e.g., "5 Issues", "2 Security Hotspots")
 * - Hover effects (color changes)
 * - Disabled state (50% opacity, grey border)
 * - Theme-aware colors using JBColor
 */
class SummaryButton(
    private val typeNameSingular: String,
    private val typeNamePlural: String,
    private val onSelectionChanged: (Boolean) -> Unit,
    private val tooltipText: String,
) : RoundedPanelWithBackgroundColor() {
    private var count = 0
    private val iconLabel = JLabel()
    private val textLabel = JLabel()
    private var isSelected = false
    private var isEnabled = true
    private var isHovered = false
    private var lastBorderColor: Color? = null
    private var lastBackgroundColor: Color? = null
    private var alpha: Float = 1.0f

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        iconLabel.verticalAlignment = SwingConstants.CENTER
        textLabel.verticalAlignment = SwingConstants.CENTER
        textLabel.text = formatText()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        toolTipText = tooltipText
        font = UIUtil.getLabelFont().deriveFont(Font.BOLD)
        isOpaque = false
        border = null

        add(Box.createRigidArea(Dimension(4, 0)))
        add(iconLabel)
        add(Box.createRigidArea(Dimension(4, 0)))
        add(textLabel)
        add(Box.createRigidArea(Dimension(6, 0)))

        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (!isEnabled) return
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        toggleSelection()
                    }
                }

                override fun mouseEntered(e: MouseEvent) {
                    if (isEnabled) {
                        isHovered = true
                        updateColors(lastBackgroundColor, lastBorderColor)
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    if (isEnabled) {
                        isHovered = false
                        updateColors(lastBackgroundColor, lastBorderColor)
                    }
                }
            },
        )
    }

    fun update(
        count: Int,
        uiModel: SummaryUiModel,
    ) {
        this.count = count
        iconLabel.icon = uiModel.icon
        textLabel.text = formatText()
        lastBackgroundColor = uiModel.backgroundColor
        lastBorderColor = uiModel.borderColor
        updateColors(lastBackgroundColor, lastBorderColor)
    }

    fun setSelected(selected: Boolean) {
        if (isSelected != selected) {
            isSelected = selected
            updateSelection()
            updateColors(lastBackgroundColor, lastBorderColor)
            onSelectionChanged(isSelected)
        }
    }

    fun isSelected(): Boolean = isSelected

    fun toggleSelection() {
        setSelected(!isSelected)
    }

    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        cursor = if (enabled) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
        iconLabel.isEnabled = enabled
        textLabel.isEnabled = enabled
        textLabel.text = formatText()
        alpha = if (enabled) 1.0f else 0.5f
        if (!enabled) {
            lastBorderColor = JBColor.GRAY
            updateColors(null, lastBorderColor)
        }
        repaint()
    }

    private fun formatText(): String =
        when {
            !isEnabled -> typeNamePlural
            count == 0 -> "No $typeNamePlural"
            count == 1 -> "1 $typeNameSingular"
            else -> "$count $typeNamePlural"
        }

    private fun updateColors(
        newBackgroundColor: Color?,
        newBorderColor: Color?,
    ) {
        val bg = when {
            isSelected && isHovered && isEnabled -> UIUtil.getPanelBackground().brighter().brighter()
            isSelected -> UIUtil.getPanelBackground().brighter()
            isHovered && isEnabled -> UIUtil.getPanelBackground().darker()
            else -> newBackgroundColor ?: lastBackgroundColor ?: UIUtil.getPanelBackground()
        }
        updateBackgroundColor(bg)
        updateBorderColor(newBorderColor)
        repaint()
    }

    private fun updateSelection() {
        textLabel.foreground = if (isSelected) UIUtil.getListSelectionForeground(true) else UIUtil.getLabelForeground()
        font = UIUtil.getLabelFont().deriveFont(if (isSelected) Font.BOLD else Font.PLAIN)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        if (alpha < 1.0f) {
            val g2 = g.create() as Graphics2D
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            super.paintComponent(g2)
            g2.dispose()
        } else {
            super.paintComponent(g)
        }
    }
}
