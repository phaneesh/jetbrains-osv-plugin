// CBOM Generator — CycloneDX 1.6 Cryptographic Bill of Materials
package io.dyuti.osvplugin.cbom

import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates CBOM (Cryptographic Bill of Materials) documents from discovered crypto assets.
 *
 * Maps internally-discovered [CryptoAsset] instances to CycloneDX 1.6 `cbom` format
 * with `cryptoProperties` per the spec. Fully unit-testable with no IntelliJ dependencies.
 */
class CbomGenerator(
    private val appName: String = "project",
    private val appVersion: String = "1.0.0",
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isoTimestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    /**
     * Generate a CBOM string in the requested format.
     */
    fun generate(assets: List<CryptoAsset>): String = generateCycloneDx(assets)

    // ─── CycloneDX 1.6 CBOM ────────────────────────────────────────

    private fun generateCycloneDx(assets: List<CryptoAsset>): String {
        val now = isoTimestamp.format(Instant.now())
        val components = assets.mapIndexed { index, asset -> toCbomComponent(asset, index) }

        val doc =
            CbomCycloneDx(
                metadata =
                    CbomMetadata(
                        timestamp = now,
                        tools =
                            listOf(
                                CbomTool(version = appVersion),
                            ),
                        properties =
                            listOf(
                                CbomProperty("projectName", appName),
                                CbomProperty("assetCount", components.size.toString()),
                            ),
                    ),
                components = components,
            )

        return gson.toJson(doc)
    }

    // ─── Asset → Component mapping ─────────────────────────────────

    private fun toCbomComponent(
        asset: CryptoAsset,
        index: Int,
    ): CbomComponent {
        val bomRef = "crypto-asset-${index + 1}"
        val cryptoProps = buildCryptoProperties(asset)

        return CbomComponent(
            name = asset.name,
            bomRef = bomRef,
            description = "${asset.subtype} discovered in ${asset.sourceFile}:${asset.lineNumber}",
            cryptoProperties = cryptoProps,
            source =
                CbomSource(
                    file = asset.sourceFile,
                    line = asset.lineNumber,
                ),
        )
    }

    private fun buildCryptoProperties(asset: CryptoAsset): CryptoProperties {
        val assetType =
            when (asset.type) {
                CryptoAssetType.ALGORITHM -> "algorithm"
                CryptoAssetType.CERTIFICATE -> "certificate"
                CryptoAssetType.RELATED_CRYPTO_MATERIAL -> "related-crypto-material"
                CryptoAssetType.PROTOCOL -> "protocol"
            }

        return when (asset.type) {
            CryptoAssetType.ALGORITHM -> {
                CryptoProperties(
                    assetType = assetType,
                    algorithmProperties =
                        AlgorithmProperties(
                            primitive = asset.properties["primitive"],
                            parameterSetIdentifier = asset.properties["algorithm"],
                            mode = asset.properties["mode"],
                            padding = asset.properties["padding"],
                            keySize = asset.properties["keySize"]?.toIntOrNull(),
                        ),
                )
            }

            CryptoAssetType.PROTOCOL -> {
                CryptoProperties(
                    assetType = assetType,
                    protocolProperties =
                        ProtocolProperties(
                            type = asset.properties["primitive"] ?: "tls",
                            version = extractVersion(asset.name),
                            cipherSuites =
                                asset.properties["suites"]
                                    ?.split(",")
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotEmpty() },
                        ),
                )
            }

            CryptoAssetType.CERTIFICATE -> {
                CryptoProperties(
                    assetType = assetType,
                    certificateProperties =
                        CertificateProperties(
                            certificateFormat = asset.name,
                        ),
                )
            }

            CryptoAssetType.RELATED_CRYPTO_MATERIAL -> {
                CryptoProperties(
                    assetType = assetType,
                    relatedCryptoMaterialProperties =
                        RelatedCryptoMaterialProperties(
                            type = asset.subtype,
                            format = asset.properties["primitive"],
                        ),
                )
            }
        }
    }

    /**
     * Extract a version string from a protocol name like "TLSv1.2" or "TLSv1.3".
     */
    private fun extractVersion(name: String): String? {
        val versionPattern = "(TLS|SSL)v?(\\d+(?:\\.\\d+)?)".toRegex()
        return versionPattern.find(name)?.groupValues?.getOrNull(2)
    }
}
