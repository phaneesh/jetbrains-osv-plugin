// Unit tests for QuantumScanner
package io.dyuti.osvplugin.qbom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuantumScannerTest {
    private fun createScanner(): QuantumScanner = QuantumScanner(project = null)

    @Test
    fun `detects ML-KEM`() {
        val code = "KeyPairGenerator.getInstance(\"ML-KEM-768\");"
        val assets = createScanner().scanContent("Test.java", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.name.contains("ML-KEM") && it.type == QuantumAssetType.PQC_ALGORITHM })
    }

    @Test
    fun `detects ML-DSA`() {
        val code = "Signature.getInstance(\"ML-DSA-65\");"
        val assets = createScanner().scanContent("Test.java", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.name.contains("ML-DSA") && it.type == QuantumAssetType.PQC_ALGORITHM })
    }

    @Test
    fun `detects SLH-DSA`() {
        val code = "Signature sig = Signature.getInstance(\"SLH-DSA-SHA2-128S\");"
        val assets = createScanner().scanContent("Test.java", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.name.contains("SLH-DSA") })
    }

    @Test
    fun `detects hybrid key exchange`() {
        val code = "new X25519Kyber768();"
        val assets = createScanner().scanContent("Test.kt", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.name.contains("X25519Kyber") && it.type == QuantumAssetType.HYBRID_KEY_EXCHANGE })
    }

    @Test
    fun `detects quantum-vulnerable RSA`() {
        val code = "RSA(2048)"
        val assets = createScanner().scanContent("Test.java", code)
        assertEquals(1, assets.size)
        assertTrue(assets[0].name.contains("RSA"))
        assertEquals(QuantumAssetType.QUANTUM_VULNERABLE, assets[0].type)
        assertEquals("Shor's algorithm", assets[0].properties["threat"])
    }

    @Test
    fun `detects PQC library dependency`() {
        val code = "implementation 'org.bouncycastle:bcprov-pqc-jdk18on:1.78'"
        val assets = createScanner().scanContent("build.gradle", code)
        assertEquals(1, assets.size)
        assertEquals(QuantumAssetType.PQC_LIBRARY, assets[0].type)
    }

    @Test
    fun `detects NIST standard reference`() {
        val code = "Compliant with FIPS 203 and FIPS 204."
        val assets = createScanner().scanContent("README.md", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.type == QuantumAssetType.PQC_STANDARD })
    }

    @Test
    fun `returns empty for non-pqc code`() {
        val code = "System.out.println(\"Hello World\");"
        val assets = createScanner().scanContent("Hello.java", code)
        assertTrue(assets.isEmpty())
    }

    @Test
    fun `detects Falcon`() {
        val code = "new Falcon512();"
        val assets = createScanner().scanContent("Test.java", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.name.contains("Falcon") && it.type == QuantumAssetType.PQC_ALGORITHM })
    }

    @Test
    fun `detects post-quantum mention`() {
        val code = "Migrating to post-quantum cryptography."
        val assets = createScanner().scanContent("README.md", code)
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.type == QuantumAssetType.PQC_POLICY })
    }
}
