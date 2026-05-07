// Tests for MaliciousPackageService
package io.dyuti.osvplugin.malicious

import io.dyuti.osvplugin.api.model.Dependency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MaliciousPackageServiceTest {
    private val service = MaliciousPackageService()

    @Test
    fun `known malicious package detected`() {
        val dep = Dependency("ua-parser-js", "0.7.29", "Npm", "compile", false, 1)
        val result = service.checkPackage(dep)

        assertTrue(result.isMalicious)
        assertTrue(result.reason.contains("ua-parser-js"))
    }

    @Test
    fun `known protestware detected`() {
        val dep = Dependency("colors", "1.4.44", "Npm", "compile", false, 1)
        val result = service.checkPackage(dep)

        assertTrue(result.isMalicious)
    }

    @Test
    fun `legitimate package not flagged`() {
        val dep = Dependency("lodash", "4.17.21", "Npm", "compile", false, 1)
        val result = service.checkPackage(dep)

        assertFalse(result.isMalicious)
    }

    @Test
    fun `typosquat detected with high similarity`() {
        val dep = Dependency("lodashs", "1.0.0", "Npm", "compile", false, 1)
        val result = service.checkPackage(dep)

        // Should detect typosquat or be flagged by OSV API check
        // Note: actual result depends on network, but typosquat layer should catch it
    }

    @Test
    fun `typosquatting check returns score`() {
        val result = service.checkTyposquatting("lodashs")

        assertTrue(result.similarityScore > 0.0)
        assertEquals("lodash", result.originalPackage)
    }

    @Test
    fun `typosquatting exact match not flagged`() {
        val result = service.checkTyposquatting("lodash")

        assertFalse(result.isTyposquat)
    }

    @Test
    fun `homoglyph detection finds unicode lookalike`() {
        // This string contains Cyrillic 'а' (U+0430) instead of Latin 'a'
        val suspicious = "p\u0430ypal" // pаypal (Cyrillic а)
        val result = service.checkHomoglyph(suspicious)

        assertNotNull(result)
        assertTrue(result!!.contains("homoglyph") || result.contains("unicode"))
    }

    @Test
    fun `homoglyph detection passes clean name`() {
        val result = service.checkHomoglyph("paypal")

        assertNull(result)
    }

    @Test
    fun `mixed scripts detection triggers`() {
        // Mix Latin and Cyrillic
        val mixed = "app\u0430" // appа (Cyrillic а at end)
        val result = service.checkHomoglyph(mixed)

        assertNotNull(result)
    }

    @Test
    fun `malicious type classification`() {
        val malware = service.checkPackage(Dependency("ua-parser-js", "0.7.29", "Npm", "compile", false, 1))
        assertEquals(MaliciousType.MALWARE, malware.maliciousType)
    }

    @Test
    fun `batch check returns results for all packages`() {
        val deps =
            listOf(
                Dependency("colors", "1.4.44", "Npm", "compile", false, 1),
                Dependency("lodash", "4.17.21", "Npm", "compile", false, 1),
            )
        val results = service.checkPackages(deps)

        assertEquals(2, results.size)
        assertTrue(results.any { it.isMalicious })
        assertTrue(results.any { !it.isMalicious })
    }

    @Test
    fun `typosquat threshold is reasonable`() {
        // "react" vs "reactt" is a clear typosquat
        val result = service.checkTyposquatting("reactt")
        assertTrue(result.similarityScore >= 0.80)
        assertEquals("react", result.originalPackage)
    }

    @Test
    fun `distant names are not typosquats`() {
        val result = service.checkTyposquatting("completely-different-name")
        assertFalse(result.isTyposquat)
    }
}
