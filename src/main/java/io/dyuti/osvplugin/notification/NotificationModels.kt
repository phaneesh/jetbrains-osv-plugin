// Project-level and team-level notification settings
package io.dyuti.osvplugin.notification

import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * Project-level settings for OSV plugin behavior on a per-project basis.
 *
 * This allows teams to share consistent policy configurations across their IDE
 * instances. The config is stored in `.idea/osv-plugin-config.json` within the
 * project directory, making it version-controllable and shareable via git.
 *
 * ## Configuration File
 *
 * `.idea/osv-plugin-config.json`:
 * ```json
 * {
 *   "teamName": "Security Team Alpha",
 *   "scanOnStartup": true,
 *   "showNotifications": true,
 *   "notificationThreshold": "MEDIUM",
 *   "autoFixEnabled": false,
 *   "policy": {
 *     "maxSeverity": "HIGH",
 *     "maxCvssScore": 7.0,
 *     "blockCisaKev": true,
 *     "forbiddenLicenses": ["GPL-3.0"]
 *   }
 * }
 * ```
 *
 * @param teamName Human-readable team/project identifier
 * @param scanOnStartup Run vulnerability scan when project opens
 * @param showNotifications Show IDE balloon notifications for new CVEs
 * @param notificationThreshold Minimum severity for notifications
 * @param autoFixEnabled Allow auto-fix to run without confirmation
 * @param policy Team policy override (null = use personal/global settings)
 */
data class TeamConfig(
    val teamName: String? = null,
    val scanOnStartup: Boolean = true,
    val showNotifications: Boolean = true,
    val notificationThreshold: OsVSeverity = OsVSeverity.MEDIUM,
    val autoFixEnabled: Boolean = false,
    val policy: TeamPolicyOverrides? = null,
    val ignoredPackages: List<String> = emptyList(),
)

/**
 * Policy overrides at the project level. These are merged with the user's
 * personal and global policy settings, with project-level taking precedence.
 */
data class TeamPolicyOverrides(
    val maxSeverity: OsVSeverity? = null,
    val maxCvssScore: Double? = null,
    val blockCisaKev: Boolean? = null,
    val blockMalicious: Boolean? = null,
    val forbiddenLicenses: List<String>? = null,
    val ignorePackages: List<String>? = null,
)

/**
 * A notification for a newly discovered vulnerability affecting the project.
 *
 * Notifications are shown as IDE balloon popups and optionally logged to the
 * event log. Each notification links to the vulnerability details and offers
 * quick actions:
 * - View in tool window
 * - Upgrade dependency
 * - Ignore this CVE
 */
data class VulnerabilityNotification(
    val id: String, // Unique notification ID
    val cveId: String?, // CVE identifier (if available)
    val packageName: String, // Affected dependency
    val currentVersion: String, // Currently used version
    val severity: OsVSeverity, // Severity level
    val summary: String, // One-line description
    val fixVersion: String?, // Available fix version
    val isCisaKev: Boolean = false, // CISA known exploited
    val timestamp: Long = System.currentTimeMillis(),
) {
    /** Display text for the IDE notification balloon. */
    fun displayText(): String {
        val severityEmoji =
            when (severity) {
                OsVSeverity.CRITICAL -> "🔴"
                OsVSeverity.HIGH -> "🟠"
                OsVSeverity.MEDIUM -> "🟡"
                OsVSeverity.LOW -> "🔵"
            }
        val cveText = cveId?.let { " ($it)" } ?: ""
        val fixText = fixVersion?.let { " — Fix: $it" } ?: ""
        val kevText = if (isCisaKev) " [CISA KEV]" else ""
        return "$severityEmoji $packageName$cveText$kevText — $summary$fixText"
    }

    /** Tooltip text for the notification. */
    fun tooltipText(): String {
        val builder = StringBuilder()
        builder.appendLine("Vulnerability detected in $packageName")
        cveId?.let { builder.appendLine("CVE: $it") }
        builder.appendLine("Severity: $severity")
        builder.appendLine("Current version: $currentVersion")
        fixVersion?.let { builder.appendLine("Fix version: $it") }
        if (isCisaKev) builder.appendLine("⚠ This vulnerability is actively exploited in the wild (CISA KEV)")
        return builder.toString()
    }
}

/**
 * Metrics about scan execution, aggregated for team reporting.
 */
data class ScanMetrics(
    val scanId: String,
    val projectName: String,
    val timestamp: Long,
    val totalDependencies: Int,
    val vulnerabilitiesFound: Int,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val scanDurationMs: Long = 0,
    val apiCallCount: Int = 0,
    val cacheHits: Int = 0,
) {
    fun severityDistribution(): String {
        val parts = mutableListOf<String>()
        if (criticalCount > 0) parts.add("$criticalCount CRITICAL")
        if (highCount > 0) parts.add("$highCount HIGH")
        if (mediumCount > 0) parts.add("$mediumCount MEDIUM")
        if (lowCount > 0) parts.add("$lowCount LOW")
        return if (parts.isEmpty()) "Clean — no vulnerabilities found" else parts.joinToString(", ")
    }
}
