// Risk scoring data models
package io.dyuti.osvplugin.risk

/**
 * Composite risk assessment for a vulnerability. Combines multiple
 * independent risk signals into a single prioritized score.
 *
 * ## Risk Signals
 *
 * | Signal | Source | Description |
 * |--------|--------|-------------|
 * | CVSS Score | OSV/NVD | Impact severity (0–10) |
 * | EPSS Score | FIRST | Probability of exploitation (0–1) |
 * | EPSS Percentile | FIRST | Score relative to all CVEs (0–1) |
 * | CISA KEV | CISA | Known exploited in-the-wild (boolean) |
 * | Has Exploit | GitHub/GitLab | Public exploit code available (boolean) |
 *
 * ## Composite Score
 *
 * The composite score (0–100) is computed as a weighted combination:
 * ```
 * composite = (cvss * 40%) + (epss * 30 * 30%) + (kev * 30 * 20%) + (hasExploit * 10 * 10%)
 * ```
 *
 * This formula prioritizes:
 * 1. **Severity** (CVSS) — how bad the vulnerability is
 * 2. **Likelihood** (EPSS) — probability of exploitation
 * 3. **Active threat** (KEV) — already exploited in the wild
 * 4. **Weaponization** (exploit code) — availability of exploit tools
 *
 * @param cveId The CVE identifier (e.g. `CVE-2021-44228`)
 * @param cvssScore CVSS base score 0.0–10.0 (null if unavailable)
 * @param epssScore EPSS exploitation probability 0.0–1.0 (null if unavailable)
 * @param epssPercentile EPSS percentile 0.0–1.0 (null if unavailable)
 * @param isCisaKev Whether listed in CISA Known Exploited Vulnerabilities catalog
 * @param hasPublicExploit Whether public exploit code exists (GitHub/GitLab)
 * @param compositeScore Weighted composite risk score 0–100
 * @param riskLevel Human-readable risk level derived from composite score
 */
data class RiskAssessment(
    val cveId: String,
    val cvssScore: Double?,
    val epssScore: Double?,
    val epssPercentile: Double?,
    val isCisaKev: Boolean,
    val hasPublicExploit: Boolean,
    val compositeScore: Double,
    val riskLevel: RiskLevel,
) {
    companion object {
        /** Weights for composite score calculation. */
        const val CVSS_WEIGHT = 0.40

        /** EPSS gets scaled from 0–1 → 0–30. */
        const val EPSS_MAX = 30.0
        const val EPSS_WEIGHT = 0.30

        /** CISA KEV binary: 0 or 30. */
        const val KEV_MAX = 30.0
        const val KEV_WEIGHT = 0.20

        /** Public exploit binary: 0 or 10. */
        const val EXPLOIT_MAX = 10.0
        const val EXPLOIT_WEIGHT = 0.10
    }
}

/**
 * Risk level derived from composite score thresholds.
 */
enum class RiskLevel {
    CRITICAL, // >= 80
    HIGH, // >= 60
    MEDIUM, // >= 40
    LOW, // >= 20
    MINIMAL, // < 20
}

/**
 * Raw EPSS API response structure.
 *
 * ```json
 * {
 *   "status": "OK",
 *   "status-code": 200,
 *   "data": [
 *     { "cve": "CVE-2021-44228", "epss": "0.97565", "percentile": "1.00000" }
 *   ]
 * }
 * ```
 */
data class EpssResponse(
    val status: String,
    val data: List<EpssEntry>?,
)

data class EpssEntry(
    val cve: String,
    val epss: String,
    val percentile: String,
)

/**
 * CISA KEV catalog entry structure.
 *
 * ```json
 * {
 *   "vulnerabilityName": "Log4j2 Remote Code Execution",
 *   "cveID": "CVE-2021-44228",
 *   "vendorProject": "Apache",
 *   "product": "Log4j2",
 *   ...
 * }
 * ```
 */
data class CisaKevCatalog(
    val vulnerabilities: List<CisaKevEntry>,
)

data class CisaKevEntry(
    val cveID: String,
    val vulnerabilityName: String,
    val vendorProject: String,
    val product: String,
)
