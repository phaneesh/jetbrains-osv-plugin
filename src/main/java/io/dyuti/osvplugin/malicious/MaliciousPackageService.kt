// Malicious Package Detection Service
// Detects intentionally harmful packages and typosquatting attempts
package io.dyuti.osvplugin.malicious

import io.dyuti.osvplugin.api.OsVApiException
import io.dyuti.osvplugin.api.OsVApiService
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.config.OsVConfig

/**
 * Result of a malicious package check.
 *
 * @param isMalicious true if the package is known to be malicious
 * @param packageName The package name that was checked
 * @param version The version that was checked
 * @param reason Human-readable reason for the classification
 * @param maliciousType Type of malicious activity (malware, backdoor, cryptominer, typosquat)
 * @param references URLs with more information
 */
data class MaliciousCheckResult(
    val isMalicious: Boolean,
    val packageName: String,
    val version: String,
    val reason: String = "",
    val maliciousType: MaliciousType = MaliciousType.UNKNOWN,
    val references: List<String> = emptyList(),
)

/**
 * Types of malicious package activity.
 */
enum class MaliciousType {
    MALWARE,
    BACKDOOR,
    CRYPTOMINER,
    TYPOSQUAT,
    EXFILTRATION,
    SABOTAGE,
    PROTESTWARE,
    UNKNOWN,
}

/**
 * Typosquatting check result.
 *
 * @param originalPackage The legitimate package name that might be targeted
 * @param similarityScore Levenshtein similarity (0.0–1.0, higher = more similar)
 * @param isTyposquat true if this appears to be a typosquat attempt
 */
data class TyposquatResult(
    val originalPackage: String,
    val similarityScore: Double,
    val isTyposquat: Boolean,
)

/**
 * Service that detects malicious packages and typosquatting attempts.
 *
 * ## Data Sources
 *
 * 1. **OSV Database**: Already contains malicious package entries
 *    (e.g., `ua-parser-js` backdoor, `node-ipc` protestware).
 *    We detect these by looking for keywords in vulnerability summaries.
 *
 * 2. **Typosquatting Detection**: Compares package names against a
 *    known-good list using Levenshtein distance. Flags packages whose
 *    names are suspiciously similar to popular packages.
 *
 * ## Usage
 *
 * ```kotlin
 * val detector = MaliciousPackageService()
 * val result = detector.checkPackage(Dependency("ua-parser-js", "0.7.29", "Npm", ...))
 * if (result.isMalicious) {
 *     println("ALERT: ${result.reason}")
 * }
 * ```
 */
class MaliciousPackageService {
    private val apiService by lazy {
        try {
            OsVApiService.getInstance()
        } catch (_: Exception) {
            OsVApiService()
        }
    }

    /**
     * Known malicious package patterns — package names that have been
     * involved in known malicious incidents. Used for fast-path detection.
     */
    private val knownMaliciousPackages =
        setOf(
            // npm incidents
            "ua-parser-js", // Backdoored versions 0.7.29, 0.8.0, 1.0.0
            "node-ipc", // Protestware / wiping code
            "colors", // Protestware / infinite loop
            "faker", // Protestware
            "event-source-polyfill", // Typosquat of event-source-polyfill
            "cross-env", // Typosquat (crossenv)
            "discord.js", // Typosquats common
            // PyPI incidents
            "python3-dateutil", // Typosquat of python-dateutil
            "jeIlyfish", // Typosquat of jellyfish (unicode homoglyph)
            "crypt", // Typosquat variations
            // Maven incidents
            "org.springframework.boot", // Some malicious spring-boot starters found
        )

    /**
     * Keywords that indicate malicious activity in OSV vulnerability summaries.
     */
    private val maliciousKeywords =
        listOf(
            "malicious",
            "malware",
            "backdoor",
            "crypto",
            "miner",
            "mining",
            "exfiltrat",
            "credential",
            "steal",
            "theft",
            "wiper",
            "wipe",
            "delete",
            "destroy",
            "protest",
            "sanction",
            "russia",
            "ukraine",
            "political",
            "homoglyph",
            "typosquat",
            "impersonat",
            "trojan",
            "ransomware",
            "spyware",
        )

