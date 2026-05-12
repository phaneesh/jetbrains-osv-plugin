// Tests for Configuration Audit Service
package io.dyuti.osvplugin.configaudit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigAuditServiceTest {
    private fun audit(content: String) = ConfigAuditService.auditProperties("application.properties", content)

    @Test
    fun `detects disabled spring security`() {
        val findings = audit("spring.security.enabled=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-001", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `detects ssl disabled`() {
        val findings = audit("server.ssl.enabled=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-003", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `detects http port without ssl`() {
        val findings = audit("server.port=80")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-004", findings[0].id)
    }

    @Test
    fun `detects csrf disabled`() {
        val findings = audit("spring.security.csrf.disabled=true")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-005", findings[0].id)
    }

    @Test
    fun `detects actuator wildcard exposure`() {
        val findings = audit("management.endpoints.web.exposure.include=*")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-006", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `detects actuator env exposure`() {
        val findings = audit("management.endpoints.web.exposure.include=env,health")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-006", findings[0].id)
    }

    @Test
    fun `detects actuator security disabled`() {
        val findings = audit("management.security.enabled=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-007", findings[0].id)
    }

    @Test
    fun `detects destructive hibernate ddl`() {
        val findings = audit("spring.jpa.hibernate.ddl-auto=create-drop")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-008", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `detects sql logging enabled`() {
        val findings = audit("spring.jpa.properties.hibernate.show_sql=true")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-009", findings[0].id)
        assertEquals(ConfigSeverity.MEDIUM, findings[0].severity)
    }

    @Test
    fun `detects long session timeout`() {
        val findings = audit("server.servlet.session.timeout=480")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-010", findings[0].id)
    }

    @Test
    fun `ignores short session timeout`() {
        val findings = audit("server.servlet.session.timeout=30")
        assertEquals(0, findings.size)
    }

    @Test
    fun `detects missing httponly cookie flag`() {
        val findings = audit("server.servlet.session.cookie.http-only=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-011", findings[0].id)
    }

    @Test
    fun `detects missing secure cookie flag`() {
        val findings = audit("server.servlet.session.cookie.secure=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-012", findings[0].id)
    }

    @Test
    fun `detects wildcard cors`() {
        val findings = audit("cors.allowed-origins=*")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-013", findings[0].id)
    }

    @Test
    fun `detects log4j lookups enabled`() {
        val findings = audit("log4j2.formatMsgNoLookups=false")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-014", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `detects hardcoded password`() {
        val findings = audit("spring.datasource.password=supersecret123")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-015", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `ignores env var password`() {
        val findings = audit("spring.datasource.password=\${DB_PASSWORD}")
        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores encrypted password`() {
        val findings = audit("spring.datasource.password=ENC(encryptedvalue)")
        assertEquals(0, findings.size)
    }

    @Test
    fun `detects excessive upload size`() {
        val findings = audit("spring.servlet.multipart.max-file-size=5GB")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-016", findings[0].id)
    }

    @Test
    fun `detects missing csp`() {
        val findings = audit("security.headers.content-security-policy=none")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-017", findings[0].id)
    }

    @Test
    fun `detects disabled x-frame-options`() {
        val findings = audit("security.headers.frame-options=disable")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-018", findings[0].id)
    }

    @Test
    fun `detects disabled hsts`() {
        val findings = audit("security.headers.hsts=disabled")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-019", findings[0].id)
    }

    @Test
    fun `detects jackson default typing`() {
        val findings = audit("spring.jackson.default-typing=object")
        assertEquals(1, findings.size)
        assertEquals("CONFIG-020", findings[0].id)
        assertEquals(ConfigSeverity.CRITICAL, findings[0].severity)
    }

    @Test
    fun `skips comments and blank lines`() {
        val content =
            """
            # This is a comment
            spring.application.name=myapp

            server.port=8080
            """.trimIndent()
        val findings = audit(content)
        assertEquals(0, findings.size)
    }

    @Test
    fun `audit result aggregates correctly`() {
        val content =
            """
            spring.security.enabled=false
            server.ssl.enabled=false
            management.endpoints.web.exposure.include=*
            spring.datasource.password=secret
            spring.jpa.hibernate.ddl-auto=create
            hibernate.show_sql=true
            """.trimIndent()
        val findings = audit(content)
        assertTrue(findings.size >= 5)

        val result = ConfigAuditResult(findings, 1, 0)
        assertTrue(result.criticalCount >= 4)
        assertTrue(result.hasActionableFindings())
    }
}
