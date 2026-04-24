// OSV Vulnerability Scanner API Client
package io.dyuti.osvplugin.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig
import io.dyuti.osvplugin.utils.CacheManager
import io.dyuti.osvplugin.utils.SeverityUtil
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.create
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OSV API Service for querying vulnerability data
 * 
 * Features:
 * - Query OSV API for vulnerabilities
 * - Batch query support
 * - Caching with TTL
 * - Rate limiting
 */
class OsVApiService {
    
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val osvApiUrl = "https://api.osv.dev/v1/query"
    
    private val gson = Gson()
    
    private val cacheManager = CacheManager.getInstance()
    
    // Use lazy initialization to avoid issues during test construction
    private val config by lazy { 
        try {
            com.intellij.openapi.components.ServiceManager.getService(OsVConfig::class.java)
        } catch (e: Exception) {
            // Return default config if running outside IntelliJ
            OsVConfig()
        }
    }
    
    private var requestsThisHour = 0
    private var rateLimitWindowStart = System.currentTimeMillis()
    
    companion object {
        fun getInstance(): OsVApiService = OsVApiService()
    }
    
    /**
     * Query OSV API for vulnerabilities by package name and version
     */
    @Throws(OsVApiException::class)
    fun queryVulnerabilities(
        packageName: String,
        ecosystem: String,
        version: String
    ): List<Vulnerability> {
        // Check cache first
        val cacheKey = "$packageName:$ecosystem:$version"
        cacheManager.getCachedVulnerabilities(cacheKey)?.let { return it }
        
        // Check rate limit
        if (!checkRateLimit()) {
            throw OsVApiException("Rate limit exceeded")
        }
        
        val requestJson = buildQueryRequest(packageName, ecosystem, version)
        
        // Create request body with proper OkHttp 4.12 API using toMediaType extension
        val mediaType = "application/json".toMediaType()
        val body = create(mediaType, requestJson)
        
        val request = Request.Builder()
            .url(osvApiUrl)
            .post(body)
            .build()
        
        return executeRequest(request, cacheKey) { response ->
            parseVulnerabilities(response)
        }
    }
    
    /**
     * Batch query vulnerabilities for multiple dependencies
     */
    @Throws(OsVApiException::class)
    fun batchQueryVulnerabilities(
        dependencies: List<Dependency>
    ): Map<Dependency, List<Vulnerability>> {
        val results = mutableMapOf<Dependency, List<Vulnerability>>()
        
        // Filter out dependencies that are already cached
        val uncategorizedDependencies = mutableListOf<Dependency>()
        
        dependencies.forEach { dep ->
            val cacheKey = "${dep.name}:${dep.ecosystem}:${dep.version}"
            cacheManager.getCachedVulnerabilities(cacheKey)?.let {
                results[dep] = it
            } ?: run {
                uncategorizedDependencies.add(dep)
            }
        }
        
        if (uncategorizedDependencies.isEmpty()) {
            return results
        }
        
        // Check rate limit (estimate for batch)
        if (!checkRateLimit(uncategorizedDependencies.size)) {
            throw OsVApiException("Rate limit exceeded")
        }
        
        // Create batch request - send individual requests for now
        // (batch support in OSV API is limited)
        uncategorizedDependencies.forEach { dep ->
            val vulnerabilities = queryVulnerabilities(dep.name, dep.ecosystem, dep.version)
            results[dep] = vulnerabilities
        }
        
        return results
    }
    
    /**
     * Check rate limit before making API calls
     */
    private fun checkRateLimit(requestsNeeded: Int = 1): Boolean {
        if (!config.rateLimitEnabled) {
            return true
        }
        
        val now = System.currentTimeMillis()
        
        // Reset window if expired
        if (now - rateLimitWindowStart > 3600000) { // 1 hour
            rateLimitWindowStart = now
            requestsThisHour = 0
        }
        
        // Check if we have enough quota
        return (requestsThisHour + requestsNeeded) <= config.rateLimitRequestsPerHour
    }
    
