// Export Action for OSV Tool Window Toolbar
package io.dyuti.osvplugin.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.dyuti.osvplugin.toolwindow.OsVToolWindowPanel

class ExportAction(
    private val panel: OsVToolWindowPanel,
) : AnAction("Export", "Export to SARIF", AllIcons.ToolbarDecorator.Export) {
    override fun actionPerformed(e: AnActionEvent) {
        panel.exportResults()
    }
}
