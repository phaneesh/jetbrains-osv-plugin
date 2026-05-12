// Configuration Audit Models
package io.dyuti.osvplugin.configaudit

import io.dyuti.osvplugin.api.model.OsVSeverity

/**
 * Severity levels for configuration audit findings.
 */
enum class ConfigSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO,
    ;

    fun toOsVSeverity(): OsVSeverity =
        when (this) {
            CRITICAL -> OsVSeverity.CRITICAL
            HIGH -> OsVSeverity.HIGH
            MEDIUM -> OsVSeverity.MEDIUM
            LOW, INFO -> OsVSeverity.LOW
        }
}

/**
 * A configuration audit finding — an insecure or suboptimal framework setting.
 */
data class ConfigAuditFinding(
    val id: String,
    val title: String,
    val description: String,
    val severity: ConfigSeverity,
    val framework: String, // e.g. "Spring", "Hibernate", "Log4j"
    val fileName: String, // e.g. "application.properties"
    val lineNumber: Int?, // Line where issue appears (null if not applicable)
    val propertyName: String?, // e.g. "spring.security.debug"
    val recommendation: String, // How to fix
    val cweId: String? = null, // e.g. "CWE-798"
)

/**
 * Result of an audit run.
 */
data class ConfigAuditResult(
    val findings: List<ConfigAuditFinding>,
    val filesScanned: Int,
    val durationMs: Long,
) {
    val criticalCount: Int get() = findings.count { it.severity == ConfigSeverity.CRITICAL }
    val highCount: Int get() = findings.count { it.severity == ConfigSeverity.HIGH }
    val mediumCount: Int get() = findings.count { it.severity == ConfigSeverity.MEDIUM }
    val lowCount: Int get() = findings.count { it.severity == ConfigSeverity.LOW }
    val infoCount: Int get() = findings.count { it.severity == ConfigSeverity.INFO }

    fun hasActionableFindings(): Boolean = findings.any { it.severity <= ConfigSeverity.HIGH }
}
