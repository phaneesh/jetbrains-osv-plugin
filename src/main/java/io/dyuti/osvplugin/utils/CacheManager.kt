package io.dyuti.osvplugin.utils

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * Cache manager for OSV API responses
 */
@State(
    name = "OsVCacheManager",
    storages = [Storage("osv-cache.xml")],
)
class CacheManager : PersistentStateComponent<CacheManager.CacheState> {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val stringCache = mutableMapOf<String, StringCacheEntry>()

    data class CacheState(
        var cacheEntries: Map<String, Long> = emptyMap(),
    )

    data class CacheEntry(
        val vulnerabilities: List<Vulnerability>,
        val timestamp: Long,
        val ttl: Long = 1L, // 1 hour in milliseconds - using 1L for simplicity
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttl * 60 * 60 * 1000
    }

    data class StringCacheEntry(
        val value: String,
        val timestamp: Long,
        val ttlMs: Long,
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    override fun getState(): CacheState =
        CacheState(
            cacheEntries = cache.mapValues { it.value.timestamp },
        )

    override fun loadState(state: CacheState) {
        cache.clear()
        // In a real implementation, we would restore the cache entries
        // For now, just clear the cache
    }

    companion object {
        fun getInstance(): CacheManager = CacheManager()
    }

    /**
     * Get cached vulnerabilities
     */
    fun getCachedVulnerabilities(key: String): List<Vulnerability>? {
        val entry = cache[key]
        return if (entry != null && !entry.isExpired()) {
            entry.vulnerabilities
        } else {
            entry?.let { cache.remove(key) }
            null
        }
    }

    /**
     * Cache vulnerabilities
     */
    fun cacheVulnerabilities(
        key: String,
        vulnerabilities: List<Vulnerability>,
    ) {
        val entry = CacheEntry(vulnerabilities, System.currentTimeMillis())
        cache[key] = entry
    }

    /**
     * Invalidate cache for a specific key
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Invalidate all cache entries
     */
    fun invalidateAll() {
        cache.clear()
        stringCache.clear()
    }

    /**
     * Remove expired entries
     */
    fun cleanupExpired() {
        val keysToRemove =
            cache.keys.filter { key ->
                val entry = cache[key]
                entry?.isExpired() ?: true
            }
        keysToRemove.forEach { cache.remove(it) }
    }

    /**
     * Get cache size
     */
    fun size(): Int = cache.size

    /**
     * Get cache stats
     */
    fun getStats(): Map<String, Any> =
        mapOf(
            "size" to size(),
            "entries" to cache.keys,
        )

    /**
     * Get cached string value
     */
    fun getString(key: String): String? {
        val entry = stringCache[key]
        return if (entry != null && !entry.isExpired()) {
            entry.value
        } else {
            entry?.let { stringCache.remove(key) }
            null
        }
    }

    /**
     * Cache a string value with TTL in milliseconds
     */
    fun cacheString(
        key: String,
        value: String,
        ttlMs: Long,
    ) {
        val entry = StringCacheEntry(value, System.currentTimeMillis(), ttlMs)
        stringCache[key] = entry
    }

    /**
     * Invalidate string cache for a specific key
     */
    fun invalidateString(key: String) {
        stringCache.remove(key)
    }
}