    /**
     * Popular packages that are common typosquat targets.
     * Used as the reference list for typosquat detection.
     */
    private val popularPackages =
        listOf(
            // npm — most-downloaded
            "lodash",
            "react",
            "express",
            "axios",
            "typescript",
            "debug",
            "commander",
            "core-js",
            "chalk",
            "request",
            "fs-extra",
            "uuid",
            "moment",
            "underscore",
            "prop-types",
            "babel-runtime",
            "semver",
            "jquery",
            "vue",
            "angular",
            "webpack",
            "babel-core",
            "left-pad",
            "is-odd",
            "is-even", // Known typosquat targets
            "cross-env",
            "rimraf",
            "mkdirp",
            // PyPI — most-downloaded
            "requests",
            "urllib3",
            "botocore",
            "pip",
            "wheel",
            " setuptools",
            "dateutil",
            "jinja2",
            "pandas",
            "numpy",
            "django",
            "flask",
            "certifi",
            "idna",
            "chardet",
            "six",
            // Maven — most-downloaded
            "junit",
            "slf4j",
            "log4j",
            "guava",
            "commons-lang3",
            "spring-boot",
            "hibernate-core",
            "jackson-databind",
            "gson",
            "mockito-core",
        )

    companion object {
        /** Similarity threshold for typosquat flagging (0.0–1.0). */
        const val TYPOSQUAT_THRESHOLD = 0.80

        /** Maximum Levenshtein distance to consider for performance. */
        const val MAX_DISTANCE = 3
    }

