// SBOM Generator — CycloneDX 1.5 + SPDX 2.3
package io.dyuti.osvplugin.sbom

import com.google.gson.GsonBuilder
import io.dyuti.osvplugin.api.model.Dependency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates SBOM (Software Bill of Materials) documents from a list of dependencies.
 *
 * Supports CycloneDX JSON, SPDX JSON, and SPDX Tag-Value formats.
 * No IntelliJ dependencies — fully unit-testable.
 */
class SbomGenerator(
    private val appName: String = "project",
    private val appVersion: String = "1.0.0",
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isoTimestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    /**
     * Generate an SBOM string in the requested format.
     */
    fun generate(
        dependencies: List<Dependency>,
        format: SbomFormat,
    ): String =
        when (format) {
            SbomFormat.CYCLONEDX_JSON -> generateCycloneDx(dependencies)
            SbomFormat.SPDX_JSON -> generateSpdxJson(dependencies)
            SbomFormat.SPDX_TAGVALUE -> generateSpdxTagValue(dependencies)
        }

    // ─── CycloneDX ───────────────────────────────────────────────

    private fun generateCycloneDx(deps: List<Dependency>): String {
        val bom =
            CycloneDxBom(
                metadata =
                    CycloneDxMetadata(
                        timestamp = isoTimestamp.format(Instant.now()),
                        tools = listOf(CycloneDxTool()),
                    ),
                components =
                    deps.map { dep ->
                        CycloneDxComponent(
                            name = dep.name,
                            version = dep.version,
                            purl = toPurl(dep),
                            scope = dep.scope.lowercase(),
                        )
                    },
            )
        return gson.toJson(bom)
    }

    // ─── SPDX JSON ───────────────────────────────────────────────

    private fun generateSpdxJson(deps: List<Dependency>): String {
        val now = isoTimestamp.format(Instant.now())
        val pkgs =
            deps.mapIndexed { index, dep ->
                SpdxPackage(
                    spdxId = "SPDXRef-Package-$index",
                    name = dep.name,
                    externalRefs =
                        listOf(
                            SpdxExternalRef(
                                referenceLocator = toPurl(dep),
                            ),
                        ),
                )
            }
        val relationships =
            pkgs.map { pkg ->
                SpdxRelationship(
                    spdxElementId = "SPDXRef-DOCUMENT",
                    relatedSpdxElement = pkg.spdxId,
                    relationshipType = "DESCRIBES",
                )
            }
        val doc =
            SpdxDocument(
                name = appName,
                documentNamespace = "https://osv-plugin.io/sbom/$appName-$appVersion",
                creationInfo =
                    SpdxCreationInfo(
                        created = now,
                        creators = listOf("Tool: jetbrains-osv-plugin-1.1.0"),
                    ),
                packages = pkgs,
                relationships = relationships,
            )
        return gson.toJson(doc)
    }

    // ─── SPDX Tag-Value ──────────────────────────────────────────

    private fun generateSpdxTagValue(deps: List<Dependency>): String {
        val now = isoTimestamp.format(Instant.now())
        val sb = StringBuilder()

        sb.appendLine("SPDXVersion: SPDX-2.3")
        sb.appendLine("DataLicense: CC0-1.0")
        sb.appendLine("SPDXID: SPDXRef-DOCUMENT")
        sb.appendLine("DocumentName: $appName")
        sb.appendLine("DocumentNamespace: https://osv-plugin.io/sbom/$appName-$appVersion")
        sb.appendLine("Creator: Tool: jetbrains-osv-plugin-1.1.0")
        sb.appendLine("Created: $now")
        sb.appendLine()

        deps.forEachIndexed { index, dep ->
            val spdxId = "SPDXRef-Package-$index"
            sb.appendLine("PackageName: ${dep.name}")
            sb.appendLine("SPDXID: $spdxId")
            sb.appendLine("PackageVersion: ${dep.version}")
            sb.appendLine("PackageDownloadLocation: NOASSERTION")
            sb.appendLine("FilesAnalyzed: false")
            sb.appendLine("PackageLicenseConcluded: NOASSERTION")
            sb.appendLine("PackageLicenseDeclared: NOASSERTION")
            sb.appendLine("PackageCopyrightText: NOASSERTION")
            sb.appendLine("ExternalRef: PACKAGE-MANAGER purl ${toPurl(dep)}")
            sb.appendLine()
        }

        deps.forEachIndexed { index, _ ->
            sb.appendLine("Relationship: SPDXRef-DOCUMENT DESCRIBES SPDXRef-Package-$index")
        }

        return sb.toString()
    }

    // ─── PURL construction ───────────────────────────────────────

    /**
     * Construct a Package URL (purl) from a dependency.
     *
     * Examples:
     *   Maven:   pkg:maven/org.springframework/spring-core@5.3.21
     *   npm:     pkg:npm/lodash@4.17.21
     *   PyPI:    pkg:pypi/requests@2.28.1
     *   Gradle:  same as Maven (Gradle uses Maven repos)
     */
    fun toPurl(dep: Dependency): String {
        val type =
            when (dep.ecosystem.lowercase()) {
                "maven", "gradle" -> "maven"
                "npm", "nodejs" -> "npm"
                "pypi", "pip" -> "pypi"
                "nuget" -> "nuget"
                "golang" -> "golang"
                "cargo", "rust" -> "cargo"
                else -> dep.ecosystem.lowercase()
            }

        val namespaceName =
            when (type) {
                "maven" -> {
                    // Split group:artifact format if present
                    val parts = dep.name.split(":", limit = 2)
                    if (parts.size == 2) {
                        "${parts[0]}/${parts[1]}"
                    } else {
                        dep.name
                    }
                }

                else -> {
                    dep.name
                }
            }

        return "pkg:$type/$namespaceName@${dep.version}"
    }
}
