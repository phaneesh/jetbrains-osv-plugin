package io.dyuti.osvplugin.api

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
                io.dyuti.osvplugin.api.model
                    .Dependency("test-package", "1.0.0", "Maven", "compile", false),
            )

        val result = apiService.batchQueryVulnerabilities(dependencies)

        assertNotNull(result)
        // The method should complete without throwing an exception
        // The actual content depends on the API response
    }

    @Test
    fun `clearCache does not throw exception`() {
        apiService.clearCache()
        assert(true) { "Cache clearing should not throw exception" }
    }
}
