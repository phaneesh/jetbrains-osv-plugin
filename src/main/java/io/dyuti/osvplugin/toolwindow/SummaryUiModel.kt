package io.dyuti.osvplugin.toolwindow

import com.intellij.ui.JBColor
import javax.swing.Icon

/**
 * UI model for a summary button.
 *
 * @param icon The icon to display (nullable)
 * @param backgroundColor The background color (nullable, theme-aware)
 * @param borderColor The border color (nullable, theme-aware)
 */
data class SummaryUiModel(
    val icon: Icon? = null,
    val backgroundColor: JBColor? = null,
    val borderColor: JBColor? = null,
)
