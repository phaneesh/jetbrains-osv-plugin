// Differential analysis engine — compare two scan snapshots
package io.dyuti.osvplugin.diff

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.notification.ScanMetrics
import io.dyuti.osvplugin.risk.RiskAssessment

/**
 * Performs differential analysis between two scan snapshots to identify
 * security posture changes.
 *
 * ## Usage
 *
 * ```kotlin
 * val baseline = ScanSnapshot.fromVulnerabilities("v1.2.0", deps1, vulns1)
 * val current = ScanSnapshot.fromVulnerabilities("v1.3.0", deps2, vulns2)
 * val diff = DiffAnalyzer.compare(baseline, current)
 *
 * if (diff.hasActionableChanges()) {
 *     println(diff.summary())
 *     diff.newVulnerabilities.forEach { println(it.vulnerability.id) }
 * }
 * ```
 *
 * ## Identity Matching
 *
 * Vulnerabilities are matched by `(packageName + OSV ID)`. This assumes the
 * same vulnerability ID refers to the same CVE across scans. Packages are
 * matched by `(name + version + ecosystem)`.
 */
class DiffAnalyzer {
    companion object {
        /**
         * Compare two scan snapshots and produce a [DiffResult].
         */
        @JvmStatic
        fun compare(
            baseline: ScanSnapshot,
            current: ScanSnapshot,
        ): DiffResult {
            // Build lookup sets for efficient comparison
            val baselineVulns = baseline.vulnerabilities.associateBy { baseline.vulnKey(it) }
            val currentVulns = current.vulnerabilities.associateBy { current.vulnKey(it) }
            val baselinePkgs = baseline.dependencies.associateBy { baseline.pkgKey(it) }
            val currentPkgs = current.dependencies.associateBy { current.pkgKey(it) }

            // 1. Find new vulnerabilities
            val newVulns =
                currentVulns
                    .filterKeys { it !in baselineVulns }
                    .map { (key, vuln) ->
                        VulnerabilityChange(
                            packageName = vuln.packageName,
                            packageVersion = current.dependencies.find { it.name == vuln.packageName }?.version ?: "?",
                            vulnerability = vuln.toModel(),
                            changeType = ChangeType.NEW,
                        )
                    }

            // 2. Find resolved vulnerabilities
            val resolvedVulns =
                baselineVulns
                    .filterKeys { it !in currentVulns }
                    .map { (key, vuln) ->
                        VulnerabilityChange(
                            packageName = vuln.packageName,
                            packageVersion = baseline.dependencies.find { it.name == vuln.packageName }?.version ?: "?",
                            vulnerability = vuln.toModel(),
                            changeType = ChangeType.RESOLVED,
                        )
                    }

            // 3. Find severity changes for vulns present in both
            val severityChanges = mutableListOf<SeverityChange>()
            for ((key, currentVuln) in currentVulns) {
                val baselineVuln = baselineVulns[key]
                if (baselineVuln != null && baselineVuln.severity != currentVuln.severity) {
                    severityChanges.add(
                        SeverityChange(
                            packageName = currentVuln.packageName,
                            packageVersion = current.dependencies.find { it.name == currentVuln.packageName }?.version ?: "?",
                            vulnId = currentVuln.id,
                            previousSeverity = baselineVuln.severity,
                            currentSeverity = currentVuln.severity,
                        ),
                    )
                }
            }

            // 4. Find new packages
            val newPackages =
                currentPkgs
                    .filterKeys { it !in baselinePkgs }
                    .values
                    .map { PackageChange(it.name, it.version, it.ecosystem) }

            // 5. Find removed packages
            val removedPackages =
                baselinePkgs
                    .filterKeys { it !in currentPkgs }
                    .values
                    .map { PackageChange(it.name, it.version, it.ecosystem) }

            // 6. Count unchanged vulns
            val unchangedCount = currentVulns.count { (key, _) -> key in baselineVulns }

            return DiffResult(
                baselineId = baseline.id,
                currentId = current.id,
                newVulnerabilities = newVulns,
                resolvedVulnerabilities = resolvedVulns,
                severityChanges = severityChanges,
                newPackages = newPackages,
                removedPackages = removedPackages,
                unchangedCount = unchangedCount,
            )
        }

        /**
         * Create a [ScanSnapshot] from raw dependency + vulnerability lists.
         */
        @JvmStatic
        fun createSnapshot(
            id: String,
            dependencies: List<io.dyuti.osvplugin.api.model.Dependency>,
            vulnerabilities: List<Vulnerability>,
            timestamp: Long = System.currentTimeMillis(),
        ): ScanSnapshot {
            val snapshotDeps =
                dependencies.map {
                    ScanSnapshot.SnapshotDependency(
                        name = it.name,
                        version = it.version,
                        ecosystem = it.ecosystem,
                    )
                }
            val snapshotVulns =
                vulnerabilities.map {
                    ScanSnapshot.SnapshotVulnerability(
                        id = it.id,
                        cveId = it.cveIds.firstOrNull(),
                        packageName = it.packageName,
                        severity = it.severity,
                        cvssScore = it.cvssScore,
                        summary = it.summary,
                    )
                }
            return ScanSnapshot(id, timestamp, snapshotDeps, snapshotVulns)
        }
    }
}

/**
 * Convert a snapshot vulnerability back to a full [Vulnerability] model.
 */
private fun ScanSnapshot.SnapshotVulnerability.toModel(): Vulnerability =
    Vulnerability(
        id = id,
        summary = summary,
        details = "",
        severity = severity,
        cvssScore = cvssScore,
        affectedVersions = emptyList(),
        fixedVersions = emptyList(),
        cveIds = listOfNotNull(cveId),
        references = emptyList(),
        cweIds = emptyList(),
        lineNumber = null,
        affectedFunctions = emptyList(),
    )
