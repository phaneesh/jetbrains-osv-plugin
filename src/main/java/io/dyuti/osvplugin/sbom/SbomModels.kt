// SBOM Data Models — CycloneDX 1.5 + SPDX 2.3 compatible
package io.dyuti.osvplugin.sbom

import com.google.gson.annotations.SerializedName

/**
 * A CycloneDX BOM (Bill of Materials) document.
 */
data class CycloneDxBom(
    @SerializedName("bomFormat")
    val bomFormat: String = "CycloneDX",
    @SerializedName("specVersion")
    val specVersion: String = "1.5",
    @SerializedName("serialNumber")
    val serialNumber: String = "urn:uuid:${java.util.UUID.randomUUID()}",
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("metadata")
    val metadata: CycloneDxMetadata,
    @SerializedName("components")
    val components: List<CycloneDxComponent>,
)

data class CycloneDxMetadata(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("tools")
    val tools: List<CycloneDxTool>,
)

data class CycloneDxTool(
    @SerializedName("vendor")
    val vendor: String = "OSV Plugin",
    @SerializedName("name")
    val name: String = "jetbrains-osv-plugin",
    @SerializedName("version")
    val version: String = "1.1.0",
)

data class CycloneDxComponent(
    @SerializedName("type")
    val type: String = "library",
    @SerializedName("name")
    val name: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("purl")
    val purl: String,
    @SerializedName("scope")
    val scope: String? = null,
)

/**
 * SPDX 2.3 Document (simplified — TV and JSON formats supported).
 */
data class SpdxDocument(
    @SerializedName("spdxVersion")
    val spdxVersion: String = "SPDX-2.3",
    @SerializedName("SPDXID")
    val spdxId: String = "SPDXRef-DOCUMENT",
    @SerializedName("name")
    val name: String,
    @SerializedName("documentNamespace")
    val documentNamespace: String,
    @SerializedName("creationInfo")
    val creationInfo: SpdxCreationInfo,
    @SerializedName("packages")
    val packages: List<SpdxPackage>,
    @SerializedName("relationships")
    val relationships: List<SpdxRelationship>,
)

data class SpdxCreationInfo(
    @SerializedName("created")
    val created: String,
    @SerializedName("creators")
    val creators: List<String>,
)

data class SpdxPackage(
    @SerializedName("SPDXID")
    val spdxId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("downloadLocation")
    val downloadLocation: String = "NOASSERTION",
    @SerializedName("filesAnalyzed")
    val filesAnalyzed: Boolean = false,
    @SerializedName("verificationCode")
    val verificationCode: Map<String, Any>? = null,
    @SerializedName("licenseConcluded")
    val licenseConcluded: String = "NOASSERTION",
    @SerializedName("licenseDeclared")
    val licenseDeclared: String = "NOASSERTION",
    @SerializedName("copyrightText")
    val copyrightText: String = "NOASSERTION",
    @SerializedName("externalRefs")
    val externalRefs: List<SpdxExternalRef>? = null,
)

data class SpdxRelationship(
    @SerializedName("spdxElementId")
    val spdxElementId: String,
    @SerializedName("relatedSpdxElement")
    val relatedSpdxElement: String,
    @SerializedName("relationshipType")
    val relationshipType: String,
)

data class SpdxExternalRef(
    @SerializedName("referenceCategory")
    val referenceCategory: String = "PACKAGE-MANAGER",
    @SerializedName("referenceType")
    val referenceType: String = "purl",
    @SerializedName("referenceLocator")
    val referenceLocator: String,
)

/**
 * Which SBOM format to emit.
 */
enum class SbomFormat {
    CYCLONEDX_JSON,
    SPDX_JSON,
    SPDX_TAGVALUE,
}
