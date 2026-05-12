package io.dyuti.osvplugin.utils

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import io.dyuti.osvplugin.api.model.OsVSeverity
import java.awt.Color
import javax.swing.Icon

object SeverityUtil {
    fun getColor(severity: OsVSeverity): JBColor =
        when (severity) {
            OsVSeverity.CRITICAL -> JBColor(Color(220, 53, 69), Color(255, 100, 100))
            OsVSeverity.HIGH -> JBColor(Color(255, 193, 7), Color(255, 220, 80))
            OsVSeverity.MEDIUM -> JBColor(Color(255, 165, 0), Color(255, 180, 40))
            OsVSeverity.LOW -> JBColor(Color(108, 117, 125), Color(150, 160, 170))
        }

    fun getPriority(severity: OsVSeverity): Int =
        when (severity) {
            OsVSeverity.CRITICAL -> 1
            OsVSeverity.HIGH -> 2
            OsVSeverity.MEDIUM -> 3
            OsVSeverity.LOW -> 4
        }

    fun getSeverityIcon(severity: OsVSeverity): Icon =
        when (severity) {
            OsVSeverity.CRITICAL -> AllIcons.Ide.FatalError
            OsVSeverity.HIGH -> AllIcons.General.Error
            OsVSeverity.MEDIUM -> AllIcons.General.Warning
            OsVSeverity.LOW -> AllIcons.General.Information
        }

    fun meetsThreshold(
        severity: OsVSeverity,
        threshold: OsVSeverity,
    ): Boolean = getPriority(severity) <= getPriority(threshold)

    fun getSeverityDescription(severity: OsVSeverity): String =
        when (severity) {
            OsVSeverity.CRITICAL -> "Critical: Vulnerability requires immediate remediation. Risk of complete system compromise."
            OsVSeverity.HIGH -> "High: Vulnerability should be addressed promptly. Significant security risk present."
            OsVSeverity.MEDIUM -> "Medium: Vulnerability should be scheduled for remediation. Moderate security impact."
            OsVSeverity.LOW -> "Low: Vulnerability can be addressed in routine maintenance. Minimal security impact."
        }
}
