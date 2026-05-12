// Status Bar Widget for OSV Scanner
package io.dyuti.osvplugin.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

class OsvStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "osv.statusBar"

    override fun getDisplayName(): String = "OSV Scanner"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = OsvStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {}

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class OsvStatusBarWidget(
    private val project: Project,
) : StatusBarWidget {
    @Volatile private var text: String = "OSV: Ready"

    override fun ID(): String = "osv.statusBar.widget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation =
        object : StatusBarWidget.TextPresentation {
            override fun getText(): String = text

            override fun getTooltipText(): String = "OSV Vulnerability Scanner status"

            override fun getClickConsumer(): Consumer<MouseEvent>? = null

            override fun getAlignment(): Float = Component.LEFT_ALIGNMENT
        }

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    fun updateStatus(newText: String) {
        text = newText
    }
}
