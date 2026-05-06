package io.dyuti.osvplugin.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for OsVApiService
 */
class OsVApiServiceTest {
    private val apiService: OsVApiService = OsVApiService()

    @Test
    fun `queryVulnerabilities returns empty list for unknown package`() {
        val result = apiService.queryVulnerabilities("test-package", "Maven", "1.0.0")

        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `batchQueryVulnerabilities runs without exception`() {
        // Test that batchQueryVulnerabilities runs without throwing an exception
        // In a real test environment, the OSV API might not be accessible
        val dependencies =
            listOf(
                Dependency("test-package", "1.0.0", "Maven", "compile", false),
            )

        val result = apiService.batchQueryVulnerabilities(dependencies)

        assertNotNull(result)
        // The method should complete without throwing an exception
        // The actual content depends on the API response
    }

    @Test
    fun `batchQueryVulnerabilities returns empty results for empty dependencies`() {
        val result = apiService.batchQueryVulnerabilities(emptyList())

        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `clearCache does not throw exception`() {
        apiService.clearCache()
        assert(true) { "Cache clearing should not throw exception" }
    }

    // -- CVSS Severity Parsing Tests --

    @Test
    fun `mapCvssToSeverity returns CRITICAL for score 9_0 to 10_0`() {
        assertEquals(OsVSeverity.CRITICAL, apiService.mapCvssToSeverity(9.0))
        assertEquals(OsVSeverity.CRITICAL, apiService.mapCvssToSeverity(9.8))
        assertEquals(OsVSeverity.CRITICAL, apiService.mapCvssToSeverity(10.0))
    }

    @Test
    fun `mapCvssToSeverity returns HIGH for score 7_0 to 8_9`() {
        assertEquals(OsVSeverity.HIGH, apiService.mapCvssToSeverity(7.0))
        assertEquals(OsVSeverity.HIGH, apiService.mapCvssToSeverity(8.0))
        assertEquals(OsVSeverity.HIGH, apiService.mapCvssToSeverity(8.9))
    }

    @Test
    fun `mapCvssToSeverity returns MEDIUM for score 4_0 to 6_9`() {
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(4.0))
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(5.5))
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(6.9))
    }

    @Test
    fun `mapCvssToSeverity returns LOW for score 0_1 to 3_9`() {
        assertEquals(OsVSeverity.LOW, apiService.mapCvssToSeverity(0.1))
        assertEquals(OsVSeverity.LOW, apiService.mapCvssToSeverity(2.0))
        assertEquals(OsVSeverity.LOW, apiService.mapCvssToSeverity(3.9))
    }

    @Test
    fun `mapCvssToSeverity returns MEDIUM for null and zero`() {
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(null))
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(0.0))
        assertEquals(OsVSeverity.MEDIUM, apiService.mapCvssToSeverity(-1.0))
    }

    @Test
    fun `parseCvssSeverity prefers CVSS_V3 over CVSS_V2`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val v3 = JsonObject()
        v3.add("type", JsonPrimitive("CVSS_V3"))
        v3.add("score", JsonPrimitive("9.8"))
        severityArray.add(v3)

        val v2 = JsonObject()
        v2.add("type", JsonPrimitive("CVSS_V2"))
        v2.add("score", JsonPrimitive("10.0"))
        severityArray.add(v2)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.CRITICAL, severity)
        assertEquals(9.8, score)
    }

    @Test
    fun `parseCvssSeverity falls back to CVSS_V2 when no V3`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val v2 = JsonObject()
        v2.add("type", JsonPrimitive("CVSS_V2"))
        v2.add("score", JsonPrimitive("7.5"))
        severityArray.add(v2)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.HIGH, severity)
        assertEquals(7.5, score)
    }

    @Test
    fun `parseCvssSeverity returns MEDIUM and null when severity array missing`() {
        val vuln = JsonObject()

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.MEDIUM, severity)
        assertNull(score)
    }

    @Test
    fun `parseCvssSeverity returns MEDIUM and null for empty severity array`() {
        val vuln = JsonObject()
        vuln.add("severity", JsonArray())

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.MEDIUM, severity)
        assertNull(score)
    }

    @Test
    fun `parseCvssSeverity handles malformed score gracefully`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("CVSS_V3"))
        entry.add("score", JsonPrimitive("not-a-number"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.MEDIUM, severity)
        assertNull(score)
    }

    @Test
    fun `parseCvssSeverity ignores unknown severity types`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("UNKNOWN_TYPE"))
        entry.add("score", JsonPrimitive("9.9"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.MEDIUM, severity)
        assertNull(score)
    }

    @Test
    fun `parseCvssSeverity parses LOW score correctly`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("CVSS_V3"))
        entry.add("score", JsonPrimitive("2.5"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.LOW, severity)
        assertEquals(2.5, score)
    }

    @Test
    fun `parseCvssSeverity parses boundary score exactly 9_0 as CRITICAL`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("CVSS_V3"))
        entry.add("score", JsonPrimitive("9.0"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.CRITICAL, severity)
        assertEquals(9.0, score)
    }

    @Test
    fun `parseCvssSeverity parses boundary score exactly 7_0 as HIGH`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("CVSS_V3"))
        entry.add("score", JsonPrimitive("7.0"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.HIGH, severity)
        assertEquals(7.0, score)
    }

    @Test
    fun `parseCvssSeverity parses boundary score exactly 4_0 as MEDIUM`() {
        val vuln = JsonObject()
        val severityArray = JsonArray()

        val entry = JsonObject()
        entry.add("type", JsonPrimitive("CVSS_V3"))
        entry.add("score", JsonPrimitive("4.0"))
        severityArray.add(entry)

        vuln.add("severity", severityArray)

        val (severity, score) = apiService.parseCvssSeverity(vuln)

        assertEquals(OsVSeverity.MEDIUM, severity)
        assertEquals(4.0, score)
    }
}
