// Tests for VulnerableApiService — Reachability Analysis
package io.dyuti.osvplugin.toolwindow

import io.dyuti.osvplugin.api.model.AffectedFunction
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VulnerableApiServiceTest {
    private val vulnApiService = VulnerableApiService()

    @Test
    fun `parse functions from JSON extracts signatures`() {
        val json =
            """
            {"affected": [{
              "database_specific": {
                "functions": [
                  "org.apache.logging.log4j.Logger.debug",
                  "org.apache.logging.log4j.Logger.error"
                ]
              }
            }]}
            """.trimIndent()

        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", json)

        assertEquals(2, functions.size)
        assertEquals("org.apache.logging.log4j.Logger.debug", functions[0].signature)
        assertEquals("org.apache.logging.log4j.Logger", functions[0].className)
        assertEquals("debug", functions[0].methodName)
        assertEquals("org.apache.logging.log4j.Logger.error", functions[1].signature)
    }

    @Test
    fun `parse empty functions returns empty list`() {
        val json = """{"affected": [{"database_specific": {"functions": []}}]}"""
        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", json)
        assertTrue(functions.isEmpty())
    }

    @Test
    fun `parse missing functions field returns empty list`() {
        val json = """{"affected": [{"package": {"name": "log4j"}}]}"""
        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", json)
        assertTrue(functions.isEmpty())
    }

    @Test
    fun `parse null JSON returns empty list`() {
        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", null)
        assertTrue(functions.isEmpty())
    }

    @Test
    fun `parse malformed JSON returns empty list`() {
        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", "not valid json")
        assertTrue(functions.isEmpty())
    }

    @Test
    fun `parse functions with nested package names`() {
        val json =
            """
            {"affected": [{
              "database_specific": {
                "functions": [
                  "com.example.core.security.AuthManager.authenticate"
                ]
              }
            }]}
            """.trimIndent()

        val functions = vulnApiService.parseVulnerableFunctionsFromJson("GHSA-xxx", json)

        assertEquals(1, functions.size)
        assertEquals("authenticate", functions[0].methodName)
        assertEquals("com.example.core.security.AuthManager", functions[0].className)
    }

    @Test
    fun `affected function model stores all fields`() {
        val func =
            AffectedFunction(
                signature = "org.example.Utils.parse",
                className = "org.example.Utils",
                methodName = "parse",
            )

        assertEquals("org.example.Utils.parse", func.signature)
        assertEquals("org.example.Utils", func.className)
        assertEquals("parse", func.methodName)
    }

    @Test
    fun `affected function model with null className still works`() {
        val func =
            AffectedFunction(
                signature = "parse",
                className = null,
                methodName = "parse",
            )

        assertEquals("parse", func.signature)
        assertNull(func.className)
    }

    @Test
    fun `vulnerability with affectedFunctions in model`() {
        val affectedFunctions =
            listOf(
                AffectedFunction("org.example.Foo.bar", "org.example.Foo", "bar"),
            )

        val vuln =
            Vulnerability(
                id = "GHSA-test",
                cveIds = emptyList(),
                summary = "Test",
                details = "Details",
                severity = OsVSeverity.HIGH,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = emptyList(),
                references = emptyList(),
                cweIds = emptyList(),
                lineNumber = null,
                affectedFunctions = affectedFunctions,
            )

        assertEquals(1, vuln.affectedFunctions.size)
        assertEquals("bar", vuln.affectedFunctions[0].methodName)
    }
}
