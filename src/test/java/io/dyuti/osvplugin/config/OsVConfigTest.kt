package io.dyuti.osvplugin.config

import io.dyuti.osvplugin.api.model.OsVSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests for OsVConfig
 */
class OsVConfigTest {
    @Test
    fun `config has default values`() {
        val config = OsVConfig()

        assertEquals(OsVSeverity.MEDIUM, config.minimumSeverity)
        assertEquals(true, config.inspectionEnabled)
        assertEquals(24, config.cacheTtl)
        assertEquals(true, config.rateLimitEnabled)
        assertEquals(1000, config.rateLimitRequestsPerHour)
        assertEquals(true, config.scanDirectDependencies)
        assertEquals(true, config.scanTransitiveDependencies)
        assertEquals(false, config.githubAdvisoryEnabled)
        assertEquals(false, config.licenseScanningEnabled)
        assertEquals(3, config.allowedLicenses.size)
        assertEquals(false, config.focusModeEnabled)
        assertEquals("main", config.baseBranch)
        assertEquals(null, config.sarifExportPath)

        // Tokens are stored via PasswordSafe, not as fields
        assertNull(OsVConfig.getGithubToken())
        assertNull(OsVConfig.getJiraToken())
        assertNull(OsVConfig.getPrivacySalt())
    }

    @Test
    fun `config can be modified`() {
        val config = OsVConfig()

        config.minimumSeverity = OsVSeverity.CRITICAL
        config.inspectionEnabled = false
        config.cacheTtl = 2
        config.rateLimitEnabled = false
        config.rateLimitRequestsPerHour = 200
        config.scanDirectDependencies = false
        config.scanTransitiveDependencies = false
        config.githubAdvisoryEnabled = true
        OsVConfig.setGithubToken("gh_token_123")
        config.licenseScanningEnabled = true
        config.allowedLicenses = listOf("MIT", "Apache-2.0")
        config.focusModeEnabled = true
        config.baseBranch = "develop"
        config.sarifExportPath = "/path/to/export"

        assertEquals(OsVSeverity.CRITICAL, config.minimumSeverity)
        assertEquals(false, config.inspectionEnabled)
        assertEquals(2, config.cacheTtl)
        assertEquals(false, config.rateLimitEnabled)
        assertEquals(200, config.rateLimitRequestsPerHour)
        assertEquals(false, config.scanDirectDependencies)
        assertEquals(false, config.scanTransitiveDependencies)
        assertEquals(true, config.githubAdvisoryEnabled)
        assertEquals("gh_token_123", OsVConfig.getGithubToken())
        assertEquals(true, config.licenseScanningEnabled)
        assertEquals(2, config.allowedLicenses.size)
        assertEquals(true, config.focusModeEnabled)
        assertEquals("develop", config.baseBranch)
        assertEquals("/path/to/export", config.sarifExportPath)

        // Clean up test token
        OsVConfig.setGithubToken(null)
    }

    @Test
    fun `config implements persistent state component`() {
        val config = OsVConfig()

        // Verify the config implements the required interface methods
        val state = config.getState()
        assertNotNull(state)

        // Verify loadState works
        val newConfig = OsVConfig()
        newConfig.minimumSeverity = OsVSeverity.HIGH
        config.loadState(newConfig)

        assertEquals(OsVSeverity.HIGH, config.minimumSeverity)
    }

    @Test
    fun `password safe accessors work without application`() {
        // In a real IDE, these use PasswordSafe; in tests without Application,
        // they fall back to an in-memory map
        OsVConfig.setGithubToken("token_1")
        OsVConfig.setJiraToken("token_2")
        OsVConfig.setPrivacySalt("salt_1")

        assertEquals("token_1", OsVConfig.getGithubToken())
        assertEquals("token_2", OsVConfig.getJiraToken())
        assertEquals("salt_1", OsVConfig.getPrivacySalt())

        OsVConfig.setGithubToken(null)
        OsVConfig.setJiraToken(null)
        OsVConfig.setPrivacySalt(null)

        assertNull(OsVConfig.getGithubToken())
        assertNull(OsVConfig.getJiraToken())
        assertNull(OsVConfig.getPrivacySalt())
    }
}
