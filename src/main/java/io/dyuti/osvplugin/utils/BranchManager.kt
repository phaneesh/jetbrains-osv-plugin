// GitHub Branch Comparison Manager - Simplified
package io.dyuti.osvplugin.utils

import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * Branch comparison manager for vulnerability analysis
 * Simplified version without git4idea dependency
 */
class BranchManager {
    /**
     * Compare vulnerabilities between branches
     */
    fun compareBranches(
        currentVulns: List<Vulnerability>,
        baseVulns: List<Vulnerability>,
    ): BranchComparisonResult {
        val currentIds = currentVulns.map { it.id }.toSet()
        val baseIds = baseVulns.map { it.id }.toSet()

        val addedVulns = currentVulns.filter { it.id !in baseIds }
        val removedVulns = baseVulns.filter { it.id !in currentIds }
        val unchangedVulns = currentVulns.filter { it.id in baseIds }

        return BranchComparisonResult(addedVulns, removedVulns, unchangedVulns)
    }

    /**
     * Filter vulnerabilities to show only new ones
     */
    fun getNewIssues(
        currentVulns: List<Vulnerability>,
        baseVulns: List<Vulnerability>,
    ): List<Vulnerability> {
        val baseIds = baseVulns.map { it.id }.toSet()
        return currentVulns.filter { it.id !in baseIds }
    }
}

/**
 * Data class for branch comparison results
 */
data class BranchComparisonResult(
    val addedVulnerabilities: List<Vulnerability>,
    val removedVulnerabilities: List<Vulnerability>,
    val unchangedVulnerabilities: List<Vulnerability>,
) {
    val totalAdded: Int get() = addedVulnerabilities.size
    val totalRemoved: Int get() = removedVulnerabilities.size
    val totalUnchanged: Int get() = unchangedVulnerabilities.size

    fun hasChanges(): Boolean = totalAdded > 0 || totalRemoved > 0

    fun getSummary(): String = "Added: $totalAdded, Removed: $totalRemoved, Unchanged: $totalUnchanged"
}
