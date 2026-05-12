// IDE Notification Service for OSV plugin
package io.dyuti.osvplugin.notificationservice

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.notification.VulnerabilityNotification

/**
 * Service for displaying real-time IDE notifications (balloons) for newly discovered vulnerabilities.
 *
 * Uses IntelliJ's notification system to show non-intrusive popup alerts when scans find
 * vulnerabilities that meet the configured severity threshold.
 */
object NotificationService {
    /**
     * The notification group ID registered in plugin.xml.
     */
    const val NOTIFICATION_GROUP_ID = "OSV Vulnerability Alerts"

    /**
     * The [NotificationGroup] instance, lazily created.
     */
    @Suppress("DEPRECATION")
    private val notificationGroup: NotificationGroup by lazy {
        NotificationGroup(NOTIFICATION_GROUP_ID, NotificationDisplayType.BALLOON, true)
    }

    /**
     * Show a notification for a single vulnerability.
     *
     * @param project The current project (for context)
     * @param vulnNotification The vulnerability notification data
     */
    fun showVulnerabilityNotification(
        project: Project?,
        vulnNotification: VulnerabilityNotification,
    ) {
        val type = mapSeverityToNotificationType(vulnNotification.severity)
        val title = buildNotificationTitle(vulnNotification)
        val content = buildNotificationContent(vulnNotification)

        val notification = notificationGroup.createNotification(title, content, type)
        Notifications.Bus.notify(notification, project)
    }

    /**
     * Show a batch notification for multiple vulnerabilities found in a single scan.
     *
     * @param project The current project
     * @param vulnNotifications List of vulnerability notifications
     */
    fun showBatchNotification(
        project: Project?,
        vulnNotifications: List<VulnerabilityNotification>,
    ) {
        if (vulnNotifications.isEmpty()) return

        val maxSeverity = vulnNotifications.minByOrNull { it.severity.ordinal }?.severity ?: OsVSeverity.LOW
        val type = mapSeverityToNotificationType(maxSeverity)

        val criticalCount = vulnNotifications.count { it.severity == OsVSeverity.CRITICAL }
        val highCount = vulnNotifications.count { it.severity == OsVSeverity.HIGH }
        val mediumCount = vulnNotifications.count { it.severity == OsVSeverity.MEDIUM }

        val title =
            when {
                criticalCount > 0 -> "$criticalCount Critical Vulnerabilities Found"
                highCount > 0 -> "$highCount High Severity Vulnerabilities Found"
                else -> "${vulnNotifications.size} Vulnerabilities Found"
            }

        val parts = mutableListOf<String>()
        if (criticalCount > 0) parts.add("$criticalCount CRITICAL")
        if (highCount > 0) parts.add("$highCount HIGH")
        if (mediumCount > 0) parts.add("$mediumCount MEDIUM")

        val content =
            buildString {
                append("Found ${vulnNotifications.size} vulnerabilities: ")
                append(parts.joinToString(", "))
                appendLine()
                appendLine()
                vulnNotifications.take(5).forEach { v ->
                    appendLine("- ${v.displayText()}")
                }
                if (vulnNotifications.size > 5) {
                    appendLine("- ... and ${vulnNotifications.size - 5} more")
                }
            }

        val notification = notificationGroup.createNotification(title, content.trim(), type)
        Notifications.Bus.notify(notification, project)
    }

    /**
     * Show a notification that a scan completed with no vulnerabilities.
     */
    fun showCleanScanNotification(project: Project?) {
        val notification =
            notificationGroup.createNotification(
                "No Vulnerabilities Found",
                "OSV scan completed. No known vulnerabilities detected in project dependencies.",
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    /**
     * Filter notifications by severity threshold and show qualifying ones.
     *
     * @param project The current project
     * @param vulnNotifications All notifications from scan
     * @param threshold Minimum severity to display
     * @param showClean Whether to show "clean scan" notification when no vulns
     */
    fun notifyByThreshold(
        project: Project?,
        vulnNotifications: List<VulnerabilityNotification>,
        threshold: OsVSeverity,
        showClean: Boolean = true,
    ) {
        val filtered = vulnNotifications.filter { it.severity.ordinal <= threshold.ordinal }
        when {
            filtered.isEmpty() && showClean -> {
                showCleanScanNotification(project)
            }

            filtered.size == 1 -> {
                showVulnerabilityNotification(project, filtered[0])
            }

            filtered.size > 1 -> {
                showBatchNotification(project, filtered)
            }

            else -> { /* no-op */ }
        }
    }

    /**
     * Convert a [Vulnerability] to a [VulnerabilityNotification] for display.
     */
    fun vulnerabilityToNotification(
        vuln: Vulnerability,
        depName: String,
        depVersion: String,
    ): VulnerabilityNotification {
        val fixVersion = vuln.fixedVersions.firstOrNull()
        return VulnerabilityNotification(
            id = vuln.id,
            cveId = vuln.cveIds.firstOrNull() ?: vuln.ghsaIds.firstOrNull(),
            packageName = depName,
            currentVersion = depVersion,
            severity = vuln.severity,
            summary = vuln.summary,
            fixVersion = fixVersion,
            isCisaKev =
                vuln.summary.contains("CISA KEV", ignoreCase = true) ||
                    vuln.details.contains("CISA KEV", ignoreCase = true),
        )
    }

    private fun mapSeverityToNotificationType(severity: OsVSeverity): NotificationType =
        when (severity) {
            OsVSeverity.CRITICAL -> NotificationType.ERROR
            OsVSeverity.HIGH -> NotificationType.WARNING
            OsVSeverity.MEDIUM -> NotificationType.WARNING
            OsVSeverity.LOW -> NotificationType.INFORMATION
        }

    private fun buildNotificationTitle(vn: VulnerabilityNotification): String {
        val prefix =
            when (vn.severity) {
                OsVSeverity.CRITICAL -> "CRITICAL"
                OsVSeverity.HIGH -> "HIGH"
                OsVSeverity.MEDIUM -> "Medium"
                else -> "Low"
            }
        return "$prefix: ${vn.packageName}"
    }

    private fun buildNotificationContent(vn: VulnerabilityNotification): String =
        buildString {
            append(vn.summary)
            vn.cveId?.let { append(" (CVE: $it)") }
            vn.fixVersion?.let {
                appendLine()
                append("Fix available: $it")
            }
            if (vn.isCisaKev) {
                appendLine()
                append("Actively exploited in the wild (CISA KEV)")
            }
        }
}
