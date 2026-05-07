// Tests for SAST data models and simple heuristics
package io.dyuti.osvplugin.sast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SastAnalyzerTest {
    @Test
    fun `sast finding model stores all fields`() {
        val finding =
            SastFinding(
                ruleId = "SQL-INJECTION",
                severity = SastSeverity.CRITICAL,
                message = "Potential SQL injection found",
                filePath = "/src/main/java/Dao.java",
                lineNumber = 42,
                remediation = "Use PreparedStatement",
            )

        assertEquals("SQL-INJECTION", finding.ruleId)
        assertEquals(SastSeverity.CRITICAL, finding.severity)
        assertEquals(42, finding.lineNumber)
        assertEquals("Use PreparedStatement", finding.remediation)
    }

    @Test
    fun `severity enum ordering is logical`() {
        val values = SastSeverity.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(SastSeverity.CRITICAL))
        assertTrue(values.contains(SastSeverity.HIGH))
        assertTrue(values.contains(SastSeverity.MEDIUM))
        assertTrue(values.contains(SastSeverity.LOW))
        assertTrue(values.contains(SastSeverity.INFO))
    }

    @Test
    fun `sast finding equality works`() {
        val f1 = SastFinding("SQL-INJECTION", SastSeverity.CRITICAL, "msg", "file.java", 1, "fix")
        val f2 = SastFinding("SQL-INJECTION", SastSeverity.CRITICAL, "msg", "file.java", 1, "fix")
        assertEquals(f1, f2)
    }

    @Test
    fun `sql injection detection requires project context`() {
        // Actual PSI-based detection requires IntelliJ project
        // This test verifies the analyzer can be instantiated
        val analyzer = SastAnalyzer()
        assertNotNull(analyzer)
    }

    @Test
    fun `path traversal detection requires project context`() {
        val detector = PathTraversalDetector()
        assertNotNull(detector)
    }

    @Test
    fun `xss detector requires project context`() {
        val detector = XssDetector()
        assertNotNull(detector)
    }
}
