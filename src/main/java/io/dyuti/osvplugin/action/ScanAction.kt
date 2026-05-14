// Scan Action for OSV Tool Window Toolbar
package io.dyuti.osvplugin.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.dyuti.osvplugin.toolwindow.OsVToolWindowPanel

class ScanAction(
    private val panel: OsVToolWindowPanel,
) : AnAction("Scan", "Scan for vulnerabilities", AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        panel.scanDependencies()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
