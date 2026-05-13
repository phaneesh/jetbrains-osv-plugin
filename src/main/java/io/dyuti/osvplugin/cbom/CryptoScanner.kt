// Crypto Asset Scanner — discovers cryptographic assets via static analysis
package io.dyuti.osvplugin.cbom

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Pattern

/**
 * Discovers cryptographic assets in source files and build configurations.
 *
 * Scans Java/Kotlin/Groovy source for JCA/JCE API calls, BouncyCastle usage,
 * TLS/SSL configurations, key specifications, and certificate handling.
 * Also detects crypto-related dependencies (BouncyCastle, OpenSSL bindings, etc.).
 */
class CryptoScanner(
    private val project: Project? = null,
) {
    /**
     * Scan the entire project for cryptographic assets.
     */
    fun scanProject(): List<CryptoAsset> {
        val assets = mutableListOf<CryptoAsset>()
        val proj = project ?: return assets
        val basePath = proj.basePath ?: return assets
        val rootDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return assets

        scanDirectory(rootDir, assets, mutableSetOf())
        return assets.sortedWith(compareBy({ it.sourceFile }, { it.lineNumber }, { it.name }))
    }

    /**
     * Scan a specific source file for cryptographic patterns.
     */
    fun scanFile(file: VirtualFile): List<CryptoAsset> {
        if (!file.isValid || file.isDirectory) return emptyList()
        val content =
            try {
                file.inputStream.bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                return emptyList()
            }
        return scanContent(file.name, content)
    }

    /**
     * Scan raw text content for cryptographic patterns.
     */
    fun scanContent(
        fileName: String,
        content: String,
    ): List<CryptoAsset> {
        val assets = mutableListOf<CryptoAsset>()
        val lines = content.lines()

        // Choose detector set based on file type
        val detectors =
            when {
                fileName.endsWith(".java") || fileName.endsWith(".kt") ||
                    fileName.endsWith(".groovy") || fileName.endsWith(".scala") -> {
                    sourceDetectors
                }

                fileName.endsWith(".xml") || fileName.endsWith(".gradle") ||
                    fileName.endsWith(".gradle.kts") || fileName == "pom.xml" ||
                    fileName.endsWith(".properties") || fileName.endsWith(".yml") ||
                    fileName.endsWith(".yaml") || fileName.endsWith(".json") ||
                    fileName.endsWith(".conf") || fileName.endsWith(".cfg") -> {
                    configDetectors
                }

                else -> {
                    sourceDetectors + configDetectors
                }
            }

        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            detectors.forEach { detector ->
                val matcher = detector.pattern.matcher(line)
                while (matcher.find()) {
                    val asset = detector.extract(fileName, lineNum, line, matcher)
                    if (asset != null) {
                        assets.add(asset)
                    }
                }
            }
        }

        return assets
    }

    // ─── Detectors ─────────────────────────────────────────────────

    private data class Detector(
        val pattern: Pattern,
        val assetType: CryptoAssetType,
        val extract: (fileName: String, lineNum: Int, line: String, matcher: java.util.regex.Matcher) -> CryptoAsset?,
    )

    @Suppress("MaxLineLength")
    private val sourceDetectors =
        listOf(
            // Cipher.getInstance("AES/GCM/NoPadding")
            Detector(
                Pattern.compile(
                    """
                    Cipher\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Symmetric Cipher",
                    properties = parseCipherSpec(m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // MessageDigest.getInstance("SHA-256")
            Detector(
                Pattern.compile(
                    """
                    MessageDigest\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Hash / Digest",
                    properties = mapOf("primitive" to "hash"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Mac.getInstance("HmacSHA256")
            Detector(
                Pattern.compile(
                    """
                    Mac\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Message Authentication Code",
                    properties = mapOf("primitive" to "mac"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // KeyGenerator.getInstance("AES") or KeyPairGenerator.getInstance("RSA")
            Detector(
                Pattern.compile(
                    """
                    (KeyGenerator|KeyPairGenerator)\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                val genType = if (m.group(1) == "KeyGenerator") "Symmetric" else "Asymmetric"
                CryptoAsset(
                    name = m.group(2),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "$genType Key Generation",
                    properties = mapOf("primitive" to "key-generation", "generatorType" to genType.lowercase()),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Signature.getInstance("SHA256withRSA")
            Detector(
                Pattern.compile(
                    """
                    Signature\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Digital Signature",
                    properties = mapOf("primitive" to "signature"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // KeyStore.getInstance("JKS") / KeyStore.getInstance("PKCS12")
            Detector(
                Pattern.compile(
                    """
                    KeyStore\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "KeyStore Type",
                    properties = mapOf("primitive" to "keystore"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // SSLContext.getInstance("TLSv1.2")
            Detector(
                Pattern.compile(
                    """
                    SSLContext\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Protocol",
                    properties = mapOf("primitive" to "tls", "family" to "TLS/SSL"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // new SecretKeySpec(bytes, "AES")
            Detector(
                Pattern.compile(
                    """
                    SecretKeySpec\s*\([^,]+,\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "SecretKeySpec(${m.group(1)})",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Secret Key Specification",
                    properties = mapOf("primitive" to "secret-key", "algorithm" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // IvParameterSpec(...) — IV usage
            Detector(
                Pattern.compile(
                    """
                    IvParameterSpec
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "IvParameterSpec",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Initialization Vector",
                    properties = mapOf("primitive" to "iv"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // BouncyCastle provider usage
            Detector(
                Pattern.compile(
                    """
                    (BouncyCastleProvider|"BC"|"BouncyCastle")
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "BouncyCastle Provider",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "JCE Provider",
                    properties = mapOf("primitive" to "provider", "name" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // CertificateFactory.getInstance("X.509")
            Detector(
                Pattern.compile(
                    """
                    CertificateFactory\.getInstance\s*\(\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.CERTIFICATE,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.CERTIFICATE,
                    subtype = "Certificate Format",
                    properties = mapOf("primitive" to "certificate"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // new SecureRandom() — randomness source
            Detector(
                Pattern.compile(
                    """
                    new\s+SecureRandom\s*\(\s*\)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "SecureRandom",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Random Number Generator",
                    properties = mapOf("primitive" to "rng"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // RSA / EC / DSA key size patterns
            Detector(
                Pattern.compile(
                    """
                    (RSA|EC|DSA)\s*\(\s*(\d+)(?:\s*,\s*[^)]+)?\s*\)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Asymmetric Algorithm",
                    properties =
                        mapOf(
                            "primitive" to "asymmetric",
                            "keySize" to m.group(2),
                        ),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Key length literals: 128, 192, 256 with AES-like context
            Detector(
                Pattern.compile(
                    """
                    (?:AES|key|KEY)\w*[^\n]*?\b(128|192|256)\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "Key Size ${m.group(1)}",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Key Parameter",
                    properties = mapOf("keySize" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    @Suppress("MaxLineLength")
    private val configDetectors =
        listOf(
            // TLS protocol in config (application.properties, XML, etc.)
            Detector(
                Pattern.compile(
                    """
                    (ssl|tls)[\w\-.]*\s*[:=]\s*["']?(TLSv?[\d.]+)["']?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(2),
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Configuration",
                    properties = mapOf("primitive" to "tls", "family" to "TLS/SSL"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Cipher suites in config
            Detector(
                Pattern.compile(
                    """
                    (cipher[_\-]?suites?)\s*[:=]\s*["']([^"']+)["']
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name =
                        m
                            .group(2)
                            .split(",")
                            .first()
                            .trim(),
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "Cipher Suite",
                    properties =
                        mapOf(
                            "primitive" to "cipher-suite",
                            "suites" to m.group(2),
                        ),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // BouncyCastle dependency in Gradle/Maven
            Detector(
                Pattern.compile(
                    """
                    (bcprov|bcpkix|bcmail|bcutil|bcpg)[\w\-.]*
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "BouncyCastle ${m.group(1)}",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Cryptographic Library Dependency",
                    properties = mapOf("primitive" to "library"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // OpenSSL / LibreSSL references
            Detector(
                Pattern.compile(
                    """
                    (openssl|libressl)[\w\-.]*
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Cryptographic Library Dependency",
                    properties = mapOf("primitive" to "library"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // AWS KMS / Azure KeyVault / GCP Cloud KMS references
            Detector(
                Pattern.compile(
                    """
                    (aws[_\-]?kms|azure[_\-]?key[_\-]?vault|gcp[_\-]?cloud[_\-]?kms|hashi[_\-]?corp[_\-]?vault)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1).replace("_", "-"),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "External Key Management Service",
                    properties = mapOf("primitive" to "kms"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // JWT / JWS / JWE algorithm references
            Detector(
                Pattern.compile(
                    """
                    (HS256|HS384|HS512|RS256|RS384|RS512|ES256|ES384|ES512|EdDSA|PS256|PS384|PS512)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "JWT/JWS Signature Algorithm",
                    properties = mapOf("primitive" to "jwt-alg"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    // ─── Helpers ───────────────────────────────────────────────────

    private fun scanDirectory(
        dir: VirtualFile,
        assets: MutableList<CryptoAsset>,
        visited: MutableSet<String>,
    ) {
        if (!dir.isValid || !dir.isDirectory) return
        if (!visited.add(dir.path)) return

        for (child in dir.children) {
            when {
                child.isDirectory -> {
                    // Skip common non-source directories
                    val name = child.name
                    if (name !in EXCLUDED_DIRS) {
                        scanDirectory(child, assets, visited)
                    }
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
            name.endsWith(".xml") || name.endsWith(".properties") ||
            name.endsWith(".gradle") || name.endsWith(".gradle.kts") ||
            name.endsWith(".yml") || name.endsWith(".yaml") ||
            name.endsWith(".json") || name.endsWith(".conf") ||
            name.endsWith(".cfg") || name == "pom.xml"

    private fun parseCipherSpec(spec: String): Map<String, String> {
        val parts = spec.split("/")
        return when (parts.size) {
            1 -> mapOf("algorithm" to parts[0])
            2 -> mapOf("algorithm" to parts[0], "mode" to parts[1])
            3 -> mapOf("algorithm" to parts[0], "mode" to parts[1], "padding" to parts[2])
            else -> mapOf("algorithm" to spec)
        }
    }

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
