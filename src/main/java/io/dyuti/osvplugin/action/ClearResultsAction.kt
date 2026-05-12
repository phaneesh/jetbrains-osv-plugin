// Clear Results Action for OSV Tool Window Toolbar
package io.dyuti.osvplugin.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.dyuti.osvplugin.toolwindow.OsVToolWindowPanel

class ClearResultsAction(
    private val panel: OsVToolWindowPanel,
) : AnAction("Clear", "Clear scan results", AllIcons.Actions.GC) {
    override fun actionPerformed(e: AnActionEvent) {
        panel.clearResults()
    }
}
