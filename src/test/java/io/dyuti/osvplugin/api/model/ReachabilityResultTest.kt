// Tests for reachability analysis data models
package io.dyuti.osvplugin.api.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReachabilityResultTest {
    @Test
    fun `affected function model stores signature correctly`() {
        val func =
            AffectedFunction(
                signature = "org.apache.logging.log4j.Logger.debug",
                className = "org.apache.logging.log4j.Logger",
                methodName = "debug",
            )

        assertEquals("org.apache.logging.log4j.Logger.debug", func.signature)
        assertEquals("org.apache.logging.log4j.Logger", func.className)
        assertEquals("debug", func.methodName)
    }

    @Test
    fun `vulnerable call site model stores location correctly`() {
        val callSite =
            VulnerableCallSite(
                methodName = "debug",
                className = "org.apache.logging.log4j.Logger",
                filePath = "/src/main/java/MyApp.java",
                lineNumber = 42,
                qualifierExpression = "logger",
            )

        assertEquals("debug", callSite.methodName)
        assertEquals("org.apache.logging.log4j.Logger", callSite.className)
        assertEquals(42, callSite.lineNumber)
    }

    @Test
    fun `reachability result computes reachable correctly`() {
        val dep =
            Dependency(
                name = "log4j",
                version = "2.14.1",
                ecosystem = "Maven",
                scope = "compile",
                transitive = false,
                lineNumber = 10,
            )

        val vuln =
            Vulnerability(
                id = "GHSA-test",
                cveIds = emptyList(),
                summary = "Test",
                details = "Details",
                severity = OsVSeverity.CRITICAL,
                cvssScore = 10.0,
                affectedVersions = emptyList(),
                fixedVersions = listOf("2.15.0"),
                references = emptyList(),
                cweIds = emptyList(),
                lineNumber = null,
                affectedFunctions = emptyList(),
            )

        val unreachable =
            ReachabilityResult(
                vulnerability = vuln,
                dependency = dep,
                callSites = emptyList(),
            )

        val reachable =
            ReachabilityResult(
                vulnerability = vuln,
                dependency = dep,
                callSites =
                    listOf(
                        VulnerableCallSite("debug", "Logger", "MyApp.java", 42, "logger"),
                    ),
            )

        assertFalse(unreachable.reachable)
        assertTrue(reachable.reachable)
    }

    @Test
    fun `affected functions parsed from JSON correctly`() {
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

        // Heuristic: "functions" strings are parsed via regex; verify extraction pattern
        val functionsFound = mutableListOf<String>()
        val regex = """"functions"\s*:\s*\[([^\]]*)\]""".toRegex()
        val match = regex.find(json)
        if (match != null) {
            val arrayContent = match.groupValues[1]
            val functionRegex = """"([^"]+)"""".toRegex()
            functionRegex.findAll(arrayContent).forEach { functionsFound.add(it.groupValues[1]) }
        }

        assertEquals(2, functionsFound.size)
        assertEquals("org.apache.logging.log4j.Logger.debug", functionsFound[0])
        assertEquals("org.apache.logging.log4j.Logger.error", functionsFound[1])
    }
}
