// OSV Vulnerability Scanner API Client
package io.dyuti.osvplugin.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.AffectedFunction
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig
import io.dyuti.osvplugin.utils.CacheManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
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
class OsVApiService(
    httpClient: HttpClient? = null,
) {
    private val httpClient: HttpClient =
        httpClient
            ?: HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build()

    private val osvApiUrl = "https://api.osv.dev/v1/query"
    private val gson = Gson()
    private val cacheManager by lazy {
        try {
            CacheManager.getInstance()
        } catch (e: Exception) {
            CacheManager()
        }
    }

    private val config by lazy {
        try {
            com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(OsVConfig::class.java)
        } catch (e: Exception) {
            OsVConfig()
        }
    }

    private var requestsThisHour = 0
    private var rateLimitWindowStart = System.currentTimeMillis()

    companion object {
        fun getInstance(): OsVApiService =
            com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(OsVApiService::class.java)
    }

    @Throws(OsVApiException::class)
    fun queryVulnerabilities(
        packageName: String,
        ecosystem: String,
        version: String,
    ): List<Vulnerability> {
        val cacheKey = "$packageName:$ecosystem:$version"
        cacheManager.getCachedVulnerabilities(cacheKey)?.let { return it }

        if (!checkRateLimit()) {
            throw OsVApiException("Rate limit exceeded")
        }

        val requestJson = buildQueryRequest(packageName, ecosystem, version)
        val request =
            HttpRequest
                .newBuilder(URI(osvApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build()

        return executeRequest(request, cacheKey) { response ->
            parseVulnerabilities(response)
        }
    }

    private fun checkRateLimit(requestsNeeded: Int = 1): Boolean {
        if (!config.rateLimitEnabled) return true

        val now = System.currentTimeMillis()
        if (now - rateLimitWindowStart > 3600000) {
            rateLimitWindowStart = now
            requestsThisHour = 0
        }
        return (requestsThisHour + requestsNeeded) <= config.rateLimitRequestsPerHour
    }

    private fun incrementRequestCount(count: Int = 1) {
        requestsThisHour += count
    }

    fun clearCache() {
        cacheManager.invalidateAll()
    }

    private inline fun <T> executeRequest(
        request: HttpRequest,
        cacheKey: String,
        block: (String) -> T,
    ): T =
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                throw OsVApiException("API request failed: ${response.statusCode()}")
            }

            val body = response.body() ?: throw OsVApiException("Empty response body")

            incrementRequestCount()

            val result = block(body)
            @Suppress("UNCHECKED_CAST")
            cacheManager.cacheVulnerabilities(cacheKey, result as List<Vulnerability>)
            result
        } catch (e: java.io.IOException) {
            throw OsVApiException("Network error: ${e.message}", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw OsVApiException("Request interrupted: ${e.message}", e)
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
        val aliases = aliasesArray?.mapNotNull { it.asString } ?: emptyList()

        val summary = vuln.getAsJsonPrimitive("summary")?.asString ?: ""
        val details = vuln.getAsJsonPrimitive("details")?.asString ?: ""

        val fixedVersions = mutableListOf<String>()
        vuln.getAsJsonArray("affected")?.forEach { affected ->
            val ranges = affected.asJsonObject.getAsJsonArray("ranges")
            ranges?.forEach { range ->
                val events = range.asJsonObject.getAsJsonArray("events")
                events?.forEach { event ->
                    val fixed = event.asJsonObject.get("fixed")
                    if (fixed != null) {
                        fixedVersions.add(fixed.asString)
                    }
                }
            }
        }

        val references = mutableListOf<String>()
        vuln.getAsJsonArray("references")?.forEach { ref ->
            val url = ref.asJsonObject.get("url")?.asString
            if (url != null) {
                references.add(url)
            }
        }

        val affectedFunctions = parseAffectedFunctions(vuln)
        val (severity, cvssScore) = parseCvssSeverity(vuln)

        return Vulnerability(
            id = id,
            cveIds = aliases.filter { it.startsWith("CVE-") || it.startsWith("GHSA-") },
            summary = summary,
            details = details,
            severity = severity,
            cvssScore = cvssScore,
            affectedVersions = emptyList(),
            fixedVersions = fixedVersions,
            references = references,
            cweIds = mutableListOf(),
            affectedFunctions = affectedFunctions,
        )
    }

    /**
     * Parse CVSS severity from OSV severity array.
     * Priority: CVSS_V3 > CVSS_V2.
     */
    internal fun parseCvssSeverity(vuln: JsonObject): Pair<OsVSeverity, Double?> {
        val severityArray =
            vuln.getAsJsonArray("severity")
                ?: return Pair(OsVSeverity.MEDIUM, null)

        var bestScore: Double? = null
        var foundType = false

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
     * Parse affected function signatures from OSV vulnerability JSON.
     */
    private fun parseAffectedFunctions(vuln: JsonObject): List<AffectedFunction> {
        val functions = mutableListOf<AffectedFunction>()

        val affectedArray =
            try {
                vuln.getAsJsonArray("affected")
            } catch (_: Exception) {
                null
            } ?: return emptyList()

        affectedArray.forEach outer@{ affected ->
            val affectedObj =
                try {
                    affected.asJsonObject
                } catch (_: Exception) {
                    return@outer
                }

            val dbSpecific =
                try {
                    affectedObj.getAsJsonObject("database_specific")
                } catch (_: Exception) {
                    null
                } ?: return@outer

            val functionsArray =
                try {
                    dbSpecific.getAsJsonArray("functions")
                } catch (_: Exception) {
                    null
                } ?: return@outer

            functionsArray.forEach { func ->
                val signature =
                    try {
                        func.asString
                    } catch (_: Exception) {
                        return@forEach
                    }

                val parts = signature.split('.')
                if (parts.size >= 2) {
                    val methodName = parts.last()
                    val className = parts.dropLast(1).joinToString(".")
                    functions.add(AffectedFunction(signature, className, methodName))
                }
            }
        }

        return functions.distinctBy { it.signature }
    }

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

    @Throws(OsVApiException::class)
    fun batchQueryVulnerabilities(dependencies: List<Dependency>): Map<Dependency, List<Vulnerability>> {
        val results = mutableMapOf<Dependency, List<Vulnerability>>()

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

        if (!checkRateLimit(uncachedDependencies.size)) {
            throw OsVApiException("Rate limit exceeded")
        }

        executeParallelQueries(uncachedDependencies, results)
        return results
    }

    /**
     * Execute parallel async queries with Semaphore-based concurrency control.
     * Max 10 concurrent requests. Uses HttpClient.sendAsync() for async execution.
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
            val request =
                HttpRequest
                    .newBuilder(URI(osvApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build()

            httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle { response, throwable ->
                    try {
                        if (throwable != null) {
                            synchronized(errors) {
                                errors.add("${dep.name}: ${throwable.message}")
                            }
                            synchronized(results) {
                                results[dep] = emptyList()
                            }
                        } else if (response.statusCode() != 200) {
                            synchronized(results) {
                                results[dep] = emptyList()
                            }
                        } else {
                            val bodyStr = response.body()
                            if (bodyStr == null) {
                                synchronized(results) {
                                    results[dep] = emptyList()
                                }
                            } else {
                                val vulns = parseVulnerabilities(bodyStr)
                                synchronized(results) {
                                    results[dep] = vulns
                                }
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
                        semaphore.release()
                        finishedLatch.countDown()
                    }
                    null
                }
        }

        val allDone = finishedLatch.await(60, TimeUnit.SECONDS)
        if (!allDone) {
            throw OsVApiException("Batch query timed out waiting for responses")
        }

        errors.forEach { err ->
            System.err.println("Error querying dependency: $err")
        }
    }
}

class OsVApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

fun getAggregatedService(): AggregatedVulnerabilityService = AggregatedVulnerabilityService()
