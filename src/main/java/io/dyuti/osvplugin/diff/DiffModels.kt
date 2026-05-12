// Differential analysis — compare two vulnerability scans to detect changes
package io.dyuti.osvplugin.diff

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * The result of comparing a baseline scan with a current scan.
 *
 * Differential analysis helps teams understand what changed between two points
 * in time — for example, before/after a dependency update, or between two
 * versions of their application.
 *
 * ## Use Cases
 *
 * - **Pull request review:** "Did this PR introduce any new vulnerabilities?"
 * - **Release gates:** "Are any new critical issues blocking the release?"
 * - **Dependency update validation:** "Did the Spring Boot upgrade fix or
 *   introduce any CVEs?"
 * - **Compliance reporting:** "Show all new vulnerabilities since last audit"
 *
 * ## Change Categories
 *
 * | Category | Description |
 * |----------|-------------|
 * | **NEW** | Vulnerability found in current scan, not in baseline |
 * | **RESOLVED** | Vulnerability in baseline, not in current scan |
 * | **SEVERITY_INCREASED** | Same vuln, higher severity in current scan |
 * | **SEVERITY_DECREASED** | Same vuln, lower severity in current scan |
 * | **NEW_PACKAGE** | Dependency added since baseline |
 * | **REMOVED_PACKAGE** | Dependency removed since baseline |
 *
 * @param baselineId Identifier for the baseline snapshot (e.g. "2026-05-01-v1.2.0")
 * @param currentId Identifier for the current snapshot (e.g. "2026-05-07-v1.3.0")
 * @param newVulnerabilities Vulnerabilities present now but not in baseline
 * @param resolvedVulnerabilities Vulnerabilities in baseline but no longer present
 * @param severityChanges Vulnerabilities with changed severity
 * @param newPackages Dependencies added since baseline
 * @param removedPackages Dependencies removed since baseline
 * @param unchangedCount Vulnerabilities that appear in both scans unchanged
 */
data class DiffResult(
    val baselineId: String,
    val currentId: String,
    val newVulnerabilities: List<VulnerabilityChange>,
    val resolvedVulnerabilities: List<VulnerabilityChange>,
    val severityChanges: List<SeverityChange>,
    val newPackages: List<PackageChange>,
    val removedPackages: List<PackageChange>,
    val unchangedCount: Int,
) {
    /** Total number of changes across all categories. */
    val totalChanges: Int
        get() =
            newVulnerabilities.size + resolvedVulnerabilities.size +
                severityChanges.size + newPackages.size + removedPackages.size

    /**
     * Whether any action is needed (new vulns or severity increases).
     */
    fun hasActionableChanges(): Boolean = newVulnerabilities.isNotEmpty() || severityChanges.any { it.isEscalation }

    /**
     * Human-readable summary of the diff.
     */
    fun summary(): String {
        if (totalChanges == 0) return "No changes — scans are identical"
        val parts = mutableListOf<String>()
        if (newVulnerabilities.isNotEmpty()) parts.add("${newVulnerabilities.size} new")
        if (resolvedVulnerabilities.isNotEmpty()) parts.add("${resolvedVulnerabilities.size} resolved")
        if (severityChanges.isNotEmpty()) {
            val escalations = severityChanges.count { it.isEscalation }
            val downgrades = severityChanges.count { !it.isEscalation }
            if (escalations > 0) parts.add("$escalations severity increases")
            if (downgrades > 0) parts.add("$downgrades severity decreases")
        }
        if (newPackages.isNotEmpty()) parts.add("${newPackages.size} new packages")
        if (removedPackages.isNotEmpty()) parts.add("${removedPackages.size} removed packages")
        return parts.joinToString(", ")
    }

    /**
     * Filter changes by minimum severity (for noise reduction).
     */
    fun filterByMinSeverity(minSeverity: OsVSeverity): DiffResult =
        copy(
            newVulnerabilities =
                newVulnerabilities.filter {
                    it.vulnerability.severity.ordinal <= minSeverity.ordinal
                },
            resolvedVulnerabilities =
                resolvedVulnerabilities.filter {
                    it.vulnerability.severity.ordinal <= minSeverity.ordinal
                },
            severityChanges =
                severityChanges.filter {
                    it.currentSeverity.ordinal <= minSeverity.ordinal ||
                        it.previousSeverity.ordinal <= minSeverity.ordinal
                },
        )
}

/**
 * A single vulnerability that appeared or disappeared.
 */
data class VulnerabilityChange(
    val packageName: String,
    val packageVersion: String,
    val vulnerability: Vulnerability,
    val changeType: ChangeType,
)

/**
 * A vulnerability whose severity changed between scans.
 */
data class SeverityChange(
    val packageName: String,
    val packageVersion: String,
    val vulnId: String,
    val previousSeverity: OsVSeverity,
    val currentSeverity: OsVSeverity,
) {
    /** Whether severity increased (more severe now). */
    val isEscalation: Boolean = currentSeverity.ordinal < previousSeverity.ordinal

    /** Whether severity decreased (less severe now). */
    val isDeescalation: Boolean = currentSeverity.ordinal > previousSeverity.ordinal
}

/**
 * A dependency that was added or removed.
 */
data class PackageChange(
    val name: String,
    val version: String,
    val ecosystem: String,
)

enum class ChangeType {
    NEW, // Vulnerability not present in baseline
    RESOLVED, // Vulnerability present in baseline, not in current
}

/**
 * A snapshot of a vulnerability scan at a point in time.
 *
 * Snapshots are used as inputs to [DiffAnalyzer.compare()]. They can be
 * persisted to disk as JSON for historical tracking, or created on-the-fly
 * from an in-memory scan result.
 */
data class ScanSnapshot(
    val id: String, // Unique snapshot identifier
    val timestamp: Long = System.currentTimeMillis(),
    val dependencies: List<SnapshotDependency>,
    val vulnerabilities: List<SnapshotVulnerability>,
    val metadata: Map<String, String> = emptyMap(), // e.g. "git-sha", "version"
) {
    data class SnapshotDependency(
        val name: String,
        val version: String,
        val ecosystem: String,
    )

    data class SnapshotVulnerability(
        val id: String, // OSV ID
        val cveId: String?, // CVE if available
        val packageName: String,
        val severity: OsVSeverity,
        val cvssScore: Double?,
        val summary: String,
    )

    /** Create a lookup key for vuln identity comparison. */
    fun vulnKey(vuln: SnapshotVulnerability): String = vuln.id

    /** Create a lookup key for package identity. */
    fun pkgKey(dep: SnapshotDependency): String = "${dep.name}:${dep.version}:${dep.ecosystem}"
}
