package io.dyuti.osvplugin.utils

import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * Utility functions for severity handling
 */
object SeverityUtil {
    
    /**
     * Get color for severity level
     */
    fun getColor(severity: OsVSeverity): java.awt.Color {
        return when (severity) {
            OsVSeverity.CRITICAL -> java.awt.Color(220, 53, 69)
            OsVSeverity.HIGH -> java.awt.Color(255, 193, 7)
            OsVSeverity.MEDIUM -> java.awt.Color(255, 165, 0)
            OsVSeverity.LOW -> java.awt.Color(108, 117, 125)
        }
    }
    
    /**
     * Get priority for severity level
     */
    fun getPriority(severity: OsVSeverity): Int {
        return when (severity) {
            OsVSeverity.CRITICAL -> 1
            OsVSeverity.HIGH -> 2
            OsVSeverity.MEDIUM -> 3
            OsVSeverity.LOW -> 4
        }
    }
    
    /**
     * Get severity icon/label
     */
    fun getSeverityIcon(severity: OsVSeverity): String {
        return when (severity) {
            OsVSeverity.CRITICAL -> "[CRITICAL]"
            OsVSeverity.HIGH -> "[HIGH]"
            OsVSeverity.MEDIUM -> "[MEDIUM]"
            OsVSeverity.LOW -> "[LOW]"
        }
    }
    
    /**
     * Check if severity meets minimum threshold
     */
    fun meetsThreshold(severity: OsVSeverity, threshold: OsVSeverity): Boolean {
        return getPriority(severity) <= getPriority(threshold)
    }
    
    /**
     * Get severity description
     */
    fun getSeverityDescription(severity: OsVSeverity): String {
        return when (severity) {
            OsVSeverity.CRITICAL -> """
                Critical: Vulnerability requires immediate remediation.
                Risk of complete system compromise.
            """.trimIndent()
            OsVSeverity.HIGH -> """
                High: Vulnerability should be addressed promptly.
                Significant security risk present.
            """.trimIndent()
            OsVSeverity.MEDIUM -> """
                Medium: Vulnerability should be scheduled for remediation.
                Moderate security impact.
            """.trimIndent()
            OsVSeverity.LOW -> """
                Low: Vulnerability can be addressed in routine maintenance.
                Minimal security impact.
            """.trimIndent()
        }
    }
}