    /**
     * Check if a dependency is known to be malicious.
     *
     * Uses three detection layers (fastest first):
     * 1. Known malicious package name list
     * 2. OSV API query with keyword analysis
     * 3. Typosquatting detection against popular packages
     *
     * @param dependency The dependency to check
     * @return [MaliciousCheckResult] with classification
     */
    fun checkPackage(dependency: Dependency): MaliciousCheckResult {
        // Layer 1: Known malicious package names
        if (isKnownMalicious(dependency.name)) {
            return MaliciousCheckResult(
                isMalicious = true,
                packageName = dependency.name,
                version = dependency.version,
                reason = "Package \"${dependency.name}\" is known to have been involved in malicious activity.",
                maliciousType = MaliciousType.MALWARE,
            )
        }

        // Layer 2: OSV API query — check for malicious indicators in vuln data
        val osvResult = checkOsvForMalicious(dependency)
        if (osvResult.isMalicious) {
            return osvResult
        }

        // Layer 3: Typosquatting detection
        val typosquatResult = checkTyposquatting(dependency.name)
        if (typosquatResult.isTyposquat) {
            return MaliciousCheckResult(
                isMalicious = true,
                packageName = dependency.name,
                version = dependency.version,
                reason = "Package \"${dependency.name}\" appears to be a typosquat of popular package \"${typosquatResult.originalPackage}\" (similarity: ${"%.1f".format(
                    typosquatResult.similarityScore * 100,
                )}%).",
                maliciousType = MaliciousType.TYPOSQUAT,
            )
        }

        // Layer 4: Homoglyph attack detection (unicode lookalikes)
        val homoglyphResult = checkHomoglyph(dependency.name)
        if (homoglyphResult != null) {
            return MaliciousCheckResult(
                isMalicious = true,
                packageName = dependency.name,
                version = dependency.version,
                reason = homoglyphResult,
                maliciousType = MaliciousType.TYPOSQUAT,
            )
        }

        // Not malicious
        return MaliciousCheckResult(
            isMalicious = false,
            packageName = dependency.name,
            version = dependency.version,
            reason = "No malicious indicators found.",
        )
    }

    /**
     * Batch-check multiple dependencies for malicious packages.
     */
    fun checkPackages(dependencies: List<Dependency>): List<MaliciousCheckResult> = dependencies.map { checkPackage(it) }

    /**
     * Check if a package name is in the known-malicious list.
     */
    private fun isKnownMalicious(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return knownMaliciousPackages.any { normalized == it.lowercase() }
    }

    /**
     * Query OSV API for a dependency and analyze responses for malicious keywords.
     */
    private fun checkOsvForMalicious(dependency: Dependency): MaliciousCheckResult {
        return try {
            val vulnerabilities =
                apiService.queryVulnerabilities(
                    dependency.name,
                    dependency.ecosystem,
                    dependency.version,
                )

            for (vuln in vulnerabilities) {
                val text = (vuln.summary + " " + vuln.details).lowercase()
                val matchedKeyword = maliciousKeywords.firstOrNull { text.contains(it) }

                if (matchedKeyword != null) {
                    val mtype = classifyMaliciousType(text)
                    return MaliciousCheckResult(
                        isMalicious = true,
                        packageName = dependency.name,
                        version = dependency.version,
                        reason = "OSV reports ${vuln.id}: ${vuln.summary}",
                        maliciousType = mtype,
                        references = vuln.references,
                    )
                }
            }

            MaliciousCheckResult(
                isMalicious = false,
                packageName = dependency.name,
                version = dependency.version,
            )
        } catch (_: OsVApiException) {
            // API failure — assume clean (false negative acceptable)
            MaliciousCheckResult(
                isMalicious = false,
                packageName = dependency.name,
                version = dependency.version,
                reason = "Unable to check OSV API (network error)",
            )
        }
    }

    /**
     * Classify the type of malicious activity from text.
     */
    private fun classifyMaliciousType(text: String): MaliciousType {
        val t = text.lowercase()
        return when {
            t.contains("backdoor") -> MaliciousType.BACKDOOR
            t.contains("crypto") || t.contains("miner") || t.contains("mining") -> MaliciousType.CRYPTOMINER
            t.contains("exfiltrat") || t.contains("steal") || t.contains("credential") -> MaliciousType.EXFILTRATION
            t.contains("wiper") || t.contains("wipe") || t.contains("destroy") -> MaliciousType.SABOTAGE
            t.contains("protest") || t.contains("sanction") || t.contains("political") -> MaliciousType.PROTESTWARE
            t.contains("malware") || t.contains("trojan") || t.contains("ransomware") -> MaliciousType.MALWARE
            else -> MaliciousType.UNKNOWN
        }
    }

    /**
     * Check for typosquatting by comparing against popular packages.
     *
     * Uses normalized Levenshtein distance: similarity = 1 - distance/maxLength.
     * Scores above [TYPOSQUAT_THRESHOLD] with edit distance > 0 trigger a flag.
     */
    fun checkTyposquatting(packageName: String): TyposquatResult {
        val normalized = packageName.lowercase().trim()
        var bestScore = 0.0
        var bestMatch = ""

        for (popular in popularPackages) {
            // Skip exact matches (legitimate package)
            if (normalized == popular.lowercase()) continue

            // Skip if length difference is too large
            if (kotlin.math.abs(normalized.length - popular.length) > MAX_DISTANCE) continue

            val distance = levenshteinDistance(normalized, popular.lowercase())
            val maxLen = kotlin.math.max(normalized.length, popular.length)
            val similarity = if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)

            if (similarity > bestScore) {
                bestScore = similarity
                bestMatch = popular
            }
        }

        return TyposquatResult(
            originalPackage = bestMatch,
            similarityScore = bestScore,
            isTyposquat = bestScore >= TYPOSQUAT_THRESHOLD && levenshteinDistance(normalized, bestMatch.lowercase()) > 0,
        )
    }

    /**
     * Check for unicode homoglyph attacks.
     *
     * Some typosquats use visually similar unicode characters:
     * - `jeIlyfish` vs `jellyfish` (capital I vs lowercase L)
     * - `pаypal` vs `paypal` (Cyrillic 'а' vs Latin 'a')
     *
     * @return A reason string if a homoglyph is detected, or null if clean
     */
    fun checkHomoglyph(packageName: String): String? {
        // Common confusable characters
        val confusables =
            setOf(
                '\u0430', // Cyrillic а (U+0430) looks like Latin a
                '\u0435', // Cyrillic е (U+0435) looks like Latin e
                '\u043E', // Cyrillic о (U+043E) looks like Latin o
                '\u0440', // Cyrillic р (U+0445) looks like Latin p
                '\u0441', // Cyrillic с (U+0441) looks like Latin c
                '\u0445', // Cyrillic х (U+0445) looks like Latin x
            )

        for (char in packageName) {
            if (char in confusables) {
                return "Package \"$packageName\" contains unicode look-alike characters (homoglyph attack)."
            }
        }

        // Check for mixed scripts (Latin + Cyrillic)
        var hasLatin = false
        var hasCyrillic = false
        for (char in packageName) {
            when {
                char in 'a'..'z' || char in 'A'..'Z' -> hasLatin = true
                char in '\u0400'..'\u04FF' -> hasCyrillic = true
            }
        }
        if (hasLatin && hasCyrillic) {
            return "Package \"$packageName\" mixes Latin and Cyrillic scripts (potential homoglyph attack)."
        }

        return null
    }

    /**
     * Calculate Levenshtein distance between two strings.
     *
     * Classic dynamic programming algorithm.
     */
    private fun levenshteinDistance(
        s1: String,
        s2: String,
    ): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val prev = IntArray(s2.length + 1)
        val curr = IntArray(s2.length + 1)

        for (j in 0..s2.length) {
            prev[j] = j
        }

        for (i in 1..s1.length) {
            curr[0] = i
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] =
                    kotlin.math.min(
                        kotlin.math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost,
                    )
            }
            // Swap arrays
            for (j in 0..s2.length) {
                prev[j] = curr[j]
            }
        }

        return prev[s2.length]
    }
}
