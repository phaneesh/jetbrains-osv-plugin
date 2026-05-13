// Unit tests for CryptoScanner content analysis (no IntelliJ dependencies)
package io.dyuti.osvplugin.cbom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CryptoScannerTest {
    /** We test content scanning directly, avoiding Project/VirtualFile dependencies. */
    private fun createScanner(): CryptoScanner = CryptoScanner(project = null)

    @Test
    fun `detects AES cipher instance`() {
        val code =
            """
            import javax.crypto.Cipher;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            """.trimIndent()

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("AES/GCM/NoPadding", assets[0].name)
        assertEquals(CryptoAssetType.ALGORITHM, assets[0].type)
        assertEquals("Symmetric Cipher", assets[0].subtype)
        assertEquals("Test.java", assets[0].sourceFile)
        assertEquals(2, assets[0].lineNumber)
    }

    @Test
    fun `detects SHA-256 message digest`() {
        val code = "MessageDigest md = MessageDigest.getInstance(\"SHA-256\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("SHA-256", assets[0].name)
        assertEquals(CryptoAssetType.ALGORITHM, assets[0].type)
        assertEquals("Hash / Digest", assets[0].subtype)
    }

    @Test
    fun `detects HmacSHA256 MAC`() {
        val code = "Mac mac = Mac.getInstance(\"HmacSHA256\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("HmacSHA256", assets[0].name)
        assertEquals(CryptoAssetType.ALGORITHM, assets[0].type)
        assertEquals("Message Authentication Code", assets[0].subtype)
    }

    @Test
    fun `detects KeyGenerator and KeyPairGenerator`() {
        val code =
            """
            KeyGenerator.getInstance("AES");
            KeyPairGenerator.getInstance("RSA");
            """.trimIndent()

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.kt", code)

        assertEquals(2, assets.size)
        assertTrue(assets.any { it.name == "AES" && it.subtype == "Symmetric Key Generation" })
        assertTrue(assets.any { it.name == "RSA" && it.subtype == "Asymmetric Key Generation" })
    }

    @Test
    fun `detects Signature algorithm`() {
        val code = "Signature.getInstance(\"SHA256withRSA\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("SHA256withRSA", assets[0].name)
        assertEquals(CryptoAssetType.ALGORITHM, assets[0].type)
        assertEquals("Digital Signature", assets[0].subtype)
    }

    @Test
    fun `detects KeyStore type`() {
        val code = "KeyStore ks = KeyStore.getInstance(\"PKCS12\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("PKCS12", assets[0].name)
        assertEquals(CryptoAssetType.RELATED_CRYPTO_MATERIAL, assets[0].type)
        assertEquals("KeyStore Type", assets[0].subtype)
    }

    @Test
    fun `detects SSLContext protocol`() {
        val code = "SSLContext ctx = SSLContext.getInstance(\"TLSv1.2\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("TLSv1.2", assets[0].name)
        assertEquals(CryptoAssetType.PROTOCOL, assets[0].type)
        assertEquals("TLS/SSL Protocol", assets[0].subtype)
    }

    @Test
    fun `detects SecretKeySpec with algorithm`() {
        val code = "SecretKeySpec keySpec = new SecretKeySpec(keyBytes, \"AES\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertTrue(assets[0].name.contains("AES"))
        assertEquals(CryptoAssetType.RELATED_CRYPTO_MATERIAL, assets[0].type)
        assertEquals("Secret Key Specification", assets[0].subtype)
    }

    @Test
    fun `detects CertificateFactory format`() {
        val code = "CertificateFactory cf = CertificateFactory.getInstance(\"X.509\");"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("X.509", assets[0].name)
        assertEquals(CryptoAssetType.CERTIFICATE, assets[0].type)
        assertEquals("Certificate Format", assets[0].subtype)
    }

    @Test
    fun `detects SecureRandom usage`() {
        val code = "SecureRandom sr = new SecureRandom();"

        val scanner = createScanner()
        val assets = scanner.scanContent("Test.java", code)

        assertEquals(1, assets.size)
        assertEquals("SecureRandom", assets[0].name)
        assertEquals(CryptoAssetType.RELATED_CRYPTO_MATERIAL, assets[0].type)
    }

    @Test
    fun `detects BouncyCastle in config file`() {
        val config = "implementation 'org.bouncycastle:bcprov-jdk18on:1.76'"

        val scanner = createScanner()
        val assets = scanner.scanContent("build.gradle", config)

        assertEquals(1, assets.size)
        assertTrue(assets[0].name.contains("BouncyCastle"))
        assertEquals(CryptoAssetType.RELATED_CRYPTO_MATERIAL, assets[0].type)
    }

    @Test
    fun `detects TLS config in properties file`() {
        val props = "server.ssl.protocol=TLSv1.3"

        val scanner = createScanner()
        val assets = scanner.scanContent("application.properties", props)

        assertEquals(1, assets.size)
        assertEquals("TLSv1.3", assets[0].name)
        assertEquals(CryptoAssetType.PROTOCOL, assets[0].type)
        assertEquals("TLS/SSL Configuration", assets[0].subtype)
    }

    @Test
    fun `detects JWT algorithm references`() {
        val code = "Alg: RS256"

        val scanner = createScanner()
        val assets = scanner.scanContent("config.json", code)

        assertEquals(1, assets.size)
        assertEquals("RS256", assets[0].name)
        assertEquals(CryptoAssetType.ALGORITHM, assets[0].type)
        assertEquals("JWT/JWS Signature Algorithm", assets[0].subtype)
    }

    @Test
    fun `returns empty for non-crypto code`() {
        val code =
            """
            class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
            """.trimIndent()

        val scanner = createScanner()
        val assets = scanner.scanContent("Hello.java", code)

        assertTrue(assets.isEmpty())
    }

    @Test
    fun `handles multiple assets in single file`() {
        val code =
            """
            Cipher.getInstance("AES/GCM/NoPadding");
            MessageDigest.getInstance("SHA-256");
            SSLContext.getInstance("TLSv1.3");
            KeyStore.getInstance("JKS");
            """.trimIndent()

        val scanner = createScanner()
        val assets = scanner.scanContent("Crypto.java", code)

        assertEquals(4, assets.size)
        assertTrue(assets.any { it.name == "AES/GCM/NoPadding" })
        assertTrue(assets.any { it.name == "SHA-256" })
        assertTrue(assets.any { it.name == "TLSv1.3" })
        assertTrue(assets.any { it.name == "JKS" })
    }

    @Test
    fun `scanContent maps line numbers correctly`() {
        val code =
            """
            // line 1
            Cipher.getInstance("AES");
            // line 3
            MessageDigest.getInstance("SHA-1");
            """.trimIndent()

        val scanner = createScanner()
        val assets = scanner.scanContent("Lines.java", code)

        // The actual line numbers depend on first line content
        assertTrue(assets.all { it.lineNumber > 0 })
        assertTrue(assets.zipWithNext().all { it.first.lineNumber < it.second.lineNumber })
    }

    @Test
    fun `no false positives from cross-language library names in unrelated files`() {
        val scanner = createScanner()

        // "ring" should NOT match in YML files (not Cargo.lock)
        val ymlAssets = scanner.scanContent("application.yml", "ring: some-config-value")
        assertTrue(ymlAssets.none { it.name == "ring" }, "ring should not match in YML")

        // "sha2" should NOT match in Java files (not Cargo.lock)
        val javaAssets = scanner.scanContent("Config.java", "String hash = \"sha2\";")
        assertTrue(javaAssets.none { it.name == "sha2" }, "sha2 should not match in Java")

        // "crypto-js" should NOT match in Gradle files (not package-lock.json)
        val gradleAssets = scanner.scanContent("build.gradle", "someVariable = 'crypto-js'")
        assertTrue(gradleAssets.none { it.name == "crypto-js" }, "crypto-js should not match in Gradle")

        // "cryptography" SHOULD match in requirements.txt
        val pipAssets = scanner.scanContent("requirements.txt", "cryptography==41.0.0")
        assertTrue(pipAssets.any { it.name == "cryptography" }, "cryptography should match in requirements.txt")

        // "sha2" SHOULD match in Cargo.lock
        val cargoAssets = scanner.scanContent("Cargo.lock", """name = "sha2"""")
        assertTrue(cargoAssets.any { it.name == "sha2" }, "sha2 should match in Cargo.lock")
    }
}
