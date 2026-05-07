// Standalone privacy hash tests — no IntelliJ dependencies
package io.dyuti.osvplugin.privacy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrivacyHasherTest {
    private lateinit var hasher: PrivacyHasher

    @BeforeEach
    fun setup() {
        hasher = PrivacyHasher(salt = "test-salt-12345")
        hasher.enabled = true
    }

    @Test
    fun `obfuscate is deterministic for same input`() {
        val hash1 = hasher.obfuscate("org.example:lib", "Maven")
        val hash2 = hasher.obfuscate("org.example:lib", "Maven")
        assertEquals(hash1, hash2, "Same input should produce same hash")
    }

    @Test
    fun `obfuscate produces different hashes for different inputs`() {
        val hash1 = hasher.obfuscate("org.example:lib1", "Maven")
        val hash2 = hasher.obfuscate("org.example:lib2", "Maven")
        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes")
    }

    @Test
    fun `obfuscated names are 16 characters`() {
        val hash = hasher.obfuscate("anything", "Maven")
        assertEquals(16, hash.length, "Hash should be truncated to 16 chars")
    }

    @Test
    fun `resolveOriginalName round-trips correctly`() {
        val original = "com.company:secret-lib"
        val obfuscated = hasher.obfuscate(original, "Maven")
        val resolved = hasher.resolveOriginalName(obfuscated)
        assertEquals(original, resolved, "Resolved name should match original")
    }

    @Test
    fun `resolveOriginalName returns null for unknown hash`() {
        hasher.clearMappings()
        val result = hasher.resolveOriginalName("0000000000000000")
        assertNull(result, "Unknown hash should return null")
    }

    @Test
    fun `when disabled obfuscate returns original`() {
        hasher.enabled = false
        val result = hasher.obfuscate("org.example:lib", "Maven")
        assertEquals("org.example:lib", result)
    }

    @Test
    fun `when salt is null obfuscate returns original`() {
        hasher.setSalt(null)
        val result = hasher.obfuscate("org.example:lib", "Maven")
        assertEquals("org.example:lib", result)
    }

    @Test
    fun `batch obfuscate handles multiple packages`() {
        val packages =
            listOf(
                "pkg1" to "Maven",
                "pkg2" to "npm",
                "pkg3" to "PyPI",
            )
        val hashes = hasher.obfuscateAll(packages)
        assertEquals(3, hashes.size)
        assertTrue(hashes.all { it.length == 16 })
        assertEquals(hashes.toSet().size, hashes.size, "All hashes should be unique")
    }

    @Test
    fun `batch resolve round-trips`() {
        val packages = listOf("a" to "Maven", "b" to "npm")
        val hashes = hasher.obfuscateAll(packages)
        val resolved = hasher.resolveAll(hashes)
        assertEquals(listOf("a", "b"), resolved)
    }

    @Test
    fun `different ecosystems produce different hashes for same name`() {
        val maven = hasher.obfuscate("same-name", "Maven")
        val npm = hasher.obfuscate("same-name", "npm")
        assertNotEquals(maven, npm, "Same name different ecosystem should hash differently")
    }

    @Test
    fun `salt rotation invalidates old mappings`() {
        val obfuscated = hasher.obfuscate("test-pkg", "Maven")
        hasher.setSalt("new-salt-67890")
        val resolved = hasher.resolveOriginalName(obfuscated)
        assertNull(resolved, "Old hash should be invalid after salt rotation")
    }

    @Test
    fun `clear mappings empties cache`() {
        val obfuscated = hasher.obfuscate("test-pkg", "Maven")
        assertNotNull(hasher.resolveOriginalName(obfuscated))
        hasher.clearMappings()
        assertNull(hasher.resolveOriginalName(obfuscated))
    }

    @Test
    fun `resolveAll falls back to obfuscated name when unknown`() {
        val result = hasher.resolveAll(listOf("unknownhash12345"))
        assertEquals(listOf("unknownhash12345"), result)
    }

    @Test
    fun `hash contains only hex characters`() {
        val hash = hasher.obfuscate("pkg", "Maven")
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' }, "Hash should be hex only")
    }
}
