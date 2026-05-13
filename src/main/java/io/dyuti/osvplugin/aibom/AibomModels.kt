// AIBOM Data Models — AI Bill of Materials
package io.dyuti.osvplugin.aibom

import com.google.gson.annotations.SerializedName

data class AiAsset(
    val name: String,
    val type: AiAssetType,
    val subtype: String,
    val properties: Map<String, String> = emptyMap(),
    val sourceFile: String,
    val lineNumber: Int,
)

enum class AiAssetType {
    LLM_API, // OpenAI, Anthropic, Google Gemini, Azure OpenAI
    ML_FRAMEWORK, // TensorFlow, PyTorch, ONNX, scikit-learn
    AI_ORCHESTRATION, // LangChain, LlamaIndex
    VECTOR_DATABASE, // Chroma, Pinecone, Weaviate, FAISS
    MODEL_ARTIFACT, // .gguf, .safetensors, .onnx, .pt
    MLOPS, // MLflow, Kubeflow, SageMaker
    AI_GENERATED_CODE, // Copilot, Tabnine, Codeium markers
}

data class AibomCycloneDx(
    @SerializedName("bomFormat")
    val bomFormat: String = "CycloneDX",
    @SerializedName("specVersion")
    val specVersion: String = "1.6",
    @SerializedName("serialNumber")
    val serialNumber: String = "urn:uuid:${java.util.UUID.randomUUID()}",
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("metadata")
    val metadata: AibomMetadata,
    @SerializedName("components")
    val components: List<AibomComponent>,
)

data class AibomMetadata(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("tools")
    val tools: List<AibomTool>,
    @SerializedName("properties")
    val properties: List<AibomProperty>? = null,
)

data class AibomTool(
    @SerializedName("vendor")
    val vendor: String = "OSV Plugin",
    @SerializedName("name")
    val name: String = "jetbrains-osv-plugin-aibom",
    @SerializedName("version")
    val version: String,
)

data class AibomProperty(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String,
)

data class AibomComponent(
    @SerializedName("type")
    val type: String = "library",
    @SerializedName("name")
    val name: String,
    @SerializedName("bom-ref")
    val bomRef: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("properties")
    val properties: List<AibomComponentProperty>? = null,
    @SerializedName("source")
    val source: AibomSource? = null,
)

data class AibomComponentProperty(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String,
)

data class AibomSource(
    @SerializedName("file")
    val file: String,
    @SerializedName("line")
    val line: Int,
)
