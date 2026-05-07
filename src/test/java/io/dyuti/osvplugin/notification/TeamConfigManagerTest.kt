// Tests for team configuration and notification models
package io.dyuti.osvplugin.notification

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.policy.EnforcementMode
import io.dyuti.osvplugin.policy.PolicyConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class TeamConfigManagerTest {
    @Test
    fun `parse config from json`() {
        val json =
            """
            {
              "teamName": "Security Alpha",
              "scanOnStartup": true,
              "showNotifications": true,
              "notificationThreshold": "HIGH",
              "autoFixEnabled": false,
              "policy": {
                "maxSeverity": "HIGH",
                "maxCvssScore": 7.0,
                "blockCisaKev": true,
                "blockMalicious": true,
                "forbiddenLicenses": ["GPL-3.0"],
                "ignorePackages": ["com.internal:*"]
              }
            }
            """.trimIndent()

        val config = TeamConfigManager.parseConfig(json)
        assertNotNull(config)
        assertEquals("Security Alpha", config!!.teamName)
        assertTrue(config.scanOnStartup)
        assertTrue(config.showNotifications)
        assertEquals(OsVSeverity.HIGH, config.notificationThreshold)
        assertFalse(config.autoFixEnabled)
        assertNotNull(config.policy)
        assertEquals(7.0, config.policy!!.maxCvssScore)
        assertTrue(config.policy!!.blockCisaKev!!)
        assertEquals(listOf("GPL-3.0"), config.policy!!.forbiddenLicenses)
    }

    @Test
    fun `parse config with minimal fields`() {
        val json = "{\"teamName\": \"Minimal\"}"
        val config = TeamConfigManager.parseConfig(json)
        assertNotNull(config)
        assertEquals("Minimal", config!!.teamName)
        assertTrue(config.scanOnStartup) // default
        assertTrue(config.showNotifications) // default
    }

    @Test
    fun `parse invalid json returns null`() {
        val config = TeamConfigManager.parseConfig("{invalid json")
        assertNull(config)
    }

    @Test
    fun `to json round trips correctly`() {
        val original =
            TeamConfig(
                teamName = "Test Team",
                scanOnStartup = false,
                showNotifications = true,
                notificationThreshold = OsVSeverity.LOW,
                autoFixEnabled = true,
            )
        val json = TeamConfigManager.toJson(original)
        val parsed = TeamConfigManager.parseConfig(json)
        assertNotNull(parsed)
        assertEquals(original.teamName, parsed!!.teamName)
        assertEquals(original.scanOnStartup, parsed.scanOnStartup)
        assertEquals(original.notificationThreshold, parsed.notificationThreshold)
    }

    @Test
    fun `has config returns false for nonexistent`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "osv-test-${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        try {
            assertFalse(TeamConfigManager.hasConfig(tmpDir))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `save and load config round trip`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "osv-test-${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        try {
            val config = TeamConfig(teamName = "SaveTest", notificationThreshold = OsVSeverity.CRITICAL)
            TeamConfigManager.save(tmpDir, config)

            assertTrue(TeamConfigManager.hasConfig(tmpDir))

            val loaded = TeamConfigManager.load(tmpDir)
            assertNotNull(loaded)
            assertEquals("SaveTest", loaded!!.teamName)
            assertEquals(OsVSeverity.CRITICAL, loaded.notificationThreshold)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `merge policy applies overrides`() {
        val base =
            PolicyConfig(
                maxSeverity = OsVSeverity.MEDIUM,
                maxSeverityMode = EnforcementMode.FAIL,
                maxCvssScore = 5.0,
                maxCvssMode = EnforcementMode.WARN,
                forbiddenLicenses = listOf("MIT"),
            )
        val overrides =
            TeamPolicyOverrides(
                maxSeverity = OsVSeverity.HIGH,
                maxCvssScore = 9.0,
                forbiddenLicenses = listOf("GPL-3.0"),
            )

        val merged = TeamConfigManager.mergePolicy(base, overrides)
        assertEquals(OsVSeverity.HIGH, merged.maxSeverity)
        assertEquals(9.0, merged.maxCvssScore)
        assertEquals(listOf("GPL-3.0"), merged.forbiddenLicenses)
        // Retained from base:
        assertEquals(EnforcementMode.FAIL, merged.maxSeverityMode)
        assertEquals(EnforcementMode.WARN, merged.maxCvssMode)
    }

    @Test
    fun `merge policy with null overrides keeps base`() {
        val base = PolicyConfig(maxSeverity = OsVSeverity.LOW, maxSeverityMode = EnforcementMode.FAIL)
        val merged = TeamConfigManager.mergePolicy(base, null)
        assertEquals(base, merged)
    }

    @Test
    fun `merge policy with partial overrides`() {
        val base = PolicyConfig(maxSeverity = OsVSeverity.MEDIUM)
        val overrides = TeamPolicyOverrides(blockCisaKev = true)
        val merged = TeamConfigManager.mergePolicy(base, overrides)
        assertEquals(OsVSeverity.MEDIUM, merged.maxSeverity)
        assertTrue(merged.blockCisaKev)
    }
}

