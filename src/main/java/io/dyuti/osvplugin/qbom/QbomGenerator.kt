// QBOM Generator — Post-Quantum Cryptography Bill of Materials
package io.dyuti.osvplugin.qbom

import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates QBOM documents from discovered post-quantum cryptographic assets.
 */
class QbomGenerator(
    private val appName: String = "project",
    private val appVersion: String = "1.0.0",
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isoTimestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    fun generate(assets: List<QuantumAsset>): String = generateCycloneDx(assets)

    private fun generateCycloneDx(assets: List<QuantumAsset>): String {
        val now = isoTimestamp.format(Instant.now())
        val components = assets.mapIndexed { index, asset -> toComponent(asset, index) }

        val doc =
            QbomCycloneDx(
                metadata =
                    QbomMetadata(
                        timestamp = now,
                        tools = listOf(QbomTool(version = appVersion)),
                        properties =
                            listOf(
                                QbomProperty("projectName", appName),
                                QbomProperty("assetCount", components.size.toString()),
                                QbomProperty("pqcAssets", assets.count { it.type == QuantumAssetType.PQC_ALGORITHM }.toString()),
                                QbomProperty(
                                    "vulnerableAssets",
                                    assets.count { it.type == QuantumAssetType.QUANTUM_VULNERABLE }.toString(),
                                ),
                            ),
                    ),
                components = components,
            )

        return gson.toJson(doc)
    }

    private fun toComponent(
        asset: QuantumAsset,
        index: Int,
    ): QbomComponent {
        val bomRef = "quantum-asset-${index + 1}"

        val assetType =
            when (asset.type) {
                QuantumAssetType.PQC_ALGORITHM -> "algorithm"
                QuantumAssetType.PQC_LIBRARY -> "related-crypto-material"
                QuantumAssetType.PQC_PROTOCOL -> "protocol"
                QuantumAssetType.PQC_STANDARD -> "related-crypto-material"
                QuantumAssetType.PQC_POLICY -> "related-crypto-material"
                QuantumAssetType.HYBRID_KEY_EXCHANGE -> "related-crypto-material"
                QuantumAssetType.QUANTUM_VULNERABLE -> "algorithm"
            }

        return QbomComponent(
            name = asset.name,
            bomRef = bomRef,
            description = "${asset.subtype} discovered in ${asset.sourceFile}:${asset.lineNumber}",
            cryptoProperties =
                QbomCryptoProperties(
                    assetType = assetType,
                    algorithmProperties =
                        QbomAlgorithmProperties(
                            primitive = asset.properties["category"] ?: asset.properties["parameterSet"],
                            parameterSetIdentifier = asset.properties["parameterSet"],
                            classification =
                                when (asset.type) {
                                    QuantumAssetType.QUANTUM_VULNERABLE -> "quantum-vulnerable"
                                    QuantumAssetType.PQC_ALGORITHM -> "post-quantum"
                                    else -> "other"
                                },
                        ),
                ),
            source = QbomSource(file = asset.sourceFile, line = asset.lineNumber),
        )
    }
}
