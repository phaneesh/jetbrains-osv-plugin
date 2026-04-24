// NVD API Integration
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
 * NVD (National Vulnerability Database) API Service
 */
class NvdApiService {
    
    private val gson: Gson = GsonBuilder().create()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
    
    private val baseUrl = "https://services.nvd.nist.gov/rest/json/cves/2.0"
    
    /**
     * Query NVD for vulnerabilities by package name
     */
    fun queryVulnerabilities(packageName: String): List<Vulnerability> {
        return try {
            val url = "$baseUrl?searchKeyword=$packageName"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                val result = gson.fromJson(response.body(), NvdApiResponse::class.java)
                result.vulnerabilities.map { convertToVulnerability(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Query NVD for vulnerabilities by CVE ID
     */
    fun queryVulnerabilityById(cveId: String): Vulnerability? {
        return try {
            val url = "$baseUrl?cveId=$cveId"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                val result = gson.fromJson(response.body(), NvdApiResponse::class.java)
                result.vulnerabilities.firstOrNull()?.let { convertToVulnerability(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Query multiple CVEs in batch
     */
    fun queryMultipleVulnerabilities(cveIds: List<String>): Map<String, Vulnerability> {
        return cveIds.associateWith { id ->
            queryVulnerabilityById(id)
        }.filterValues { it != null }.mapValues { it.value!! }
    }
    
    /**
     * Convert NVD CVE to Vulnerability model
     */
    private fun convertToVulnerability(nvdVuln: NvdVulnerability): Vulnerability {
        val cvss = nvdVuln.cve?.metrics?.cvssMetricV31?.firstOrNull()?.cvssData
            ?: nvdVuln.cve?.metrics?.cvssMetricV30?.firstOrNull()?.cvssData
            ?: nvdVuln.cve?.metrics?.cvssMetricV2?.firstOrNull()?.cvssData
        
        val severity = cvss?.baseSeverity?.let { OsVSeverity.valueOf(it.uppercase()) }
            ?: OsVSeverity.MEDIUM
        
        val baseScore = cvss?.baseScore?.toDoubleOrNull() ?: 0.0
        
        return Vulnerability(
            id = nvdVuln.cve?.id ?: "UNKNOWN",
            cveIds = listOf(nvdVuln.cve?.id ?: "UNKNOWN"),
            summary = nvdVuln.cve?.descriptions?.firstOrNull { it.lang == "en" }?.value ?: "No summary",
            details = nvdVuln.cve?.descriptions?.firstOrNull { it.lang == "en" }?.value ?: "No details",
            severity = severity,
            affectedVersions = emptyList(),
            fixedVersions = emptyList(),
            references = nvdVuln.cve?.references?.mapNotNull { it.url } ?: emptyList(),
            cweIds = emptyList()
        )
    }
}

/**
 * NVD API Response Models
 */
data class NvdApiResponse(
    val totalResults: Int,
    val resultsPerPage: Int,
    val startIndex: Int,
    val vulnerabilities: List<NvdVulnerability>
)

data class NvdVulnerability(
    val cve: NvdCve?
)

data class NvdCve(
    val id: String?,
    val sourceIdentifier: String?,
    val published: String?,
    val lastModified: String?,
    val vulnStatus: String?,
    val descriptions: List<NvdDescription>?,
    val metrics: NvdMetrics?,
    val configurations: List<NvdConfiguration>?,
    val references: List<NvdReference>?
)

data class NvdDescription(
    val lang: String,
    val value: String
)

data class NvdMetrics(
    val cvssMetricV31: List<NvdCvss>? = null,
    val cvssMetricV30: List<NvdCvss>? = null,
    val cvssMetricV2: List<NvdCvss>? = null
)

data class NvdCvss(
    val cvssData: NvdCvssData?,
    val severity: String?
)

data class NvdCvssData(
    val version: String,
    val vectorString: String,
    val baseScore: String,
    val baseSeverity: String,
    val attackVector: String,
    val attackComplexity: String,
    val privilegesRequired: String,
    val userInteraction: String,
    val scope: String,
    val confidentialityImpact: String,
    val integrityImpact: String,
    val availabilityImpact: String
)

data class NvdConfiguration(
    val operator: String?,
    val negate: Boolean?,
    val cpeMatch: List<NvdCpeMatch>?
)

data class NvdCpeMatch(
    val criteria: String?,
    val matchCriteriaId: String?,
    val vulnerable: Boolean?
)

data class NvdReference(
    val url: String?,
    val source: String?,
    val tags: List<String>?
)
