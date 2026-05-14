package io.dyuti.osvplugin.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for CacheManager string caching support
 */
class CacheManagerTest {
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        cacheManager = CacheManager()
        cacheManager.invalidateAll()
    }

    @Test
    fun `getString returns null for missing key`() {
        assertNull(cacheManager.getString("nonexistent"))
    }

    @Test
    fun `getString returns cached value`() {
        cacheManager.cacheString("test-key", "test-value", 60000L)

        val result = cacheManager.getString("test-key")

        assertNotNull(result)
        assertEquals("test-value", result)
    }

    @Test
    fun `getString returns null after TTL expires`() {
        // Cache with 1ms TTL so it expires immediately
        cacheManager.cacheString("expiring", "value", 1L)

        // Brief sleep to ensure expiration
        Thread.sleep(10)

        val result = cacheManager.getString("expiring")
        assertNull(result)
    }

    @Test
    fun `cacheString overwrites existing value`() {
        cacheManager.cacheString("key", "first", 60000L)
        cacheManager.cacheString("key", "second", 60000L)

        val result = cacheManager.getString("key")
        assertEquals("second", result)
    }

    @Test
    fun `invalidateString removes cached string`() {
        cacheManager.cacheString("to-remove", "value", 60000L)
        assertNotNull(cacheManager.getString("to-remove"))

        cacheManager.invalidateString("to-remove")
        assertNull(cacheManager.getString("to-remove"))
    }

    @Test
    fun `invalidateAll clears string cache`() {
        cacheManager.cacheString("key1", "value1", 60000L)
        cacheManager.cacheString("key2", "value2", 60000L)

        cacheManager.invalidateAll()

        assertNull(cacheManager.getString("key1"))
        assertNull(cacheManager.getString("key2"))
    }

    @Test
    fun `getString handles empty string value`() {
        cacheManager.cacheString("empty", "", 60000L)

        val result = cacheManager.getString("empty")
        assertNotNull(result)
        assertEquals("", result)
    }

    @Test
    fun `getString returns value before TTL expires`() {
        cacheManager.cacheString("fresh", "value", 10000L)

        val result = cacheManager.getString("fresh")
        assertEquals("value", result)
    }
}
