// Quantum Asset Scanner — discovers post-quantum cryptography assets
package io.dyuti.osvplugin.qbom

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Pattern

/**
 * Discovers post-quantum cryptographic assets and quantum-vulnerable code.
 *
 * Scans for:
 *   - PQC algorithm usage (Kyber, Dilithium, Falcon, SPHINCS+, ML-KEM, ML-DSA)
 *   - Hybrid post-quantum TLS / key exchange
 *   - PQC library dependencies (BouncyCastle PQC, liboqs, OQS-OpenSSL)
 *   - Classical algorithms that are quantum-vulnerable (RSA < 3072, ECC, DSA)
 *   - NIST PQC standard references (FIPS 203, 204, 205)
 */
class QuantumScanner(
    private val project: Project? = null,
) {
    fun scanProject(): List<QuantumAsset> {
        val assets = mutableListOf<QuantumAsset>()
        val proj = project ?: return assets
        val basePath = proj.basePath ?: return assets
        val rootDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return assets

        scanDirectory(rootDir, assets, mutableSetOf())
        return assets.sortedWith(compareBy({ it.sourceFile }, { it.lineNumber }, { it.name }))
    }

    fun scanFile(file: VirtualFile): List<QuantumAsset> {
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
    ): List<QuantumAsset> {
        val assets = mutableListOf<QuantumAsset>()
        val lines = content.lines()

        val detectors =
            when {
                isSourceFile(fileName) -> sourceDetectors
                isConfigFile(fileName) -> configDetectors
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
        val extract: (fileName: String, lineNum: Int, line: String, matcher: java.util.regex.Matcher) -> QuantumAsset?,
    )

    @Suppress("MaxLineLength")
    private val sourceDetectors =
        listOf(
            // ML-KEM / Kyber
            Detector(
                Pattern.compile(
                    """
                    (ML-KEM|Kyber|KYBER)[-_]?(512|768|1024)?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1) + (m.group(2)?.let { "-$it" } ?: ""),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "NIST FIPS 203 Key Encapsulation Mechanism",
                    properties = mapOf("standard" to "FIPS 203", "category" to "kem", "parameterSet" to (m.group(2) ?: "768")),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // ML-DSA / Dilithium
            Detector(
                Pattern.compile(
                    """
                    (ML-DSA|Dilithium|DILITHIUM)[-_]?(2|3|5)?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1) + (m.group(2)?.let { "-$it" } ?: ""),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "NIST FIPS 204 Digital Signature",
                    properties = mapOf("standard" to "FIPS 204", "category" to "signature", "parameterSet" to (m.group(2) ?: "3")),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // SLH-DSA / SPHINCS+
            Detector(
                Pattern.compile(
                    """
                    (SLH-DSA|SPHINCS?\+|SPHINCS)[-_]?(128|192|256)?[a-z]*
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1) + (m.group(2)?.let { "-$it" } ?: ""),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "NIST FIPS 205 Hash-based Signature",
                    properties = mapOf("standard" to "FIPS 205", "category" to "signature", "parameterSet" to (m.group(2) ?: "128")),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Falcon
            Detector(
                Pattern.compile(
                    """
                    (Falcon|FALCON)[-_]?(512|1024)?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1) + (m.group(2)?.let { "-$it" } ?: ""),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "NIST Round 3 Finalist Lattice Signature",
                    properties = mapOf("standard" to "NIST-PQC-R3", "category" to "signature", "parameterSet" to (m.group(2) ?: "512")),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // BIKE / HQC / Classic McEliece (NIST Round 4)
            Detector(
                Pattern.compile(
                    """
                    (BIKE|HQC|Classic\s*McElience|McElience)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1).replace("\\s+", ""),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "NIST Round 4 KEM Candidate",
                    properties = mapOf("standard" to "NIST-PQC-R4", "category" to "kem"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Hybrid PQC key exchange (X25519Kyber768)
            Detector(
                Pattern.compile(
                    """
                    (X25519Kyber768|SecP256r1MLKEM768|X25519MLKEM768|P256Kyber768)
                    """.trimIndent().replace("\n", ""),
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.HYBRID_KEY_EXCHANGE,
                    subtype = "Hybrid Post-Quantum Key Exchange",
                    properties = mapOf("hybrid" to "true", "classic" to "ECDH", "pqc" to "ML-KEM"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Quantum-vulnerable: RSA < 3072
            Detector(
                Pattern.compile(
                    """
                    RSA\s*\(\s*(512|768|1024|1536|2048)\s*\)
                    """.trimIndent().replace("\n", ""),
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = "RSA-${m.group(1)}",
                    type = QuantumAssetType.QUANTUM_VULNERABLE,
                    subtype = "Quantum-Vulnerable Asymmetric Encryption",
                    properties = mapOf("threat" to "Shor's algorithm", "migration" to "ML-KEM/ML-DSA", "keySize" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Quantum-vulnerable: ECC curves
            Detector(
                Pattern.compile(
                    """
                    (ECGenParameterSpec|ECParameterSpec|getByName)\s*\(\s*["'](secp(192|224|256)r1|prime192v1|prime256v1|NIST\s*P-192|NIST\s*P-224|NIST\s*P-256)["']
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(3),
                    type = QuantumAssetType.QUANTUM_VULNERABLE,
                    subtype = "Quantum-Vulnerable Elliptic Curve",
                    properties = mapOf("threat" to "Shor's algorithm", "migration" to "ML-DSA"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // General PQC references
            Detector(
                Pattern.compile(
                    """
                    (post[-_\s]?quantum|pqc|quantum[-_\s]?safe|quantum[-_\s]?resistant)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = "Post-Quantum Reference",
                    type = QuantumAssetType.PQC_POLICY,
                    subtype = "Post-Quantum Cryptography Policy/Mention",
                    properties = mapOf("context" to m.group(1)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    @Suppress("MaxLineLength")
    private val configDetectors =
        listOf(
            // PQC library deps
            Detector(
                Pattern.compile(
                    """
                    (bcprov-pqc|bcpqc|oqs|liboqs|oqs-openssl|pqcrypto|pqc-jca|pqc-clean|pqclean)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "Post-Quantum Cryptography Library",
                    properties = mapOf("type" to "library"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // PQC TLS config
            Detector(
                Pattern.compile(
                    """
                    (tls|PQTLS|pq-tls|quantum[-_\s]?tls)[\w\-.]*\s*[:=]\s*["']?(true|1|yes|enabled)["']?
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = "Quantum-Safe TLS",
                    type = QuantumAssetType.PQC_PROTOCOL,
                    subtype = "Post-Quantum TLS Configuration",
                    properties = mapOf("enabled" to m.group(2)),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // NIST standard references
            Detector(
                Pattern.compile(
                    """
                    (FIPS\s*203|FIPS\s*204|FIPS\s*205|NIST\.IR\.8413[\w\-]*)
                    """.trimIndent().replace("\n", ""),
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1).replace("\\s+", " "),
                    type = QuantumAssetType.PQC_STANDARD,
                    subtype = "NIST PQC Standard Reference",
                    properties = mapOf("type" to "standard"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // ─── CROSS-LANGUAGE PQC DETECTORS ───────────────────
            // Python: liboqs / oqs-python binding
            Detector(
                Pattern.compile(
                    """
                    import\s+oqs\b|from\s+oqs\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, _ ->
                QuantumAsset(
                    name = "liboqs (Python)",
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "PQC Library",
                    properties = mapOf("language" to "Python", "library" to "liboqs"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Python: pycryptodome / cryptography PQC awareness
            Detector(
                Pattern.compile(
                    """
                    cryptography\.hazmat\.primitives\.asymmetric\.(kyber|dilithium|falcon)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.PQC_ALGORITHM,
                    subtype = "PQC Algorithm (Python)",
                    properties = mapOf("language" to "Python"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Node.js: oqs-openssl, post-quantum-crypto
            Detector(
                Pattern.compile(
                    """
                    (oqs-openssl|post-quantum-crypto|mlkem|mldsa)\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "PQC Library (Node.js)",
                    properties = mapOf("language" to "JavaScript"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Rust: pqc_core, pqc_kyber, pqc_dilithium, pqc_falcon
            Detector(
                Pattern.compile(
                    """
                    (pqc_kyber|pqc_dilithium|pqc_falcon|pqc_sphincs|pqc_core)\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "PQC Crate (Rust)",
                    properties = mapOf("language" to "Rust"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Go: cloudflare/circl (PQC primitives)
            Detector(
                Pattern.compile(
                    """
                    cloudflare/circl\b
                    """.trimIndent().replace("\n", ""),
                ),
            ) { file, line, _, _ ->
                QuantumAsset(
                    name = "cloudflare/circl",
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "PQC Go Library",
                    properties = mapOf("language" to "Go"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // C/C++: oqs-provider, liboqs, pqclean
            Detector(
                Pattern.compile(
                    """
                    (oqs-provider|liboqs|pqclean|pqcrypto)\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.PQC_LIBRARY,
                    subtype = "PQC Library (C/C++)",
                    properties = mapOf("language" to "C/C++"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Generic: X25519Kyber768Draft00 (hybrid KEM)
            Detector(
                Pattern.compile(
                    """
                    (X25519Kyber768|X25519Kyber768Draft00|Kyber768|ML-KEM-\d+)
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
            ) { file, line, _, m ->
                QuantumAsset(
                    name = m.group(1),
                    type = QuantumAssetType.HYBRID_KEY_EXCHANGE,
                    subtype = "Hybrid Post-Quantum Key Exchange",
                    properties = mapOf("type" to "hybrid-kem"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
        )

    private fun scanDirectory(
        dir: VirtualFile,
        assets: MutableList<QuantumAsset>,
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

    private fun isSourceFile(name: String): Boolean =
        name.endsWith(".java") || name.endsWith(".kt") ||
            name.endsWith(".groovy") || name.endsWith(".scala") ||
            name.endsWith(".py") || name.endsWith(".js") ||
            name.endsWith(".ts") || name.endsWith(".go") ||
            name.endsWith(".rs") || name.endsWith(".php") ||
            name.endsWith(".rb") || name.endsWith(".r") ||
            name.endsWith(".dart") || name.endsWith(".cs") ||
            name.endsWith(".swift") || name.endsWith(".cpp") ||
            name.endsWith(".c") || name.endsWith(".h") ||
            name.endsWith(".hpp") || name.endsWith(".ex") ||
            name.endsWith(".exs")

    private fun isConfigFile(name: String): Boolean =
        name.endsWith(".xml") || name.endsWith(".properties") ||
            name.endsWith(".gradle") || name.endsWith(".gradle.kts") ||
            name.endsWith(".yml") || name.endsWith(".yaml") ||
            name.endsWith(".json") || name.endsWith(".conf") ||
            name.endsWith(".cfg") || name.endsWith(".ini") ||
            name.endsWith(".toml") || name.endsWith(".lock") ||
            name.endsWith(".config") || name == "pom.xml"

    private fun isSourceOrConfigFile(name: String): Boolean = isSourceFile(name) || isConfigFile(name)

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
