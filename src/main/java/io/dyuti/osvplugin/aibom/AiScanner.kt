// AI Asset Scanner — discovers AI/ML model dependencies and AI-generated code
package io.dyuti.osvplugin.aibom

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Pattern

/**
 * Discovers AI/ML assets in source files and project configurations.
 *
 * Scans for:
 *   - ML model usage (TensorFlow, PyTorch, ONNX, scikit-learn, HuggingFace Transformers)
 *   - Large Language Model APIs (OpenAI, Anthropic, Google Gemini, Azure OpenAI)
 *   - AI service SDKs and REST calls
 *   - Model configuration files
 *   - AI-generated code markers / co-pilot references
 *   - MLOps pipeline frameworks (MLflow, Kubeflow, SageMaker)
 */
class AiScanner(
    private val project: Project? = null,
) {
    fun scanProject(): List<AiAsset> {
        val assets = mutableListOf<AiAsset>()
        val proj = project ?: return assets
        val basePath = proj.basePath ?: return assets
        val rootDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return assets

        scanDirectory(rootDir, assets, mutableSetOf())
        return assets.sortedWith(compareBy({ it.sourceFile }, { it.lineNumber }, { it.name }))
    }

    fun scanFile(file: VirtualFile): List<AiAsset> {
        if (!file.isValid || file.isDirectory) return emptyList()
        val content =
            try {
                file.inputStream.bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                return emptyList()
            }
        return scanContent(file.name, content)
    }

    fun scanContent(
        fileName: String,
        content: String,
    ): List<AiAsset> {
        val assets = mutableListOf<AiAsset>()
        val lines = content.lines()

        val detectors =
            when {
                fileName.endsWith(".java") || fileName.endsWith(".kt") ||
                    fileName.endsWith(".groovy") || fileName.endsWith(".scala") ||
                    fileName.endsWith(".py") || fileName.endsWith(".js") ||
                    fileName.endsWith(".ts") || fileName.endsWith(".rs") -> sourceDetectors

                fileName.endsWith(".xml") || fileName.endsWith(".gradle") ||
                    fileName.endsWith(".gradle.kts") || fileName == "pom.xml" ||
                    fileName.endsWith(".properties") || fileName.endsWith(".yml") ||
                    fileName.endsWith(".yaml") || fileName.endsWith(".json") ||
                    fileName.endsWith(".toml") || fileName.endsWith(".cfg") ||
                    fileName.endsWith(".ini") || fileName.endsWith(".txt") -> configDetectors

                else -> sourceDetectors + configDetectors
            }

        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            detectors.forEach { detector ->
                val matcher = detector.pattern.matcher(line)
                while (matcher.find()) {
                    val asset = detector.extract(fileName, lineNum, line, matcher)
                    if (asset != null) assets.add(asset)
                }
            }
        }

        return assets
    }

    private data class Detector(
        val pattern: Pattern,
        val extract: (fileName: String, lineNum: Int, line: String, matcher: java.util.regex.Matcher) -> AiAsset?,
    )

    @Suppress("MaxLineLength")
    private val sourceDetectors =
        listOf(
            // OpenAI API
            Detector(
                Pattern.compile(
                    """
                    (openai|ChatCompletion|Completion|Embedding)\s*\(|\.build\(\)|openai\.api_key|OpenAI\.apiKey|openAIClient
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "OpenAI API",
                    type = AiAssetType.LLM_API,
                    subtype = "Large Language Model Service",
                    properties = mapOf("provider" to "OpenAI", "model" to "GPT"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Anthropic Claude
            Detector(
                Pattern.compile(
                    """
                    (anthropic|claude|AnthropicClient|claude-\d|messages\.create|completion\s*\(.*model\s*[=:]\s*["']claude)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "Anthropic Claude",
                    type = AiAssetType.LLM_API,
                    subtype = "Large Language Model Service",
                    properties = mapOf("provider" to "Anthropic", "model" to "Claude"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Google Gemini / Vertex AI
            Detector(
                Pattern.compile(
                    """
                    (vertexai|gemini|GenerativeModel|gemini-pro|gemini-\d|google\.generativeai|google\.cloud\.aiplatform)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "Google Gemini / Vertex AI",
                    type = AiAssetType.LLM_API,
                    subtype = "Large Language Model Service",
                    properties = mapOf("provider" to "Google", "model" to "Gemini"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Azure OpenAI
            Detector(
                Pattern.compile(
                    """
                    (azure\.openai|AzureOpenAI|openai\.azure|\.azure\.com/openai)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "Azure OpenAI",
                    type = AiAssetType.LLM_API,
                    subtype = "Large Language Model Service",
                    properties = mapOf("provider" to "Microsoft Azure", "model" to "GPT"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // HuggingFace Transformers
            Detector(
                Pattern.compile(
                    """
                    (transformers|AutoModel|from_pretrained|HuggingFace|huggingface|pipeline\s*\(\s*["'](text-generation|summarization|translation|question-answering))
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "HuggingFace Transformers",
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "Pre-trained Model Library",
                    properties = mapOf("provider" to "HuggingFace", "type" to "transformer"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // TensorFlow
            Detector(
                Pattern.compile(
                    """
                    (tensorflow|tf\.|keras|tf\.keras|TensorFlow|SavedModel|load_model)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "TensorFlow / Keras",
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "Deep Learning Framework",
                    properties = mapOf("provider" to "Google", "type" to "neural-network"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // PyTorch
            Detector(
                Pattern.compile(
                    """
                    (torch\.|pytorch|nn\.Module|torchvision|torchaudio|load_state_dict)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "PyTorch",
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "Deep Learning Framework",
                    properties = mapOf("provider" to "Meta", "type" to "neural-network"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // ONNX
            Detector(
                Pattern.compile(
                    """
                    (onnx|onnxruntime|InferenceSession|ort\.)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "ONNX Runtime",
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "Model Inference Runtime",
                    properties = mapOf("provider" to "Microsoft", "type" to "onnx"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // scikit-learn
            Detector(
                Pattern.compile(
                    """
                    (sklearn|scikit-learn|scikit_learn|\.fit\(|\.predict\()
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "scikit-learn",
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "Machine Learning Library",
                    properties = mapOf("provider" to "scikit-learn", "type" to "classic-ml"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // LangChain
            Detector(
                Pattern.compile(
                    """
                    (langchain|LangChain|ChatPromptTemplate|LLMChain|RetrievalQA)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "LangChain",
                    type = AiAssetType.AI_ORCHESTRATION,
                    subtype = "LLM Application Framework",
                    properties = mapOf("provider" to "LangChain", "type" to "orchestration"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Vector DBs for RAG
            Detector(
                Pattern.compile(
                    """
                    (chromadb|chroma|pinecone|weaviate|qdrant|milvus|faiss|pgvector)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = m.group(1).replaceFirstChar { it.uppercase() },
                    type = AiAssetType.VECTOR_DATABASE,
                    subtype = "Vector Database for RAG",
                    properties = mapOf("purpose" to "retrieval-augmented-generation"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // AI-generated code markers
            Detector(
                Pattern.compile(
                    """
                    (Generated\s*(by|with)\s*AI|AI-generated|@ai-generated|@generated|copilot|GitHub\s*Copilot|tabnine|codeium)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = "AI-Generated Code Marker",
                    type = AiAssetType.AI_GENERATED_CODE,
                    subtype = "AI-Assisted Code Attribution",
                    properties = mapOf("marker" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // MLOps / Model serving
            Detector(
                Pattern.compile(
                    """
                    (mlflow|kubeflow|sagemaker|bentoml|triton|torchserve|tensorflow\s*serving|model_server)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = m.group(1).replaceFirstChar { it.uppercase() },
                    type = AiAssetType.MLOPS,
                    subtype = "MLOps / Model Serving Platform",
                    properties = mapOf("category" to "mlops"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    @Suppress("MaxLineLength")
    private val configDetectors =
        listOf(
            // Model dependencies in config
            Detector(
                Pattern.compile(
                    """
                    (tensorflow|torch|transformers|onnxruntime|scikit-learn|keras|langchain|openai|anthropic|google-generativeai)\s*[=:]\s*["']?[\d.]+["']?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = m.group(1),
                    type = AiAssetType.ML_FRAMEWORK,
                    subtype = "AI/ML Dependency",
                    properties = mapOf("type" to "library", "detectedFrom" to "config"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Model config files
            Detector(
                Pattern.compile(
                    """
                    (model\.config|config\.json|\.gguf|\.safetensors|\.onnx|\.pt|\.pth|\.pb|\.h5|\.tflite)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                AiAsset(
                    name = m.group(1),
                    type = AiAssetType.MODEL_ARTIFACT,
                    subtype = "Serialized Model Artifact",
                    properties = mapOf("format" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    private fun scanDirectory(
        dir: VirtualFile,
        assets: MutableList<AiAsset>,
        visited: MutableSet<String>,
    ) {
        if (!dir.isValid || !dir.isDirectory) return
        if (!visited.add(dir.path)) return

        for (child in dir.children) {
            when {
                child.isDirectory -> {
                    val name = child.name
                    if (name !in EXCLUDED_DIRS) scanDirectory(child, assets, visited)
                }

                child.isValid && isSourceOrConfigFile(child.name) -> {
                    assets.addAll(scanFile(child))
                }
            }
        }
    }

    private fun isSourceOrConfigFile(name: String): Boolean =
        name.endsWith(".java") || name.endsWith(".kt") ||
            name.endsWith(".groovy") || name.endsWith(".scala") ||
            name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".ts") ||
            name.endsWith(".rs") || name.endsWith(".xml") || name.endsWith(".properties") ||
            name.endsWith(".gradle") || name.endsWith(".gradle.kts") ||
            name.endsWith(".yml") || name.endsWith(".yaml") ||
            name.endsWith(".json") || name.endsWith(".toml") ||
            name.endsWith(".cfg") || name.endsWith(".ini") ||
            name.endsWith(".txt") || name == "pom.xml"

    companion object {
        private val EXCLUDED_DIRS =
            setOf(
                ".git",
                ".svn",
                ".hg",
                ".bzr",
                "node_modules",
                "vendor",
                "bower_components",
                "build",
                "target",
                "out",
                "dist",
                ".gradle",
                "gradle",
                ".idea",
                ".vscode",
                ".eclipse",
                "__pycache__",
                ".pytest_cache",
                ".mypy_cache",
                "bin",
                "obj",
                "Debug",
                "Release",
            )
    }
}
