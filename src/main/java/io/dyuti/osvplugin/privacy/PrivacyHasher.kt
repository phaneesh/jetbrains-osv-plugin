// Core privacy hash logic — no IntelliJ dependencies, purely standalone
package io.dyuti.osvplugin.privacy

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Standalone privacy hasher that obfuscates package names with a configurable salt.
 *
 * This class has **no IntelliJ dependencies** and can be unit-tested without the
 * platform framework.
 *
 * @param salt The salt string (typically a UUID). Must be non-empty for obfuscation.
 *   If null or empty, obfuscation returns the original name unchanged.
 */
class PrivacyHasher(
    private var salt: String? = null,
) {
    /** hash → original name */
    private val nameMap = ConcurrentHashMap<String, String>()

    /** original name → hash (for duplicate detection) */
    private val reverseMap = ConcurrentHashMap<String, String>()

    /**
     * Enable or disable hashing globally.
     */
    var enabled: Boolean = true

    /**
     * Check if hashing is active (enabled and has a non-empty salt).
     */
    fun isActive(): Boolean = enabled && !salt.isNullOrBlank()

    /**
     * Obfuscate a package name.
     *
     * @param packageName Original name (e.g. `org.example:lib`)
     * @param ecosystem Ecosystem name (e.g. `Maven`, `npm`)
     * @return 16-char hex hash, or original name if inactive
     */
    fun obfuscate(
        packageName: String,
        ecosystem: String = "Maven",
    ): String {
        if (!isActive()) return packageName

        val cacheKey = "$ecosystem:$packageName"
        reverseMap[cacheKey]?.let { return it }

        val currentSalt = salt!!
        val hash = sha256("$currentSalt::$packageName::$ecosystem").take(16)

        nameMap[hash] = packageName
        reverseMap[cacheKey] = hash
        return hash
    }

    /**
     * Resolve an obfuscated hash back to its original name.
     */
    fun resolveOriginalName(obfuscatedName: String): String? {
        if (!isActive()) return obfuscatedName
        return nameMap[obfuscatedName]
    }

    /**
     * Obfuscate a batch of packages.
     */
    fun obfuscateAll(packages: List<Pair<String, String>>): List<String> {
        if (!isActive()) return packages.map { it.first }
        return packages.map { (name, ecosystem) -> obfuscate(name, ecosystem) }
    }

    /**
     * Resolve a batch of obfuscated names.
     */
    fun resolveAll(obfuscatedNames: List<String>): List<String> {
        if (!isActive()) return obfuscatedNames
        return obfuscatedNames.map { nameMap[it] ?: it }
    }

    /**
     * Set a new salt, clearing all mappings.
     */
    fun setSalt(newSalt: String?) {
        this.salt = newSalt
        clearMappings()
    }

    /**
     * Clear all in-memory mappings.
     */
    fun clearMappings() {
        nameMap.clear()
        reverseMap.clear()
    }

    /**
     * Private: compute SHA-256 hex digest.
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
