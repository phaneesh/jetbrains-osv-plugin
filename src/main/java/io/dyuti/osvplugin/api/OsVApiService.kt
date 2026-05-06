// OSV Vulnerability Scanner API Client
package io.dyuti.osvplugin.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Package
import io.dyuti.osvplugin.api.model.Version
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig
import io.dyuti.osvplugin.utils.CacheManager
import io.dyuti.osvplugin.utils.SeverityUtil
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
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
    private val httpClient: OkHttpClient =
        OkHttpClient
            .Builder()
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
            @Suppress("DEPRECATION")
            com.intellij.openapi.components.ServiceManager
                .getService(OsVConfig::class.java)
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
        version: String,
    ): List<Vulnerability> {
        // Check cache first
        val cacheKey = "$packageName:$ecosystem:$version"
        cacheManager.getCachedVulnerabilities(cacheKey)?.let { return it }

        // Check rate limit
        if (!checkRateLimit()) {
            throw OsVApiException("Rate limit exceeded")
        }

        val requestJson = buildQueryRequest(packageName, ecosystem, version)

        // Create request body with proper OkHttp 4.12 API
        val mediaType = "application/json".toMediaType()

        @Suppress("DEPRECATION")
        val body = RequestBody.create(mediaType, requestJson)

        val request =
            Request
                .Builder()
                .url(osvApiUrl)
                .post(body)
                .build()

        return executeRequest(request, cacheKey) { response ->
            parseVulnerabilities(response)
        }
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
        block: (String) -> T,
    ): T =
        try {
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

    private fun buildQueryRequest(
        packageName: String,
        ecosystem: String,
        version: String,
    ): String {
        val request = JsonObject()
        request.add("package", buildPackageObject(packageName, ecosystem))
        request.addProperty("version", version)
        return request.toString()
    }

    private fun buildPackageObject(
        packageName: String,
        ecosystem: String,
    ): JsonObject {
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
        val aliases =
            if (aliasesArray != null) {
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

        // Parse CVSS severity from severity array
        val (severity, cvssScore) = parseCvssSeverity(vuln)

        return Vulnerability(
            id = id,
            cveIds = aliases.filter { it.startsWith("CVE-") || it.startsWith("GHSA-") },
            summary = summary,
            details = details,
            severity = severity,
            cvssScore = cvssScore,
            affectedVersions = emptyList(), // Will be populated from affected ranges
            fixedVersions = fixedVersions,
            references = references,
            cweIds = mutableListOf(),
        )
    }

    /**
     * Parse CVSS severity from OSV severity array.
     * Priority: CVSS_V3 > CVSS_V2. Returns score and mapped severity.
     */
    internal fun parseCvssSeverity(vuln: JsonObject): Pair<OsVSeverity, Double?> {
        val severityArray =
            vuln.getAsJsonArray("severity")
                ?: return Pair(OsVSeverity.MEDIUM, null)

        // Find the best score: prefer CVSS_V3, fallback to CVSS_V2
        var bestScore: Double? = null
        var foundType = false

        // First pass: try CVSS_V3
        severityArray.forEach { sev ->
            val sevObj = sev.asJsonObject
            val type = sevObj.getAsJsonPrimitive("type")?.asString
            if (type == "CVSS_V3") {
                val scoreStr = sevObj.getAsJsonPrimitive("score")?.asString
                val score = scoreStr?.toDoubleOrNull()
                if (score != null) {
                    bestScore = score
                    foundType = true
                }
            }
        }

        // Second pass: try CVSS_V2 if no V3 found
        if (!foundType) {
            severityArray.forEach { sev ->
                val sevObj = sev.asJsonObject
                val type = sevObj.getAsJsonPrimitive("type")?.asString
                if (type == "CVSS_V2") {
                    val scoreStr = sevObj.getAsJsonPrimitive("score")?.asString
                    val score = scoreStr?.toDoubleOrNull()
                    if (score != null) {
                        bestScore = score
                        foundType = true
                    }
                }
            }
        }

        return Pair(mapCvssToSeverity(bestScore), bestScore)
    }

    /**
     * Map a CVSS score to OsVSeverity enum.
     * - 9.0–10.0 → CRITICAL
     * - 7.0–8.9 → HIGH
     * - 4.0–6.9 → MEDIUM
     * - 0.1–3.9 → LOW
     */
    internal fun mapCvssToSeverity(score: Double?): OsVSeverity {
        if (score == null) return OsVSeverity.MEDIUM
        return when {
            score >= 9.0 -> OsVSeverity.CRITICAL
            score >= 7.0 -> OsVSeverity.HIGH
            score >= 4.0 -> OsVSeverity.MEDIUM
            score > 0.0 -> OsVSeverity.LOW
            else -> OsVSeverity.MEDIUM
        }
    }

    /**
     * Batch query vulnerabilities for multiple dependencies using OSV API.
     * Executes queries in parallel with max 10 concurrent requests for performance.
     */
    @Throws(OsVApiException::class)
    fun batchQueryVulnerabilities(dependencies: List<Dependency>): Map<Dependency, List<Vulnerability>> {
        val results = mutableMapOf<Dependency, List<Vulnerability>>()

        // Check cache first
        val uncachedDependencies = mutableListOf<Dependency>()
        dependencies.forEach { dep ->
            val cacheKey = "${dep.name}:${dep.ecosystem}:${dep.version}"
            cacheManager.getCachedVulnerabilities(cacheKey)?.let {
                results[dep] = it
            } ?: run {
                uncachedDependencies.add(dep)
            }
        }

        if (uncachedDependencies.isEmpty()) {
            return results
        }

        // Check rate limit before any async calls
        if (!checkRateLimit(uncachedDependencies.size)) {
            throw OsVApiException("Rate limit exceeded")
        }

        // Execute parallel async queries with max 10 concurrent
        executeParallelQueries(uncachedDependencies, results)
        return results
    }

    /**
     * Execute parallel async queries with Semaphore-based concurrency control.
     * Max 10 concurrent requests. Uses OkHttp Call.enqueue() for async execution.
     */
    private fun executeParallelQueries(
        dependencies: List<Dependency>,
        results: MutableMap<Dependency, List<Vulnerability>>,
    ) {
        val maxConcurrent = 10
        val semaphore = Semaphore(maxConcurrent)
        val finishedLatch = CountDownLatch(dependencies.size)
        val errors = mutableListOf<String>()

        dependencies.forEach { dep ->
            semaphore.acquire()
            val requestJson = buildQueryRequest(dep.name, dep.ecosystem, dep.version)
            val mediaType = "application/json".toMediaType()

            @Suppress("DEPRECATION")
            val body = RequestBody.create(mediaType, requestJson)
            val request =
                Request
                    .Builder()
                    .url(osvApiUrl)
                    .post(body)
                    .build()

            httpClient.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        synchronized(errors) {
                            errors.add("${dep.name}: ${e.message}")
                        }
                        synchronized(results) {
                            results[dep] = emptyList()
                        }
                        semaphore.release()
                        finishedLatch.countDown()
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        try {
                            if (!response.isSuccessful) {
                                synchronized(results) {
                                    results[dep] = emptyList()
                                }
                            } else {
                                val bodyStr = response.body?.string()
                                if (bodyStr == null) {
                                    synchronized(results) {
                                        results[dep] = emptyList()
                                    }
                                } else {
                                    val vulns = parseVulnerabilities(bodyStr)
                                    synchronized(results) {
                                        results[dep] = vulns
                                    }
                                    // Cache the result
                                    val cacheKey = "${dep.name}:${dep.ecosystem}:${dep.version}"
                                    cacheManager.cacheVulnerabilities(cacheKey, vulns)
                                    incrementRequestCount()
                                }
                            }
                        } catch (e: Exception) {
                            synchronized(errors) {
                                errors.add("${dep.name}: ${e.message}")
                            }
                            synchronized(results) {
                                results[dep] = emptyList()
                            }
                        } finally {
                            response.close()
                            semaphore.release()
                            finishedLatch.countDown()
                        }
                    }
                },
            )
        }

        // Wait for all requests to complete (with generous timeout)
        val allDone = finishedLatch.await(60, TimeUnit.SECONDS)
        if (!allDone) {
            throw OsVApiException("Batch query timed out waiting for responses")
        }

        // Log any errors that occurred
        errors.forEach { err ->
            System.err.println("Error querying dependency: $err")
        }
    }
}

/**
 * Exception thrown when OSV API requests fail
 */
class OsVApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Get AggregatedVulnerabilityService instance
 */
fun getAggregatedService(): AggregatedVulnerabilityService = AggregatedVulnerabilityService()
