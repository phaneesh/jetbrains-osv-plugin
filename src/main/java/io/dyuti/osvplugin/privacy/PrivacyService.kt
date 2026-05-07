// Privacy-preserving package name handling for OSV queries
// Wraps PrivacyHasher with IntelliJ-configured salt and enablement
package io.dyuti.osvplugin.privacy

import com.intellij.openapi.components.service
import io.dyuti.osvplugin.config.OsVConfig
import java.util.UUID

/**
 * Privacy-preserving service that protects intellectual property by hashing
 * package names in UI, logs, exports, and persistent state.
 *
 * This service wraps [PrivacyHasher] and provides IntelliJ-specific configuration
 * management (salt persistence in [OsVConfig], enable/disable toggles).
 *
 * ## Threat Model
 *
 * - Screenshots of IDE tool windows shared in public channels
 * - Exported SARIF reports committed to public repositories
 * - Plugin log files uploaded for debugging
 * - CI/CD artifacts containing dependency lists
 *
 * ## Limitation
 *
 * The OSV API **must** receive actual package names — there is no hash-based query
 * endpoint. If this is unacceptable, self-host an OSV mirror.
 *
 * ## Architecture
 *
 * ```
 * PrivacyService (IntelliJ-aware)
 *   ├── PrivacyHasher (standalone, no IntelliJ deps)
 *   │     ├── ConcurrentHashMap<String, String> nameMap
 *   │     ├── ConcurrentHashMap<String, String> reverseMap
 *   │     └── SHA-256 truncated to 16 hex chars
 *   └── OsVConfig (salt persistence, enable/disable)
 * ```
 */
class PrivacyService private constructor() {
    companion object {
        @Volatile
        private var _instance: PrivacyService? = null

        fun getInstance(): PrivacyService =
            _instance ?: synchronized(this) {
                _instance ?: PrivacyService().also { _instance = it }
            }
    }

    /** Standalone hasher — unit-testable without IntelliJ framework. */
    private val hasher = PrivacyHasher()

    init {
        syncFromConfig()
    }

    /**
     * Sync hasher state from [OsVConfig]: salt and enabled flag.
     * Call after config changes or on initialization.
     */
    fun syncFromConfig() {
        val config = service<OsVConfig>()
        hasher.enabled = config.privacyPreservingEnabled
        hasher.setSalt(config.privacySalt)
    }

    /**
     * Generate and persist a new random salt in [OsVConfig].
     * Call once per IDE installation.
     */
    fun generateSalt() {
        val config = service<OsVConfig>()
        if (config.privacySalt == null || config.privacySalt!!.isBlank()) {
            config.privacySalt = UUID.randomUUID().toString()
            syncFromConfig()
        }
    }

    /** Return whether privacy-preserving mode is enabled. */
    fun isEnabled(): Boolean = hasher.isActive()

    /** Obfuscate a package name. */
    fun obfuscate(
        packageName: String,
        ecosystem: String = "Maven",
    ): String {
        syncFromConfig()
        return hasher.obfuscate(packageName, ecosystem)
    }

    /** Resolve an obfuscated name back to its original. */
    fun resolveOriginalName(obfuscatedName: String): String? {
        syncFromConfig()
        return hasher.resolveOriginalName(obfuscatedName)
    }

    /** Batch obfuscate. */
    fun obfuscateAll(packages: List<Pair<String, String>>): List<String> {
        syncFromConfig()
        return hasher.obfuscateAll(packages)
    }

    /** Batch resolve. */
    fun resolveAll(obfuscatedNames: List<String>): List<String> {
        syncFromConfig()
        return hasher.resolveAll(obfuscatedNames)
    }

    /** Re-salt: generate new salt, invalidate existing mappings. */
    fun rotateSalt() {
        val config = service<OsVConfig>()
        config.privacySalt = UUID.randomUUID().toString()
        syncFromConfig()
    }

    /** Clear in-memory name mappings. */
    fun clearMappings() {
        hasher.clearMappings()
    }
}
