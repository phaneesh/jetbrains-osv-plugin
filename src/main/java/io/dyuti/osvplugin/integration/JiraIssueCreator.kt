// Jira Issue Creator
package io.dyuti.osvplugin.integration

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.api.model.Dependency

/**
 * Jira Issue Creator for creating security issues from vulnerabilities
 */
class JiraIssueCreator(private val project: Project) {
    
    private val jiraConnector: JiraConnector
    
    init {
        jiraConnector = ServiceManager.getService(project, JiraConnector::class.java)
            ?: JiraConnector(project)
    }
    
    /**
     * Create issue for single vulnerability
     */
    fun createIssue(vulnerability: Vulnerability, dependency: Dependency? = null): String? {
        return jiraConnector.createVulnerabilityIssue(vulnerability)
    }
    
    /**
     * Create issues for multiple vulnerabilities
     */
    fun createIssues(vulnerabilities: List<VulnerabilityWithDependency>): List<String> {
        return vulnerabilities.mapNotNull { createIssue(it.vulnerability, it.dependency) }
    }
    
    /**
     * Create issue with custom fields
     */
    fun createIssueWithFields(
        vulnerability: Vulnerability,
        summary: String,
        description: String,
        priority: String = "High"
    ): String? {
        return jiraConnector.createVulnerabilityIssue(vulnerability)
    }
    
    /**
     * Link vulnerability to existing Jira issue
     */
    fun linkToIssue(vulnerability: Vulnerability, issueKey: String): Boolean {
        return jiraConnector.addComment(issueKey, """
            **Vulnerability Linked**: ${vulnerability.id}
            
            ${vulnerability.summary}
            
            Severity: ${vulnerability.severity}
            
            Fix: Upgrade to ${vulnerability.fixedVersions.joinToString(", ")}
        """.trimIndent())
    }
    
    /**
     * Create epic for vulnerability family
     */
    fun createEpic(epicName: String, description: String): String? {
        // Get project key from Jira connector
        val projectKey = jiraConnector.projectKey ?: "PROJ"
        
        val issue = io.dyuti.osvplugin.integration.JiraIssue(
            fields = io.dyuti.osvplugin.integration.JiraIssueFields(
                project = io.dyuti.osvplugin.integration.JiraProject(key = projectKey),
                summary = epicName,
                description = description,
                issueType = io.dyuti.osvplugin.integration.JiraIssueType(name = "Epic"),
                priority = io.dyuti.osvplugin.integration.JiraPriority.HIGH,
                labels = listOf("security", "epic")
            )
        )
        
        return jiraConnector.createIssue(issue)
    }
}

/**
 * Data class for vulnerability with dependency
 */
data class VulnerabilityWithDependency(
    val vulnerability: Vulnerability,
    val dependency: Dependency
)
