// OSV Vulnerability Scanner Tool Window Factory
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import io.dyuti.osvplugin.OsVPlugin

/**
 * Factory for creating the OSV Tool Window
 */
class OsVToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()
        
        // Create the tool window content
        val content = contentFactory.createContent(
            OsVToolWindowPanel(project),
            "",
            false
        )
        
        contentManager.addContent(content)
    }
    
    override fun init(toolWindow: ToolWindow) {
        // Initialization logic
    }
    
    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }
}
