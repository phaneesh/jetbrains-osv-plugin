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
            // ─── CROSS-LANGUAGE CRYPTO DETECTORS ──────────────────
            // Python: cryptography.hazmat.primitives.ciphers.Cipher(...)
            Detector(
                Pattern.compile(
                    """
                    Cipher\s*\(\s*algorithms?\.(\w+)[^)]*
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Symmetric Cipher (Python)",
                    properties = mapOf("primitive" to "cipher", "language" to "Python"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Python: hashlib.sha256(...)
            Detector(
                Pattern.compile(
                    """
                    hashlib\.(\w+)\s*\(
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Hash / Digest (Python)",
                    properties = mapOf("primitive" to "hash", "language" to "Python"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Python: Fernet(...) from cryptography
            Detector(
                Pattern.compile(
                    """
                    from\s+cryptography\.(?:fernet|hazmat)\b[^\n]*
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "cryptography",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Cryptographic Library (Python)",
                    properties = mapOf("primitive" to "library", "language" to "Python"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Node.js: crypto.createCipheriv('aes-256-gcm', ...)
            Detector(
                Pattern.compile(
                    """
                    crypto\.(?:createCipheriv|createDecipheriv|createHash|createHmac)\s*\(\s*['"]([^'"]+)['"]
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Cipher / Hash (Node.js)",
                    properties = mapOf("primitive" to "cipher", "language" to "JavaScript"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Node.js: crypto.createHash('sha256')
            Detector(
                Pattern.compile(
                    """
                    crypto\.(?:createHash|createHmac|pbkdf2)\s*\(\s*['"]([^'"]+)['"]
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Hash / KDF (Node.js)",
                    properties = mapOf("primitive" to "hash", "language" to "JavaScript"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Node.js/jose: jose.SignJWT, jose.jwtVerify
            Detector(
                Pattern.compile(
                    """
                    jose\.(?:SignJWT|jwtVerify|EncryptJWT)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "JOSE/JWT",
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "JWT / JWS / JWE (Node.js)",
                    properties = mapOf("primitive" to "jwt", "language" to "JavaScript"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Go: crypto/aes, crypto/rsa import or use
            Detector(
                Pattern.compile(
                    """
                    crypto/(aes|rsa|ecdsa|tls|x509|sha256|sha512|md5)\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Go Crypto Package",
                    properties = mapOf("primitive" to "package", "language" to "Go"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Go: tls.Config{CipherSuites: [...]}
            Detector(
                Pattern.compile(
                    """
                    tls\.Config\b[^\n]*|CipherSuites\s*:\s*\[
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "TLS Config",
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Configuration (Go)",
                    properties = mapOf("primitive" to "tls", "language" to "Go"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Rust: sha2::Sha256, aes_gcm::Aes256Gcm
            Detector(
                Pattern.compile(
                    """
                    (sha2|sha3|aes|aes_gcm|chacha20|ring|openssl)::
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Rust Crypto Crate",
                    properties = mapOf("primitive" to "crate", "language" to "Rust"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Rust: rustls::ClientConfig
            Detector(
                Pattern.compile(
                    """
                    rustls::\w*Config\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.PROTOCOL,
            ) { file, line, _, _ ->
                CryptoAsset(
                    name = "rustls",
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Library (Rust)",
                    properties = mapOf("primitive" to "tls", "language" to "Rust"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // C/C++: OpenSSL EVP, AES, SHA usage
            Detector(
                Pattern.compile(
                    """
                    (EVP_|AES_|SHA256_|SHA512_|MD5_|HMAC_|RSA_|EC_KEY_|SSL_CTX_)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "OpenSSL Primitive (C/C++)",
                    properties = mapOf("primitive" to "openssl", "language" to "C/C++"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // C/C++: mbedtls / wolfSSL
            Detector(
                Pattern.compile(
                    """
                    (mbedtls_|wolfSSL_|wc_Aes|wc_Sha)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Embedded TLS Library (C/C++)",
                    properties = mapOf("primitive" to "library", "language" to "C/C++"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // PHP: openssl_encrypt, hash('sha256', ...)
            Detector(
                Pattern.compile(
                    """
                    openssl_(encrypt|decrypt|sign|verify|seal|open)\s*\(
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "openssl_${m.group(1)}",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "OpenSSL Operation (PHP)",
                    properties = mapOf("primitive" to "openssl", "language" to "PHP"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // PHP: sodium_crypto, libsodium
            Detector(
                Pattern.compile(
                    """
                    sodium_crypto_(\w+)\s*\(
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "sodium_${m.group(1)}",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Libsodium Operation (PHP)",
                    properties = mapOf("primitive" to "libsodium", "language" to "PHP"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Ruby: OpenSSL::Cipher, Digest::SHA256
            Detector(
                Pattern.compile(
                    """
                    OpenSSL::(Cipher|Digest|PKey|X509|SSL)
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.ALGORITHM,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = "OpenSSL::${m.group(1)}",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Ruby OpenSSL",
                    properties = mapOf("primitive" to "openssl", "language" to "Ruby"),
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
            // ─── CROSS-LANGUAGE CRYPTO LIBRARIES IN LOCKFILES ──
            // Python: cryptography, PyCryptodome, pycryptodomex
            Detector(
                Pattern.compile(
                    """
                    (cryptography|pycryptodome|pycryptodomex|m2crypto|paramiko)\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Python Crypto Library",
                    properties = mapOf("primitive" to "library", "language" to "Python"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Node.js: crypto-js, node-rsa, forge, jsonwebtoken, jose
            Detector(
                Pattern.compile(
                    """
                    (crypto-js|node-rsa|node-forge|jsonwebtoken|jose|bcrypt|argon2)\b
                    """.trimIndent().replace("\n", ""),
                    Pattern.CASE_INSENSITIVE,
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Node.js Crypto Library",
                    properties = mapOf("primitive" to "library", "language" to "JavaScript"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Go: golang.org/x/crypto, crypto/tls
            Detector(
                Pattern.compile(
                    """
                    (golang\.org/x/crypto|crypto/tls)\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Go Crypto Library",
                    properties = mapOf("primitive" to "library", "language" to "Go"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // Rust: sha2, aes, aes-gcm, chacha20poly1305, ring, rustls
            Detector(
                Pattern.compile(
                    """
                    (sha2|sha3|aes|aes-gcm|chacha20|chacha20poly1305|ring|rustls)\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Rust Crypto Crate",
                    properties = mapOf("primitive" to "library", "language" to "Rust"),
                    sourceFile = file,
                    lineNumber = line,
                )
            },
            // .NET: System.Security.Cryptography, BouncyCastle.Crypto
            Detector(
                Pattern.compile(
                    """
                    (System\.Security\.Cryptography|BouncyCastle\.Crypto)\b
                    """.trimIndent().replace("\n", ""),
                ),
                CryptoAssetType.RELATED_CRYPTO_MATERIAL,
            ) { file, line, _, m ->
                CryptoAsset(
                    name = m.group(1),
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = ".NET Crypto Library",
                    properties = mapOf("primitive" to "library", "language" to ".NET"),
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

    /** Returns true if this is a known source code file extension. */
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

    /** Returns true if this is a known configuration / manifest file. */
    private fun isConfigFile(name: String): Boolean =
        name.endsWith(".xml") || name.endsWith(".properties") ||
            name.endsWith(".gradle") || name.endsWith(".gradle.kts") ||
            name.endsWith(".yml") || name.endsWith(".yaml") ||
            name.endsWith(".json") || name.endsWith(".conf") ||
            name.endsWith(".cfg") || name.endsWith(".ini") ||
            name.endsWith(".toml") || name.endsWith(".lock") ||
            name.endsWith(".config") || name == "pom.xml"

    /** Returns true if this file should be scanned at all. */
    private fun isSourceOrConfigFile(name: String): Boolean = isSourceFile(name) || isConfigFile(name)

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
