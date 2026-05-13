// QBOM Data Models — Post-Quantum Cryptography Bill of Materials
package io.dyuti.osvplugin.qbom

import com.google.gson.annotations.SerializedName

/**
 * A post-quantum cryptographic asset discovered via static analysis.
 */
data class QuantumAsset(
    val name: String,
    val type: QuantumAssetType,
    val subtype: String,
    val properties: Map<String, String> = emptyMap(),
    val sourceFile: String,
    val lineNumber: Int,
)

enum class QuantumAssetType {
    PQC_ALGORITHM, // ML-KEM, ML-DSA, SLH-DSA, Falcon, etc.
    PQC_LIBRARY, // BouncyCastle PQC, liboqs, OQS-OpenSSL
    PQC_PROTOCOL, // PQC TLS configurations
    PQC_STANDARD, // FIPS 203, 204, 205 references
    PQC_POLICY, // Post-quantum migration policy text
    HYBRID_KEY_EXCHANGE, // X25519Kyber768 etc
    QUANTUM_VULNERABLE, // RSA < 3072, ECC, DSA (vulnerable to Shor's)
}

// ─── CycloneDX-compatible QBOM JSON Models ─────────────────────

data class QbomCycloneDx(
    @SerializedName("bomFormat")
    val bomFormat: String = "CycloneDX",
    @SerializedName("specVersion")
    val specVersion: String = "1.6",
    @SerializedName("serialNumber")
    val serialNumber: String = "urn:uuid:${java.util.UUID.randomUUID()}",
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("metadata")
    val metadata: QbomMetadata,
    @SerializedName("components")
    val components: List<QbomComponent>,
)

data class QbomMetadata(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("tools")
    val tools: List<QbomTool>,
    @SerializedName("properties")
    val properties: List<QbomProperty>? = null,
)

data class QbomTool(
    @SerializedName("vendor")
    val vendor: String = "OSV Plugin",
    @SerializedName("name")
    val name: String = "jetbrains-osv-plugin-qbom",
    @SerializedName("version")
    val version: String,
)

data class QbomProperty(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String,
)

data class QbomComponent(
    @SerializedName("type")
    val type: String = "cryptographic-asset",
    @SerializedName("name")
    val name: String,
    @SerializedName("bom-ref")
    val bomRef: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("cryptoProperties")
    val cryptoProperties: QbomCryptoProperties,
    @SerializedName("source")
    val source: QbomSource? = null,
)

data class QbomCryptoProperties(
    @SerializedName("assetType")
    val assetType: String = "algorithm",
    @SerializedName("algorithmProperties")
    val algorithmProperties: QbomAlgorithmProperties? = null,
    @SerializedName("relatedCryptoMaterialProperties")
    val relatedCryptoMaterialProperties: QbomRelatedCryptoMaterialProperties? = null,
)

data class QbomAlgorithmProperties(
    @SerializedName("primitive")
    val primitive: String? = null,
    @SerializedName("parameterSetIdentifier")
    val parameterSetIdentifier: String? = null,
    @SerializedName("classification")
    val classification: String? = null,
)

data class QbomRelatedCryptoMaterialProperties(
    @SerializedName("type")
    val type: String? = null,
)

data class QbomSource(
    @SerializedName("file")
    val file: String,
    @SerializedName("line")
    val line: Int,
)

data class QbomDependency(
    @SerializedName("ref")
    val ref: String,
    @SerializedName("dependsOn")
    val dependsOn: List<String>? = null,
)
