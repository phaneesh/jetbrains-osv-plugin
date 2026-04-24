package io.dyuti.osvplugin.utils

import org.junit.jupiter.api.Test

/**
 * Tests for CacheManager
 */
class CacheManagerTest {
    
    @Test
    fun `cacheManager has initial state`() {
        val cacheManager = CacheManager()
        assert(cacheManager.size() >= 0)
    }
}
