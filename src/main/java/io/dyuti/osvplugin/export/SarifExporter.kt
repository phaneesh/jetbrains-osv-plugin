// SARIF Exporter - Security Assessment Results Format
package io.dyuti.osvplugin.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.diagnostic.Logger
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.privacy.PrivacyService
import java.io.File

/**
 * Export vulnerabilities to SARIF v2.1.0 format
 */
class SarifExporter {
    companion object {
        private val LOG = Logger.getInstance(SarifExporter::class.java)
    }

    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    /**
     * Export vulnerabilities to SARIF format
     */
    fun exportVulnerabilities(
        vulnerabilities: List<VulnerabilityWithDependency>,
        outputFile: String,
    ): Boolean =
        try {
            val sarif = createSarifReport(vulnerabilities)
            val json = gson.toJson(sarif)

            File(outputFile).writeText(json)
            true
        } catch (e: Exception) {
            LOG.error("Error exporting SARIF", e)
            false
        }

    /**
     * Create SARIF report structure
     */
    private fun createSarifReport(vulnerabilities: List<VulnerabilityWithDependency>): SarifReport {
        val run =
            SarifRun(
                tool =
                    SarifTool(
                        driver =
                            SarifDriver(
                                name = "OSV Vulnerability Scanner",
                                version = "2.0.0",
                                informationUri = "https://osv.dev",
                                rules = createRules(vulnerabilities),
                            ),
                    ),
                results = createResults(vulnerabilities),
            )

        return SarifReport(
            version = "2.1.0",
            runs = listOf(run),
        )
    }

    /**
     * Create SARIF rules from vulnerabilities
     */
    private fun createRules(vulnerabilities: List<VulnerabilityWithDependency>): List<SarifRule> =
        vulnerabilities
            .groupBy { it.vulnerability.id }
            .map { (vulnId, vulns) ->
                val vuln = vulns.first().vulnerability
                SarifRule(
                    id = vulnId,
                    name = vuln.id,
                    shortDescription = SarifMessage(vuln.summary),
                    fullDescription = SarifMessage(vuln.details),
                    helpUri = vuln.references.firstOrNull() ?: "https://osv.dev",
                    severity = mapSeverity(vuln.severity),
                    properties =
                        SarifProperties(
                            tags = listOf("vulnerability", "security"),
                            securitySeverity = mapSeverity(vuln.severity),
                        ),
                )
            }

    /**
     * Create SARIF results from vulnerabilities
     */
    private fun createResults(vulnerabilities: List<VulnerabilityWithDependency>): List<SarifResult> =
        vulnerabilities.map { vulnWithDep ->
            // Obfuscate dependency name if privacy mode is enabled
            val depName =
                PrivacyService
                    .getInstance()
                    .obfuscate(vulnWithDep.dependency.name, vulnWithDep.dependency.ecosystem)
            SarifResult(
                ruleId = vulnWithDep.vulnerability.id,
                level = mapSeverity(vulnWithDep.vulnerability.severity),
                message =
                    SarifMessage(
                        text = "${vulnWithDep.vulnerability.id}: ${vulnWithDep.vulnerability.summary}",
                    ),
                locations =
                    listOf(
                        SarifLocation(
                            physicalLocation =
                                SarifPhysicalLocation(
                                    artifactLocation =
                                        SarifArtifactLocation(
                                            uri = "dependency:///$depName",
                                        ),
                                ),
                        ),
                    ),
                properties =
                    SarifResultProperties(
                        securitySeverity = mapSeverity(vulnWithDep.vulnerability.severity),
                        precision = "high",
                    ),
            )
        }

    /**
     * Map OSV severity to SARIF severity
     */
    private fun mapSeverity(severity: OsVSeverity): String =
        when (severity) {
            OsVSeverity.CRITICAL -> "critical"
            OsVSeverity.HIGH -> "high"
            OsVSeverity.MEDIUM -> "medium"
            OsVSeverity.LOW -> "low"
        }
}

/**
 * SARIF Report Structure
 */
data class SarifReport(
    val version: String,
    val runs: List<SarifRun>,
)

/**
 * SARIF Run Structure
 */
data class SarifRun(
    val tool: SarifTool,
    val results: List<SarifResult>,
)

/**
 * SARIF Tool Structure
 */
data class SarifTool(
    val driver: SarifDriver,
)

/**
 * SARIF Driver Structure
 */
data class SarifDriver(
    val name: String,
    val version: String,
    val informationUri: String,
    val rules: List<SarifRule>,
)

/**
 * SARIF Rule Structure
 */
data class SarifRule(
    val id: String,
    val name: String,
    val shortDescription: SarifMessage,
    val fullDescription: SarifMessage,
    val helpUri: String,
    val severity: String,
    val properties: SarifProperties,
)

/**
 * SARIF Message Structure
 */
data class SarifMessage(
    val text: String,
)

/**
 * SARIF Properties Structure
 */
data class SarifProperties(
    val tags: List<String>,
    val securitySeverity: String,
)

/**
 * SARIF Result Structure
 */
data class SarifResult(
    val ruleId: String,
    val level: String,
    val message: SarifMessage,
    val locations: List<SarifLocation>,
    val properties: SarifResultProperties,
)

/**
 * SARIF Result Properties
 */
data class SarifResultProperties(
    val securitySeverity: String,
    val precision: String,
)

/**
 * SARIF Location Structure
 */
data class SarifLocation(
    val physicalLocation: SarifPhysicalLocation,
)

/**
 * SARIF Physical Location
 */
data class SarifPhysicalLocation(
    val artifactLocation: SarifArtifactLocation,
)

/**
 * SARIF Artifact Location
 */
data class SarifArtifactLocation(
    val uri: String,
)

/**
 * Data class for vulnerability with dependency
 */
data class VulnerabilityWithDependency(
    val vulnerability: Vulnerability,
    val dependency: Dependency,
)
