// Jira Connector - Enterprise Integration
package io.dyuti.osvplugin.integration

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import io.dyuti.osvplugin.api.model.Vulnerability
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * Jira Connector for creating and managing security issues
 */
class JiraConnector(
    private val project: Project,
) {
    private val gson: Gson = GsonBuilder().create()
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()

    // Jira configuration
    var baseUrl: String = ""
    var apiToken: String = ""
    var email: String = ""
    var projectKey: String = ""

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JiraConnector =
            project.getService(JiraConnector::class.java)
                ?: JiraConnector(project)
    }

    /**
     * Configure Jira connection
     */
    fun configure(
        baseUrl: String,
        apiToken: String,
        email: String,
        projectKey: String,
    ) {
        this.baseUrl = baseUrl.removeSuffix("/")
        this.apiToken = apiToken
        this.email = email
        this.projectKey = projectKey
    }

    /**
     * Check if Jira is configured
     */
    fun isConfigured(): Boolean =
        baseUrl.isNotEmpty() && apiToken.isNotEmpty() &&
            email.isNotEmpty() && projectKey.isNotEmpty()

    /**
     * Create a new issue in Jira
     */
    fun createIssue(issue: JiraIssue): String? {
        if (!isConfigured()) return null

        return try {
            val url = "$baseUrl/rest/api/2/issue"
            val request = createPostRequest(url, issue)

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 201) {
                val result = gson.fromJson(response.body(), JiraCreateResponse::class.java)
                result.key
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Search issues in Jira
     */
    fun searchIssues(
        jql: String,
        maxResults: Int = 50,
    ): List<JiraIssue> {
        if (!isConfigured()) return emptyList()

        return try {
            val url = "$baseUrl/rest/api/2/search?jql=$jql&maxResults=$maxResults"
            val request = createGetRequest(url)

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val result = gson.fromJson(response.body(), JiraSearchResponse::class.java)
                result.issues ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get issue by key
     */
    fun getIssue(key: String): JiraIssue? {
        if (!isConfigured()) return null

        return try {
            val url = "$baseUrl/rest/api/2/issue/$key"
            val request = createGetRequest(url)

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val result = gson.fromJson(response.body(), JiraIssueWrapper::class.java)
                result.issue
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Add comment to issue
     */
    fun addComment(
        issueKey: String,
        comment: String,
    ): Boolean {
        if (!isConfigured()) return false

        return try {
            val url = "$baseUrl/rest/api/2/issue/$issueKey/comment"
            val commentBody =
                mapOf(
                    "body" to comment,
                )
            val request = createPostRequest(url, commentBody)

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 201
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update issue fields
     */
    fun updateIssue(
        issueKey: String,
        fields: Map<String, Any>,
    ): Boolean {
        if (!isConfigured()) return false

        return try {
            val url = "$baseUrl/rest/api/2/issue/$issueKey"
            val updateBody = mapOf("fields" to fields)
            val request = createPutRequest(url, updateBody)

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Create vulnerability issue from OSV data
     */
    fun createVulnerabilityIssue(vulnerability: Vulnerability): String? {
        val summary = "${vulnerability.id}: ${vulnerability.summary.take(255)}"

        val description =
            buildString {
                appendLine("*Vulnerability Details*")
                appendLine("---")
                appendLine("Issue: *${vulnerability.id}*")
                appendLine("Summary: ${vulnerability.summary}")
                appendLine("Severity: *${vulnerability.severity}*")
                appendLine("---")
                appendLine("## Description")
                appendLine(vulnerability.details)
                appendLine("---")
                appendLine("## Affected Versions")
                appendLine(vulnerability.affectedVersions.joinToString(", "))
                appendLine("---")
                appendLine("## Fix Versions")
                appendLine(vulnerability.fixedVersions.joinToString(", "))
                appendLine("---")
                appendLine("## References")
                vulnerability.references.forEach { ref ->
                    appendLine("- [$ref]($ref)")
                }
                appendLine("---")
                appendLine("## CWE IDs")
                appendLine(vulnerability.cweIds.joinToString(", "))
                appendLine("---")
                appendLine("## CVE IDs")
                appendLine(vulnerability.cveIds.joinToString(", "))
            }

        val issue =
            JiraIssue(
                fields =
                    JiraIssueFields(
                        project = JiraProject(key = projectKey),
                        summary = summary,
                        description = description,
                        issueType = JiraIssueType(name = "Bug"),
                        priority = mapSeverity(vulnerability.severity),
                        labels = listOf("security", "vulnerability", vulnerability.id),
                    ),
            )

        return createIssue(issue)
    }

    /**
     * Map OSV severity to Jira priority
     */
    private fun mapSeverity(severity: io.dyuti.osvplugin.api.model.OsVSeverity): JiraPriority =
        when (severity) {
            io.dyuti.osvplugin.api.model.OsVSeverity.CRITICAL -> JiraPriority.HIGHEST
            io.dyuti.osvplugin.api.model.OsVSeverity.HIGH -> JiraPriority.HIGH
            io.dyuti.osvplugin.api.model.OsVSeverity.MEDIUM -> JiraPriority.MEDIUM
            io.dyuti.osvplugin.api.model.OsVSeverity.LOW -> JiraPriority.LOW
        }

    /**
     * Create POST HTTP request
     */
    private fun createPostRequest(
        url: String,
        body: Any,
    ): HttpRequest {
        val json = gson.toJson(body)
        return HttpRequest
            .newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .header("Authorization", createAuthHeader())
            .header("User-Agent", "OSV-Vulnerability-Scanner/2.0.0")
            .build()
    }

    /**
     * Create PUT HTTP request
     */
    private fun createPutRequest(
        url: String,
        body: Any,
    ): HttpRequest {
        val json = gson.toJson(body)
        return HttpRequest
            .newBuilder()
            .uri(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .header("Authorization", createAuthHeader())
            .header("User-Agent", "OSV-Vulnerability-Scanner/2.0.0")
            .build()
    }

    /**
     * Create GET HTTP request
     */
    private fun createGetRequest(url: String): HttpRequest =
        HttpRequest
            .newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Authorization", createAuthHeader())
            .header("User-Agent", "OSV-Vulnerability-Scanner/2.0.0")
            .build()

    /**
     * Create Authorization header
     */
    private fun createAuthHeader(): String {
        val credentials = "$email:$apiToken"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }
}

/**
 * Jira Issue Models
 */
data class JiraIssue(
    val fields: JiraIssueFields,
)

data class JiraIssueFields(
    val project: JiraProject,
    val summary: String,
    val description: String,
    val issueType: JiraIssueType,
    val priority: JiraPriority,
    val labels: List<String> = emptyList(),
)

data class JiraProject(
    val key: String,
)

data class JiraIssueType(
    val name: String,
)

enum class JiraPriority {
    HIGHEST,
    HIGH,
    MEDIUM,
    LOW,
}

data class JiraIssueWrapper(
    val issue: JiraIssue,
)

data class JiraCreateResponse(
    val key: String,
)

data class JiraSearchResponse(
    val issues: List<JiraIssue>?,
)

data class JiraCommentBody(
    val comment: JiraCommentText,
)

data class JiraCommentText(
    val content: List<JiraCommentContent> = emptyList(),
    val type: String = "doc",
    val version: Int = 1,
)

data class JiraCommentContent(
    val content: List<JiraCommentContent> = emptyList(),
    val type: String = "paragraph",
    val text: String = "",
)
