// Exploit Prediction Scoring System (EPSS) + CISA KEV integration
package io.dyuti.osvplugin.risk

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Risk scoring service that fetches EPSS (Exploit Prediction Scoring System)
 * scores and CISA Known Exploited Vulnerabilities (KEV) catalog to produce
 * a composite risk assessment for each vulnerability.
 */
class RiskScoringService(
    httpClient: HttpClient? = null,
) {
    private val client: HttpClient =
        httpClient
            ?: HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()

    private val gson: Gson = GsonBuilder().create()

    /** In-memory set of CVEs known to CISA KEV (populated once per scan). */
    private var cisaKevSet: Set<String>? = null

    /** In-memory cache: CVE → EPSS entry. */
    private val epssCache = mutableMapOf<String, EpssEntry>()

    /** In-memory cache: CVE → has public exploit (heuristic). */
    private val exploitCache = mutableMapOf<String, Boolean>()

    /**
     * Assess risk for a list of vulnerabilities.
     */
    fun assessRisks(vulnerabilities: List<Vulnerability>): Map<String, RiskAssessment> {
        loadCisaKevCatalog()

        val cveVulns =
            vulnerabilities
                .flatMap { vuln -> vuln.cveIds.map { cve -> cve to vuln } }

        val cveIds = cveVulns.map { it.first }.distinct()
        fetchEpssScores(cveIds)

        return cveVulns.associate { (cveId, vuln) ->
            val assessment = buildAssessment(cveId, vuln)
            cveId to assessment
        }
    }

    /**
     * Build a [RiskAssessment] for a single CVE + vulnerability.
     */
    private fun buildAssessment(
        cveId: String,
        vuln: Vulnerability,
    ): RiskAssessment {
        val cvssScore = vuln.cvssScore
        val epssEntry = epssCache[cveId]
        val epssScore = epssEntry?.epss?.toDoubleOrNull()
        val epssPercentile = epssEntry?.percentile?.toDoubleOrNull()
        val isCisaKev = cisaKevSet?.contains(cveId) ?: false
        val hasPublicExploit = hasPublicExploit(cveId)

        val compositeScore = computeComposite(cvssScore, epssScore, isCisaKev, hasPublicExploit)
        val riskLevel = compositeToLevel(compositeScore)

        return RiskAssessment(
            cveId = cveId,
            cvssScore = cvssScore,
            epssScore = epssScore,
            epssPercentile = epssPercentile,
            isCisaKev = isCisaKev,
            hasPublicExploit = hasPublicExploit,
            compositeScore = compositeScore,
            riskLevel = riskLevel,
        )
    }

    /**
     * Compute composite risk score (0–100).
     */
    fun computeComposite(
        cvss: Double?,
        epss: Double?,
        isKev: Boolean,
        hasExploit: Boolean,
    ): Double {
        val cvssComponent = (cvss ?: 5.0) / 10.0 * 100 * RiskAssessment.CVSS_WEIGHT
        val epssComponent = ((epss ?: 0.0) * 100) * RiskAssessment.EPSS_WEIGHT
        val kevComponent = (if (isKev) 100.0 else 30.0) * RiskAssessment.KEV_WEIGHT
        val exploitComponent = (if (hasExploit) 100.0 else 30.0) * RiskAssessment.EXPLOIT_WEIGHT
        return cvssComponent + epssComponent + kevComponent + exploitComponent
    }

    fun compositeToLevel(score: Double): RiskLevel =
        when {
            score >= 80.0 -> RiskLevel.CRITICAL
            score >= 60.0 -> RiskLevel.HIGH
            score >= 40.0 -> RiskLevel.MEDIUM
            score >= 20.0 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }

    fun riskLevelToSeverity(level: RiskLevel): OsVSeverity =
        when (level) {
            RiskLevel.CRITICAL -> OsVSeverity.CRITICAL
            RiskLevel.HIGH -> OsVSeverity.HIGH
            RiskLevel.MEDIUM -> OsVSeverity.MEDIUM
            RiskLevel.LOW, RiskLevel.MINIMAL -> OsVSeverity.LOW
        }

    fun getEpssPriority(percentile: Double?): String =
        when {
            percentile == null -> "EPSS unavailable"
            percentile >= 0.99 -> "Top 1% likelihood of exploitation"
            percentile >= 0.95 -> "Top 5% likelihood of exploitation"
            percentile >= 0.90 -> "Top 10% likelihood of exploitation"
            percentile >= 0.75 -> "Top 25% likelihood of exploitation"
            percentile >= 0.50 -> "Above average likelihood"
            else -> "Below average likelihood"
        }

    /**
     * Fetch EPSS scores from the FIRST API for a batch of CVEs.
     */
    private fun fetchEpssScores(cveIds: List<String>) {
        if (cveIds.isEmpty()) return

        val batches = cveIds.chunked(100)
        for (batch in batches) {
            val cveParam = batch.joinToString(",")
            val request =
                HttpRequest
                    .newBuilder(URI("https://api.first.org/data/v1/epss?cve=$cveParam&pretty=false"))
                    .header("Accept", "application/json")
                    .GET()
                    .build()

            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    val body = response.body() ?: continue
                    val epssResponse = gson.fromJson(body, EpssResponse::class.java)
                    epssResponse.data?.forEach { entry ->
                        epssCache[entry.cve] = entry
                    }
                }
            } catch (_: Exception) {
                // EPSS API unavailable
            }
        }
    }

    /**
     * Load the CISA KEV catalog from the official JSON feed.
     */
    private fun loadCisaKevCatalog() {
        if (cisaKevSet != null) return

        val request =
            HttpRequest
                .newBuilder(
                    URI(
                        "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json",
                    ),
                ).header("Accept", "application/json")
                .GET()
                .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body() ?: return
                val catalog = gson.fromJson(body, CisaKevCatalogWrapper::class.java)
                cisaKevSet = catalog.vulnerabilities.map { it.cveID }.toSet()
            }
        } catch (_: Exception) {
            cisaKevSet = emptySet()
        }
    }

    /** Wrapper class to parse CISA KEV JSON root. */
    private data class CisaKevCatalogWrapper(
        val vulnerabilities: List<CisaKevEntry>,
    )

    private fun hasPublicExploit(cveId: String): Boolean {
        exploitCache[cveId]?.let { return it }
        val epssPercentile = epssCache[cveId]?.percentile?.toDoubleOrNull()
        val result = epssPercentile != null && epssPercentile >= 0.95
        exploitCache[cveId] = result
        return result
    }

    fun resetCaches() {
        cisaKevSet = null
        epssCache.clear()
        exploitCache.clear()
    }
}
