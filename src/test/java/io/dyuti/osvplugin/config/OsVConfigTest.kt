package io.dyuti.osvplugin.config

import io.dyuti.osvplugin.api.model.OsVSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        assertEquals(1, config.cacheTtl)
        assertEquals(true, config.rateLimitEnabled)
        assertEquals(100, config.rateLimitRequestsPerHour)
        assertEquals(true, config.scanDirectDependencies)
        assertEquals(true, config.scanTransitiveDependencies)
        assertEquals(false, config.githubAdvisoryEnabled)
        assertEquals(null, config.githubToken)
        assertEquals(false, config.licenseScanningEnabled)
        assertEquals(3, config.allowedLicenses.size)
        assertEquals(false, config.focusModeEnabled)
        assertEquals("main", config.baseBranch)
        assertEquals(null, config.sarifExportPath)
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
        config.githubToken = "gh_token_123"
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
        assertEquals("gh_token_123", config.githubToken)
        assertEquals(true, config.licenseScanningEnabled)
        assertEquals(2, config.allowedLicenses.size)
        assertEquals(true, config.focusModeEnabled)
        assertEquals("develop", config.baseBranch)
        assertEquals("/path/to/export", config.sarifExportPath)
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
}
