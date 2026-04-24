// New Issues Only Filter
package io.dyuti.osvplugin.filter

import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * Filter for showing only new issues introduced in current branch
 */
class NewIssuesOnlyFilter {
    
    private var baseBranchVulnIds: Set<String> = emptySet()
    
    /**
     * Set base branch vulnerability IDs
     */
    fun setBaseBranchVulnerabilities(vulnerabilities: List<Vulnerability>) {
        baseBranchVulnIds = vulnerabilities.map { it.id }.toSet()
    }
    
    /**
     * Filter vulnerabilities to show only new ones
     */
    fun filterNewIssues(currentVulnerabilities: List<Vulnerability>): List<Vulnerability> {
        return currentVulnerabilities.filter { it.id !in baseBranchVulnIds }
    }
    
    /**
     * Check if vulnerability is new
     */
    fun isNewIssue(vulnerability: Vulnerability): Boolean {
        return vulnerability.id !in baseBranchVulnIds
    }
    
    /**
     * Check if vulnerability exists in base branch
     */
    fun existsInBaseBranch(vulnerability: Vulnerability): Boolean {
        return vulnerability.id in baseBranchVulnIds
    }
    
    /**
     * Clear base branch vulnerability IDs
     */
    fun clearBaseBranchVulnerabilities() {
        baseBranchVulnIds = emptySet()
    }
}
