// CBOM Data Models — CycloneDX CBOM 1.6 compatible
package io.dyuti.osvplugin.cbom

import com.google.gson.annotations.SerializedName

/**
 * A cryptographic asset discovered via static analysis.
 *
 * Aligned with CycloneDX CBOM component types:
 *   - algorithm   → cryptographic protocol/algorithm
 *   - certificate → X.509, PGP, etc.
 *   - related-crypto-material → keys, IVs, salts, keystores, providers
 *   - protocol    → TLS, SSL, SSH, etc.
 */
data class CryptoAsset(
    val name: String,
    val type: CryptoAssetType,
    val subtype: String,
    val properties: Map<String, String> = emptyMap(),
    val sourceFile: String,
    val lineNumber: Int,
)

enum class CryptoAssetType {
    ALGORITHM,
    CERTIFICATE,
    RELATED_CRYPTO_MATERIAL,
    PROTOCOL,
}

// ─── CycloneDX 1.6 CBOM JSON Models ────────────────────────────

/**
 * CBOM root document (CycloneDX 1.6 with crypto extension).
 */
data class CbomCycloneDx(
    @SerializedName("bomFormat")
    val bomFormat: String = "CycloneDX",
    @SerializedName("specVersion")
    val specVersion: String = "1.6",
    @SerializedName("serialNumber")
    val serialNumber: String = "urn:uuid:${java.util.UUID.randomUUID()}",
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("metadata")
    val metadata: CbomMetadata,
    @SerializedName("components")
    val components: List<CbomComponent>,
)

data class CbomMetadata(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("tools")
    val tools: List<CbomTool>,
    @SerializedName("properties")
    val properties: List<CbomProperty>? = null,
)

data class CbomTool(
    @SerializedName("vendor")
    val vendor: String = "OSV Plugin",
    @SerializedName("name")
    val name: String = "jetbrains-osv-plugin-cbom",
    @SerializedName("version")
    val version: String,
)

data class CbomProperty(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String,
)

/**
 * CBOM component representing a cryptographic primitive.
 *
 * CycloneDX 1.6 introduces `cryptoProperties` for assets in the
 * `cryptographic-asset` class. We represent each discovered usage as a component
 * with detailed metadata.
 */
data class CbomComponent(
    @SerializedName("type")
    val type: String = "cryptographic-asset",
    @SerializedName("name")
    val name: String,
    @SerializedName("bom-ref")
    val bomRef: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("cryptoProperties")
    val cryptoProperties: CryptoProperties,
    @SerializedName("source")
    val source: CbomSource? = null,
)

data class CryptoProperties(
    @SerializedName("assetType")
    val assetType: String, // algorithm | certificate | related-crypto-material | protocol
    @SerializedName("algorithmProperties")
    val algorithmProperties: AlgorithmProperties? = null,
    @SerializedName("relatedCryptoMaterialProperties")
    val relatedCryptoMaterialProperties: RelatedCryptoMaterialProperties? = null,
    @SerializedName("certificateProperties")
    val certificateProperties: CertificateProperties? = null,
    @SerializedName("protocolProperties")
    val protocolProperties: ProtocolProperties? = null,
)

data class AlgorithmProperties(
    @SerializedName("primitive")
    val primitive: String? = null,
    @SerializedName("parameterSetIdentifier")
    val parameterSetIdentifier: String? = null,
    @SerializedName("curve")
    val curve: String? = null,
    @SerializedName("padding")
    val padding: String? = null,
    @SerializedName("mode")
    val mode: String? = null,
    @SerializedName("keySize")
    val keySize: Int? = null,
)

data class RelatedCryptoMaterialProperties(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("size")
    val size: Int? = null,
    @SerializedName("format")
    val format: String? = null,
    @SerializedName("securedBy")
    val securedBy: String? = null,
)

data class CertificateProperties(
    @SerializedName("subjectName")
    val subjectName: String? = null,
    @SerializedName("issuerName")
    val issuerName: String? = null,
    @SerializedName("signatureAlgorithmRef")
    val signatureAlgorithmRef: String? = null,
    @SerializedName("subjectPublicKeyRef")
    val subjectPublicKeyRef: String? = null,
    @SerializedName("certificateFormat")
    val certificateFormat: String? = null,
)

data class ProtocolProperties(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("version")
    val version: String? = null,
    @SerializedName("cipherSuites")
    val cipherSuites: List<String>? = null,
)

data class CbomSource(
    @SerializedName("file")
    val file: String,
    @SerializedName("line")
    val line: Int,
)

/**
 * Export format selector.
 */
enum class CbomFormat {
    CYCLONEDX_CBOM_JSON,
}
