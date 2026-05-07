// Project-level team configuration persistence and management
package io.dyuti.osvplugin.notification

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.policy.EnforcementMode
import io.dyuti.osvplugin.policy.PolicyConfig
import java.io.File

/**
 * Manages reading and writing of project-level team configuration from
 * `.idea/osv-plugin-config.json`.
 *
 * This allows teams to share policy settings, notification preferences, and
 * scan behavior across all team members via version control. The config file
 * is stored in the `.idea` directory so it is automatically shared when the
 * team commits `.idea/` to git.
 *
 * ## Configuration Hierarchy
 *
 * Settings are resolved in this priority (highest first):
 * 1. Project-level config (`.idea/osv-plugin-config.json`) ← this class
 * 2. User-level config (IntelliJ persistent settings via [OsVConfig])
 * 3. Plugin defaults
 *
 * This means teams can override personal settings with team-wide standards.
 *
 * ## Usage
 *
 * ```kotlin
 * val teamConfig = TeamConfigManager.load(projectBaseDir)
 * val notificationThreshold = teamConfig?.notificationThreshold ?: OsVSeverity.MEDIUM
 * ```
 */
class TeamConfigManager {
    companion object {
        /** Config file path relative to project root. */
        const val CONFIG_PATH = ".idea/osv-plugin-config.json"

        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        /**
         * Load team config from the project's `.idea/osv-plugin-config.json` if it exists.
         *
         * @param projectBaseDir The project root directory (e.g. `/home/user/myproject`)
         * @return TeamConfig if the file exists and is valid, null otherwise
         */
        @JvmStatic
        fun load(projectBaseDir: File): TeamConfig? {
            val configFile = File(projectBaseDir, CONFIG_PATH)
            if (!configFile.exists()) return null

            return try {
                val json = configFile.readText(Charsets.UTF_8)
                parseConfig(json)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Save team config to `.idea/osv-plugin-config.json`.
         *
         * Creates the `.idea` directory if it doesn't exist.
         */
        @JvmStatic
        fun save(
            projectBaseDir: File,
            config: TeamConfig,
        ) {
            val ideaDir = File(projectBaseDir, ".idea")
            if (!ideaDir.exists()) ideaDir.mkdirs()

            val configFile = File(ideaDir, "osv-plugin-config.json")
            val json = gson.toJson(config)
            configFile.writeText(json, Charsets.UTF_8)
        }

        /**
         * Parse a JSON string into TeamConfig.
         */
        @JvmStatic
        fun parseConfig(json: String): TeamConfig? =
            try {
                gson.fromJson(json, TeamConfig::class.java)
            } catch (_: Exception) {
                null
            }

        /**
         * Serialize TeamConfig to JSON string.
         */
        @JvmStatic
        fun toJson(config: TeamConfig): String = gson.toJson(config)

        /**
         * Check if project has a team config file.
         */
        @JvmStatic
        fun hasConfig(projectBaseDir: File): Boolean = File(projectBaseDir, CONFIG_PATH).exists()

        /**
         * Merge team policy overrides into a base [PolicyConfig] to produce
         * the effective policy for this project.
         *
         * Project-level overrides apply on top of base policy.
         */
        @JvmStatic
        fun mergePolicy(
            base: PolicyConfig,
            overrides: TeamPolicyOverrides?,
        ): PolicyConfig {
            if (overrides == null) return base

            return base.copy(
                maxSeverity = overrides.maxSeverity ?: base.maxSeverity,
                maxCvssScore = overrides.maxCvssScore ?: base.maxCvssScore,
                blockCisaKev = overrides.blockCisaKev ?: base.blockCisaKev,
                blockMalicious = overrides.blockMalicious ?: base.blockMalicious,
                forbiddenLicenses = overrides.forbiddenLicenses ?: base.forbiddenLicenses,
                ignorePackages = overrides.ignorePackages ?: base.ignorePackages,
            )
        }
    }
}
