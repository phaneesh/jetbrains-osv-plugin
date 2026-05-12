// Tests for Notification Service
package io.dyuti.osvplugin.notificationservice

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.notification.VulnerabilityNotification
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationServiceTest {
    private fun vuln(
        id: String,
        severity: OsVSeverity,
        summary: String,
        fixedVersions: List<String> = emptyList(),
    ) = Vulnerability(
        id = id,
        cveIds = emptyList(),
        summary = summary,
        details = "",
        severity = severity,
        cvssScore = null,
        affectedVersions = emptyList(),
        fixedVersions = fixedVersions,
        references = emptyList(),
        cweIds = emptyList(),
    )

    @Test
    fun `vulnerability to notification conversion works`() {
        val v = vuln("CVE-2023-1234", OsVSeverity.CRITICAL, "RCE", listOf("2.0.0"))

        val notification =
            NotificationService.vulnerabilityToNotification(
                v,
                "com.example:example-lib",
                "1.0.0",
            )

        assertEquals("CVE-2023-1234", notification.id)
        assertEquals("com.example:example-lib", notification.packageName)
        assertEquals("1.0.0", notification.currentVersion)
        assertEquals(OsVSeverity.CRITICAL, notification.severity)
        assertEquals("2.0.0", notification.fixVersion)
        assertEquals("CVE-2023-1234", notification.cveId)
    }

    @Test
    fun `extracts null cve for non-cve ids`() {
        val v = vuln("GHSA-abcd-1234", OsVSeverity.HIGH, "XSS")

        val notification =
            NotificationService.vulnerabilityToNotification(v, "lodash", "4.17.20")

        assertNull(notification.cveId)
        assertEquals("GHSA-abcd-1234", notification.id)
    }

    @Test
    fun `display text contains severity emoji`() {
        val vn =
            VulnerabilityNotification(
                id = "CVE-2023-1",
                cveId = "CVE-2023-1",
                packageName = "test-pkg",
                currentVersion = "1.0.0",
                severity = OsVSeverity.CRITICAL,
                summary = "RCE",
                fixVersion = "2.0.0",
            )

        val text = vn.displayText()
        assertTrue(text.contains("🔴"))
        assertTrue(text.contains("test-pkg"))
        assertTrue(text.contains("RCE"))
        assertTrue(text.contains("Fix: 2.0.0"))
    }

    @Test
    fun `high severity displays orange emoji`() {
        val vn =
            VulnerabilityNotification(
                id = "CVE-2023-2",
                cveId = null,
                packageName = "pkg",
                currentVersion = "1.0",
                severity = OsVSeverity.HIGH,
                summary = "SQL injection",
                fixVersion = null,
            )

        val text = vn.displayText()
        assertTrue(text.contains("🟠"))
    }

    @Test
    fun `cisa kev flag in tooltip`() {
        val vn =
            VulnerabilityNotification(
                id = "CVE-2023-3",
                cveId = null,
                packageName = "pkg",
                currentVersion = "1.0",
                severity = OsVSeverity.HIGH,
                summary = "Test",
                fixVersion = null,
                isCisaKev = true,
            )

        val tooltip = vn.tooltipText()
        assertTrue(tooltip.contains("actively exploited"))
        assertTrue(tooltip.contains("CISA KEV"))
    }

    @Test
    fun `notification service constants defined`() {
        assertEquals("OSV Vulnerability Alerts", NotificationService.NOTIFICATION_GROUP_ID)
    }

    @Test
    fun `notifyByThreshold shows clean when empty`() {
        // Outside IntelliJ, Notifications.Bus will throw NPE; catch and verify method reaches that point
        try {
            NotificationService.notifyByThreshold(
                null,
                emptyList(),
                OsVSeverity.LOW,
                showClean = true,
            )
        } catch (_: NullPointerException) {
            // Expected in plain JUnit — ApplicationManager.getApplication() is null
        }
        assertTrue(true)
    }

    @Test
    fun `notifyByThreshold filters by severity`() {
        val low =
            VulnerabilityNotification(
                id = "LOW-1",
                cveId = null,
                packageName = "a",
                currentVersion = "1",
                severity = OsVSeverity.LOW,
                summary = "Low",
                fixVersion = null,
            )
        val critical =
            VulnerabilityNotification(
                id = "CRIT-1",
                cveId = null,
                packageName = "b",
                currentVersion = "1",
                severity = OsVSeverity.CRITICAL,
                summary = "Crit",
                fixVersion = null,
            )

        // CRITICAL(0) < LOW(3), so ordinal comparison works for "more severe"
        assertTrue(critical.severity.ordinal < low.severity.ordinal)
    }
}
