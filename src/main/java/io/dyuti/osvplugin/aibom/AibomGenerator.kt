// AIBOM Generator — AI Bill of Materials
package io.dyuti.osvplugin.aibom

import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates AIBOM documents from discovered AI/ML assets.
 */
class AibomGenerator(
    private val appName: String = "project",
    private val appVersion: String = "1.0.0",
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isoTimestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    fun generate(assets: List<AiAsset>): String = generateCycloneDx(assets)

    private fun generateCycloneDx(assets: List<AiAsset>): String {
        val now = isoTimestamp.format(Instant.now())
        val components = assets.mapIndexed { index, asset -> toComponent(asset, index) }

        val doc =
            AibomCycloneDx(
                metadata =
                    AibomMetadata(
                        timestamp = now,
                        tools = listOf(AibomTool(version = appVersion)),
                        properties =
                            listOf(
                                AibomProperty("projectName", appName),
                                AibomProperty("assetCount", components.size.toString()),
                                AibomProperty("llmApis", assets.count { it.type == AiAssetType.LLM_API }.toString()),
                                AibomProperty("mlFrameworks", assets.count { it.type == AiAssetType.ML_FRAMEWORK }.toString()),
                                AibomProperty("vectorDbs", assets.count { it.type == AiAssetType.VECTOR_DATABASE }.toString()),
                                AibomProperty("aiGeneratedCode", assets.count { it.type == AiAssetType.AI_GENERATED_CODE }.toString()),
                            ),
                    ),
                components = components,
            )

        return gson.toJson(doc)
    }

    private fun toComponent(
        asset: AiAsset,
        index: Int,
    ): AibomComponent {
        val bomRef = "ai-asset-${index + 1}"

        val cdxType =
            when (asset.type) {
                AiAssetType.LLM_API -> "library"
                AiAssetType.ML_FRAMEWORK -> "library"
                AiAssetType.AI_ORCHESTRATION -> "library"
                AiAssetType.VECTOR_DATABASE -> "library"
                AiAssetType.MODEL_ARTIFACT -> "data"
                AiAssetType.MLOPS -> "library"
                AiAssetType.AI_GENERATED_CODE -> "file"
            }

        return AibomComponent(
            type = cdxType,
            name = asset.name,
            bomRef = bomRef,
            description = "${asset.subtype} discovered in ${asset.sourceFile}:${asset.lineNumber}",
            properties = asset.properties.map { (k, v) -> AibomComponentProperty(k, v) },
            source = AibomSource(file = asset.sourceFile, line = asset.lineNumber),
        )
    }
}