    /**
     * Increment request counter
     */
    private fun incrementRequestCount(count: Int = 1) {
        requestsThisHour += count
    }
    
    /**
     * Clear the cache
     */
    fun clearCache() {
        cacheManager.invalidateAll()
    }
    
    private inline fun <T> executeRequest(
        request: Request,
        cacheKey: String,
        block: (String) -> T
    ): T {
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw OsVApiException("API request failed: ${response.code}")
                }
                
                val body = response.body?.string() ?: throw OsVApiException("Empty response body")
                
                // Increment request counter
                incrementRequestCount()
                
                // Cache the result
                val result = block(body)
                @Suppress("UNCHECKED_CAST")
                cacheManager.cacheVulnerabilities(cacheKey, result as List<Vulnerability>)
                result
            }
        } catch (e: IOException) {
            throw OsVApiException("Network error: ${e.message}", e)
        }
    }
    
    private fun buildQueryRequest(packageName: String, ecosystem: String, version: String): String {
        val request = JsonObject()
        request.add("package", buildPackageObject(packageName, ecosystem))
        request.addProperty("version", version)
        return request.toString()
    }
    
    private fun buildPackageObject(packageName: String, ecosystem: String): JsonObject {
        val pkg = JsonObject()
        pkg.addProperty("name", packageName)
        pkg.addProperty("ecosystem", ecosystem)
        return pkg
    }
    
    private fun parseVulnerabilities(responseBody: String): List<Vulnerability> {
        val vulnerabilities = mutableListOf<Vulnerability>()
        
        val json = JsonParser.parseString(responseBody).asJsonObject
        val vulnsArray = json.getAsJsonArray("vulns")
        
        vulnsArray?.forEach { vuln ->
            vulnerabilities.add(parseVulnerability(vuln.asJsonObject))
        }
        
        return vulnerabilities
    }
    
    private fun parseVulnerability(vuln: JsonObject): Vulnerability {
        val id = vuln.getAsJsonPrimitive("id")?.asString ?: ""
        val aliasesArray = vuln.getAsJsonArray("aliases")
        val aliases = if (aliasesArray != null) {
            aliasesArray.mapNotNull { it.asString }
        } else {
            emptyList()
        }
        
        val summary = vuln.getAsJsonPrimitive("summary")?.asString ?: ""
        val details = vuln.getAsJsonPrimitive("details")?.asString ?: ""
        
        // Parse fixed versions from affected range
        val fixedVersions = mutableListOf<String>()
        vuln.getAsJsonArray("affected")?.forEach { affected ->
            val ranges = affected.asJsonObject.getAsJsonArray("ranges")
            ranges.forEach { range ->
                val events = range.asJsonObject.getAsJsonArray("events")
                events.forEach { event ->
                    val fixed = event.asJsonObject.get("fixed")
                    if (fixed != null) {
                        fixedVersions.add(fixed.asString)
                    }
                }
            }
        }
        
        // Parse references
        val references = mutableListOf<String>()
        vuln.getAsJsonArray("references")?.forEach { ref ->
            val url = ref.asJsonObject.get("url")?.asString
            if (url != null) {
                references.add(url)
            }
        }
        
        return Vulnerability(
            id = id,
            cveIds = aliases.filter { it.startsWith("CVE-") || it.startsWith("GHSA-") },
            summary = summary,
            details = details,
            severity = OsVSeverity.MEDIUM,  // Default - OSV may have severity info
            affectedVersions = emptyList(),  // Will be populated from affected ranges
            fixedVersions = fixedVersions,
            references = references,
            cweIds = mutableListOf()
        )
    }
}

/**
 * Exception thrown when OSV API requests fail
 */
class OsVApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Get AggregatedVulnerabilityService instance
 */
fun getAggregatedService(): AggregatedVulnerabilityService {
    return AggregatedVulnerabilityService()
}
