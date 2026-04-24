// GitHub Advisory API Integration
package io.dyuti.osvplugin.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

/**
 * GitHub Security Advisory API Service
 */
class GitHubAdvisoryApiService {
    
    private val gson: Gson = GsonBuilder().create()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
    
    private val baseUrl = "https://api.github.com/advisories"
    
    /**
     * Query GitHub advisories by package name
     */
    fun queryAdvisories(packageName: String): List<Vulnerability> {
        return try {
            val url = "$baseUrl?package=$packageName"
            val request = createRequest(url)
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                val advisories = gson.fromJson(response.body(), Array<GithubAdvisory>::class.java)
                advisories.map { convertToVulnerability(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Query advisories by CVE ID
     */
    fun queryAdvisoryById(cveId: String): Vulnerability? {
        return try {
            val advisories = queryAdvisories(cveId)
            advisories.find { it.id.contains(cveId, ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Query advisories for multiple packages
     */
    fun queryAdvisoriesForPackages(packageNames: List<String>): Map<String, List<Vulnerability>> {
        return packageNames.associateWith { packageName ->
            queryAdvisories(packageName)
        }
    }
    
    /**
     * Convert GitHub Advisory to Vulnerability model
     */
    private fun convertToVulnerability(advisory: GithubAdvisory): Vulnerability {
        val severity = advisory.severity?.let {
            when {
                it.contains("critical", ignoreCase = true) -> OsVSeverity.CRITICAL
                it.contains("high", ignoreCase = true) -> OsVSeverity.HIGH
                it.contains("medium", ignoreCase = true) -> OsVSeverity.MEDIUM
                else -> OsVSeverity.LOW
            }
        } ?: OsVSeverity.MEDIUM
        
        return Vulnerability(
            id = advisory.ghsaId ?: "GHSA-${advisory.cveId ?: "UNKNOWN"}",
            cveIds = advisory.cveId?.let { listOf(it) } ?: emptyList(),
            summary = advisory.summary ?: "Security Advisory",
            details = advisory.description ?: "No details available",
            severity = severity,
            affectedVersions = advisory.affected?.map { it.packageInfo?.packageName ?: "" } ?: emptyList(),
            fixedVersions = advisory.fixedIn?.toList() ?: emptyList(),
            references = advisory.references?.mapNotNull { it.url } ?: emptyList(),
            cweIds = advisory.cweIds ?: emptyList()
        )
    }
    
    /**
     * Create HTTP request with GitHub headers
     */
    private fun createRequest(url: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "OSV-Vulnerability-Scanner/2.0.0")
            .build()
    }
}

/**
 * GitHub Advisory Models
 */
data class GithubAdvisory(
    val id: String?,
    val ghsaId: String?,
    val cveId: String?,
    val url: String?,
    val repositoryName: String?,
    val packageInfo: GithubPackage?,
    val summary: String?,
    val description: String?,
    val severity: String?,
    val cwes: List<GithubCwe>?,
    val cweIds: List<String>?,
    val vulnerabilities: List<GithubVulnerability>?,
    val affected: List<GithubAffected>?,
    val patched: List<GithubPatched>?,
    val fixedIn: List<String>?,
    val CVSS: GithubCVSS?,
    val cvssScore: Double?,
    val perceivedSeverity: String?,
    val publicationTime: String?,
    val publishedAt: String?,
    val updateTime: String?,
    val updatedAt: String?,
    val disclosureStartTime: String?,
    val disclosureEndTime: String?,
    val references: List<GithubReference>?,
    val advisoryUrls: List<String>?
)

data class GithubPackage(
    val packageName: String?,
    val ecosystem: String?,
    val url: String?
)

data class GithubCwe(
    val cweId: String?,
    val name: String?
)

data class GithubVulnerability(
    val packageInfo: GithubPackage?,
    val vulnerableVersionRange: String?,
    val patchedVersion: String?
)

data class GithubAffected(
    val packageInfo: GithubPackage?,
    val vulnerableVersionRange: String?
)

data class GithubPatched(
    val packageInfo: GithubPackage?,
    val patchedVersion: String?
)

data class GithubCVSS(
    val version: String?,
    val vectorString: String?,
    val score: Double?,
    val severity: String?
)

data class GithubReference(
    val url: String?,
    val source: String?
)
