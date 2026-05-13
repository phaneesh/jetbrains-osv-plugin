// Unit tests for CbomGenerator — no IntelliJ dependencies
package io.dyuti.osvplugin.cbom

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CbomGeneratorTest {
    private val generator = CbomGenerator(appName = "test-app", appVersion = "1.0.0")
    private val gson = Gson()

    @Test
    fun `generates valid CycloneDX CBOM JSON`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "AES/GCM/NoPadding",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Symmetric Cipher",
                    properties = mapOf("primitive" to "symmetric-cipher", "mode" to "GCM"),
                    sourceFile = "Crypto.java",
                    lineNumber = 42,
                ),
                CryptoAsset(
                    name = "TLSv1.3",
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Protocol",
                    properties = mapOf("primitive" to "tls", "family" to "TLS/SSL"),
                    sourceFile = "Config.java",
                    lineNumber = 10,
                ),
            )

        val json = generator.generate(assets)

        // Must contain our data
        assertTrue(json.contains("AES/GCM/NoPadding"))
        assertTrue(json.contains("TLSv1.3"))
        assertTrue(json.contains("test-app"))

        // Parse and verify structure via Gson
        val root = gson.fromJson(json, Map::class.java)

        assertEquals("CycloneDX", root["bomFormat"])
        assertEquals("1.6", root["specVersion"])
        assertNotNull(root["serialNumber"])
        assertNotNull(root["metadata"])

        val components = root["components"] as List<Map<String, Any>>
        assertEquals(2, components.size)

        // Verify both component types are present
        val types = components.map { it["type"] as String }
        assertTrue(types.contains("cryptographic-asset"))
    }

    @Test
    fun `includes cryptoProperties for algorithm asset`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "RSA",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Asymmetric Key Generation",
                    properties = mapOf("keySize" to "2048", "primitive" to "asymmetric"),
                    sourceFile = "Keys.java",
                    lineNumber = 5,
                ),
            )

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val component = components.first()
        val cryptoProps = component["cryptoProperties"] as Map<String, Any>

        assertEquals("algorithm", cryptoProps["assetType"])
        val algProps = cryptoProps["algorithmProperties"] as Map<String, Any>
        assertEquals(2048.0, algProps["keySize"]) // Gson parses as Double
        assertEquals("asymmetric", algProps["primitive"])
    }

    @Test
    fun `includes protocolProperties for TLS asset`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "TLSv1.2",
                    type = CryptoAssetType.PROTOCOL,
                    subtype = "TLS/SSL Configuration",
                    properties = mapOf("primitive" to "tls"),
                    sourceFile = "ServerConfig.java",
                    lineNumber = 20,
                ),
            )

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val component = components.first()
        val cryptoProps = component["cryptoProperties"] as Map<String, Any>

        assertEquals("protocol", cryptoProps["assetType"])
        val protoProps = cryptoProps["protocolProperties"] as Map<String, Any>
        assertEquals("1.2", protoProps["version"])
        assertEquals("tls", protoProps["type"])
    }

    @Test
    fun `handles empty asset list`() {
        val json = generator.generate(emptyList())

        val root = gson.fromJson(json, Map::class.java)
        assertEquals("CycloneDX", root["bomFormat"])
        assertEquals(1.0, root["version"]) // Gson parses as Double

        val components = root["components"] as List<*>
        assertTrue(components.isEmpty())
    }

    @Test
    fun `each component has a unique bom-ref`() {
        val assets =
            List(5) { i ->
                CryptoAsset(
                    name = "Asset-$i",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Test",
                    sourceFile = "Test.java",
                    lineNumber = i + 1,
                )
            }

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val refs = components.map { it["bom-ref"] as String }

        assertEquals(refs.toSet().size, refs.size) // all unique
        assertTrue(refs.all { it.startsWith("crypto-asset-") })
    }

    @Test
    fun `includes source location in component`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "HmacSHA256",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "MAC",
                    sourceFile = "AuthService.java",
                    lineNumber = 88,
                ),
            )

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val component = components.first()
        val source = component["source"] as Map<String, Any>

        assertEquals("AuthService.java", source["file"])
        assertEquals(88.0, source["line"]) // Gson parses as Double
    }

    @Test
    fun `certificate asset has certificateProperties`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "X.509",
                    type = CryptoAssetType.CERTIFICATE,
                    subtype = "Certificate Format",
                    sourceFile = "TrustStore.java",
                    lineNumber = 15,
                ),
            )

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val cryptoProps = components.first()["cryptoProperties"] as Map<String, Any>

        assertEquals("certificate", cryptoProps["assetType"])
        assertNotNull(cryptoProps["certificateProperties"])
    }

    @Test
    fun `related crypto material has relatedCryptoMaterialProperties`() {
        val assets =
            listOf(
                CryptoAsset(
                    name = "SecureRandom",
                    type = CryptoAssetType.RELATED_CRYPTO_MATERIAL,
                    subtype = "Random Number Generator",
                    properties = mapOf("primitive" to "rng"),
                    sourceFile = "Utils.java",
                    lineNumber = 30,
                ),
            )

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val components = root["components"] as List<Map<String, Any>>
        val cryptoProps = components.first()["cryptoProperties"] as Map<String, Any>

        assertEquals("related-crypto-material", cryptoProps["assetType"])
        val rcmProps = cryptoProps["relatedCryptoMaterialProperties"] as Map<String, Any>
        assertEquals("rng", rcmProps["format"])
    }

    @Test
    fun `metadata includes project name and asset count`() {
        val assets =
            List(3) { i ->
                CryptoAsset(
                    name = "Test-$i",
                    type = CryptoAssetType.ALGORITHM,
                    subtype = "Test",
                    sourceFile = "A.java",
                    lineNumber = i + 1,
                )
            }

        val json = generator.generate(assets)
        val root = gson.fromJson(json, Map::class.java)
        val metadata = root["metadata"] as Map<String, Any>
        val properties = metadata["properties"] as List<Map<String, Any>>

        val projectProp = properties.find { it["name"] == "projectName" }
        assertNotNull(projectProp)
        assertEquals("test-app", projectProp!!["value"])

        val countProp = properties.find { it["name"] == "assetCount" }
        assertNotNull(countProp)
        assertEquals("3", countProp!!["value"])
    }
}