class NotificationModelsTest {
    @Test
    fun `notification display text includes severity and cve`() {
        val notification =
            VulnerabilityNotification(
                id = "N1",
                cveId = "CVE-2021-44228",
                packageName = "org.apache.logging.log4j:log4j-core",
                currentVersion = "2.14.0",
                severity = OsVSeverity.CRITICAL,
                summary = "Remote code execution",
                fixVersion = "2.17.1",
                isCisaKev = true,
            )
        val text = notification.displayText()
        assertTrue(text.contains("CVE-2021-44228"))
        assertTrue(text.contains("log4j-core"))
        assertTrue(text.contains("Fix: 2.17.1"))
        assertTrue(text.contains("[CISA KEV]"))
    }

    @Test
    fun `notification display text without cve`() {
        val notification =
            VulnerabilityNotification(
                id = "N2",
                cveId = null,
                packageName = "test:pkg",
                currentVersion = "1.0.0",
                severity = OsVSeverity.HIGH,
                summary = "Test vuln",
                fixVersion = null,
            )
        val text = notification.displayText()
        assertTrue(text.contains("test:pkg"))
        assertFalse(text.contains("CVE"))
    }

    @Test
    fun `notification tooltip includes cve and version`() {
        val notification =
            VulnerabilityNotification(
                id = "N1",
                cveId = "CVE-2021-44228",
                packageName = "org:test",
                currentVersion = "1.0.0",
                severity = OsVSeverity.HIGH,
                summary = "XSS",
                fixVersion = "2.0.0",
                isCisaKev = false,
            )
        val tooltip = notification.tooltipText()
        assertTrue(tooltip.contains("CVE: CVE-2021-44228"))
        assertTrue(tooltip.contains("Fix version: 2.0.0"))
        assertFalse(tooltip.contains("CISA KEV"))
    }

    @Test
    fun `scan metrics distribution with vulnerabilities`() {
        val metrics =
            ScanMetrics(
                scanId = "S1",
                projectName = "Test",
                timestamp = 0,
                totalDependencies = 50,
                vulnerabilitiesFound = 5,
                criticalCount = 1,
                highCount = 2,
                mediumCount = 2,
                lowCount = 0,
            )
        val dist = metrics.severityDistribution()
        assertTrue(dist.contains("1 CRITICAL"))
        assertTrue(dist.contains("2 HIGH"))
        assertTrue(dist.contains("2 MEDIUM"))
    }

    @Test
    fun `scan metrics distribution clean`() {
        val metrics =
            ScanMetrics(
                scanId = "S1",
                projectName = "Test",
                timestamp = 0,
                totalDependencies = 50,
                vulnerabilitiesFound = 0,
            )
        assertEquals("Clean — no vulnerabilities found", metrics.severityDistribution())
    }

    @Test
    fun `notification timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val notification =
            VulnerabilityNotification(
                id = "N1",
                cveId = null,
                packageName = "test",
                currentVersion = "1.0",
                severity = OsVSeverity.LOW,
                summary = "Test",
                fixVersion = null,
            )
        val after = System.currentTimeMillis()
        assertTrue(notification.timestamp in before..after)
    }
}
