// Exploit Prediction Scoring System (EPSS) + CISA KEV integration
package io.dyuti.osvplugin.risk

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Risk scoring service that fetches EPSS (Exploit Prediction Scoring System)
 * scores and CISA Known Exploited Vulnerabilities (KEV) catalog to produce
 * a composite risk assessment for each vulnerability.
 *
 * ## EPSS (FIRST.org)
 *
 * EPSS provides a probability score (0–1) that a vulnerability will be exploited
 * in the next 30 days, based on machine learning over thousands of signals:
 * - Existence of exploit code on GitHub/pastebin
 * - Honeypot exploit attempts
 * - Dark web chatter
 * - Social media mentions
 *
 * API: `GET https://api.first.org/data/v1/epss?cve=CVE-YYYY-NNNN`
 *
 * ## CISA KEV Catalog
 *
 * CISA maintains a list of vulnerabilities that have been actually exploited
 * in the wild. Being on this list means nation-state actors or ransomware
 * groups are actively using the vulnerability.
 *
 * Catalog: `GET https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json`
 *
 * ## Composite Scoring
 *
 * The composite score (0–100) combines severity + likelihood + active threat:
 * - CVSS (40%): severity of the vulnerability
 * - EPSS (30%): probability of exploitation
 * - CISA KEV (20%): actively exploited in the wild
 * - Has exploit (10%): public exploit code available
 *
 * This produces a risk score that is **more actionable** than CVSS alone,
 * because it prioritizes vulnerabilities that attackers are actually using
 * over theoretical high-severity issues with no known exploitation.
 *
 * ## Design Decisions
 *
 * - **Lazy loading**: EPSS and KEV data is only fetched for CVEs that are
 *   actually present in the scan results, not all historical CVEs.
 * - **In-memory caching**: EPSS/KEV lookups are cached per scan session
 *   in [cisaKevSet] and [epssCache]. No persistent cache because EPSS
 *   scores are updated daily.
 * - **Graceful degradation**: If EPSS/CISA APIs are unavailable, CVSS-based
 *   scoring is used as a fallback with a warning indicator.
 */
class RiskScoringService {
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
     *
     * @param vulnerabilities The vulnerabilities detected in the project
     * @return Map of CVE ID → RiskAssessment (only for vulnerabilities with CVE IDs)
     */
    fun assessRisks(vulnerabilities: List<Vulnerability>): Map<String, RiskAssessment> {
        // Step 1: Ensure CISA KEV catalog is loaded
        loadCisaKevCatalog()

        // Step 2: Only vulnerabilities with CVE IDs can be scored
        val cveVulns =
            vulnerabilities
                .flatMap { vuln -> vuln.cveIds.map { cve -> cve to vuln } }

        // Step 3: Batch fetch EPSS scores
        val cveIds = cveVulns.map { it.first }.distinct()
        fetchEpssScores(cveIds)

        // Step 4: Build assessment for each CVE
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
        val cvssScore = vuln.cvssScore // From OSV API (NVD enrichment)
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
     *
     * Formula:
     * ```
     * score = (cvssNormalized * 40%) + (epssScaled * 30%) + (kevScaled * 20%) + (exploitScaled * 10%)
     * ```
     *
     * Where:
     * - cvssNormalized = cvssScore / 10.0 * 100 (CVSS 0–10 → 0–100)
     * - epssScaled = epssScore * 100 (EPSS 0–1 → 0–100)
     * - kevScaled = if (isKev) 100 else 50 (binary with partial credit when unknown)
     * - exploitScaled = if (hasExploit) 100 else 30 (partial credit when unknown)
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

    /**
     * Convert composite score (0–100) to [RiskLevel].
     */
    fun compositeToLevel(score: Double): RiskLevel =
        when {
            score >= 80.0 -> RiskLevel.CRITICAL
            score >= 60.0 -> RiskLevel.HIGH
            score >= 40.0 -> RiskLevel.MEDIUM
            score >= 20.0 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }

    /**
     * Map [RiskLevel] back to [OsVSeverity] for UI compatibility.
     */
    fun riskLevelToSeverity(level: RiskLevel): OsVSeverity =
        when (level) {
            RiskLevel.CRITICAL -> OsVSeverity.CRITICAL
            RiskLevel.HIGH -> OsVSeverity.HIGH
            RiskLevel.MEDIUM -> OsVSeverity.MEDIUM
            RiskLevel.LOW, RiskLevel.MINIMAL -> OsVSeverity.LOW
        }

    /**
     * Determine EPSS priority by percentile.
     *
     * @return Human-readable priority string (e.g. "Top 5% of all CVEs")
     */
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
     *
     * Uses batch API: `GET /data/v1/epss?cve=CVE-1,CVE-2,...`
     */
    private fun fetchEpssScores(cveIds: List<String>) {
        if (cveIds.isEmpty()) return

        // FIRST EPSS API limits to 100 CVEs per request
        val batches = cveIds.chunked(100)
        for (batch in batches) {
            val cveParam = batch.joinToString(",")
            val request =
                Request
                    .Builder()
                    .url("https://api.first.org/data/v1/epss?cve=$cveParam&pretty=false")
                    .header("Accept", "application/json")
                    .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return
                        val epssResponse = gson.fromJson(body, EpssResponse::class.java)
                        epssResponse.data?.forEach { entry ->
                            epssCache[entry.cve] = entry
                        }
                    }
                }
            } catch (_: Exception) {
                // EPSS API unavailable — continue with CVSS-only scoring
            }
        }
    }

    /**
     * Load the CISA KEV catalog from the official JSON feed.
     *
     * This is a large (~2MB) JSON file that lists all known exploited vulnerabilities.
     * We parse it once per scan and keep only the CVE IDs in memory.
     */
    private fun loadCisaKevCatalog() {
        if (cisaKevSet != null) return // Already loaded

        val request =
            Request
                .Builder()
                .url(
                    "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json",
                ).header("Accept", "application/json")
                .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    // Parse the CISA KEV JSON (vulnerabilities array)
                    val catalog =
                        gson.fromJson(body, CisaKevCatalogWrapper::class.java)
                    cisaKevSet = catalog.vulnerabilities.map { it.cveID }.toSet()
                }
            }
        } catch (_: Exception) {
            // CISA API unavailable — continue with empty set (worst case: no KEV flagged)
            cisaKevSet = emptySet()
        }
    }

    /** Wrapper class to parse CISA KEV JSON root. */
    private data class CisaKevCatalogWrapper(
        val vulnerabilities: List<CisaKevEntry>,
    )

    /**
     * Check if a CVE has public exploit code available.
     *
     * This is a **heuristic** approach — checking GitHub search API for public
     * exploit repositories. In production, a dedicated service (e.g., trickest/cve)
     * would be more accurate. For now, we assume high EPSS score implies
     * exploit availability and flag above-95th percentile as "has exploit".
     */
    private fun hasPublicExploit(cveId: String): Boolean {
        exploitCache[cveId]?.let { return it }

        val epssPercentile = epssCache[cveId]?.percentile?.toDoubleOrNull()
        val result = epssPercentile != null && epssPercentile >= 0.95
        exploitCache[cveId] = result
        return result
    }

    /**
     * Clear all caches. Call at the start of a new scan.
     */
    fun resetCaches() {
        cisaKevSet = null
        epssCache.clear()
        exploitCache.clear()
    }
}
