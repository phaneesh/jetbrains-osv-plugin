// Configuration Audit Service — Detect insecure framework configurations
package io.dyuti.osvplugin.configaudit

/**
 * Service for auditing framework configuration files for security issues.
 *
 * Scans properties files, YAML files, and configuration classes for common
 * security misconfigurations across popular Java frameworks.
 */
object ConfigAuditService {
    /**
     * Audit a single properties file (properties or YAML content).
     */
    fun auditProperties(
        fileName: String,
        content: String,
    ): List<ConfigAuditFinding> {
        val findings = mutableListOf<ConfigAuditFinding>()
        val lines = content.lines()

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val parts = trimmed.split("=", ":", limit = 2)
            if (parts.size < 2) continue

            val key = parts[0].trim()
            val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")

            checkProperty(fileName, lineNumber, key, value)?.let { findings.add(it) }
        }
        return findings
    }

    /**
     * Check a single key-value property pair for security issues.
     */
    internal fun checkProperty(
        fileName: String,
        lineNumber: Int,
        key: String,
        value: String,
    ): ConfigAuditFinding? {
        // Spring Security
        if (key.contains("spring.security.enabled", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-001",
                title = "Spring Security Disabled",
                description = "Spring Security is explicitly disabled. This removes authentication and authorization from the application.",
                severity = ConfigSeverity.CRITICAL,
                framework = "Spring Security",
                recommendation = "Remove or set spring.security.enabled=true, or configure proper security rules.",
                cweId = "CWE-306",
            )
        }

        if (key.contains("debug", ignoreCase = true) && value.equals("true", ignoreCase = true)) {
            if (key.contains("security", ignoreCase = true)) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-002",
                    title = "Security Debug Mode Enabled",
                    description = "Debug mode for security is enabled, which may expose sensitive stack traces and configuration details.",
                    severity = ConfigSeverity.HIGH,
                    framework = "Spring Security",
                    recommendation = "Set security debug to false in production.",
                    cweId = "CWE-200",
                )
            }
        }

        // HTTPS / TLS
        if (key.contains("server.ssl.enabled", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-003",
                title = "SSL/TLS Disabled",
                description = "HTTPS is disabled. All traffic will be transmitted in plaintext.",
                severity = ConfigSeverity.CRITICAL,
                framework = "Spring Boot",
                recommendation = "Set server.ssl.enabled=true and configure a valid keystore.",
                cweId = "CWE-319",
            )
        }

        if ((key.contains("server.port", ignoreCase = true) || key == "server.port") && value == "80") {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-004",
                title = "HTTP Port Used Without SSL",
                description = "Application is running on port 80 without explicit SSL configuration. This implies HTTP-only traffic.",
                severity = ConfigSeverity.HIGH,
                framework = "Spring Boot",
                recommendation = "Use port 443 with SSL enabled, or configure a reverse proxy with TLS termination.",
                cweId = "CWE-319",
            )
        }

        // CSRF
        if (key.contains("csrf.disabled", ignoreCase = true) && value.equals("true", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-005",
                title = "CSRF Protection Disabled",
                description = "Cross-Site Request Forgery protection is disabled. This allows state-changing requests to be forged.",
                severity = ConfigSeverity.HIGH,
                framework = "Spring Security",
                recommendation = "Enable CSRF protection. If using stateless APIs, use CSRF tokens or switch to session-less authentication.",
                cweId = "CWE-352",
            )
        }

        // Actuator Security
        if (key.contains("management.endpoints.web.exposure.include", ignoreCase = true)) {
            if (value.contains("*") || value.contains("env", ignoreCase = true) || value.contains("heapdump", ignoreCase = true)) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-006",
                    title = "Sensitive Actuator Endpoints Exposed",
                    description = "Actuator endpoints like /env and /heapdump expose sensitive application state and may leak secrets.",
                    severity = ConfigSeverity.CRITICAL,
                    framework = "Spring Boot Actuator",
                    recommendation = "Expose only health and info endpoints publicly. Secure others with authentication or disable them.",
                    cweId = "CWE-200",
                )
            }
        }

        if (key.contains("management.security.enabled", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-007",
                title = "Actuator Security Disabled",
                description = "Actuator endpoints are accessible without authentication.",
                severity = ConfigSeverity.HIGH,
                framework = "Spring Boot Actuator",
                recommendation = "Enable management security and restrict access to authorized roles.",
                cweId = "CWE-306",
            )
        }

        // Hibernate SQL Injection
        if ((key.contains("hibernate.hbm2ddl.auto", ignoreCase = true) || key.contains("hibernate.ddl-auto", ignoreCase = true)) &&
            (value.equals("create", ignoreCase = true) || value.equals("create-drop", ignoreCase = true))
        ) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-008",
                title = "Destructive DDL Mode in Production",
                description = "Hibernate is configured to drop and recreate the database schema. This will destroy all data on startup.",
                severity = ConfigSeverity.CRITICAL,
                framework = "Hibernate",
                recommendation = "Use 'validate' or 'none' in production. Use 'update' only for development.",
                cweId = "CWE-489",
            )
        }

        // SQL Injection
        if (key.contains("hibernate.show_sql", ignoreCase = true) && value.equals("true", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-009",
                title = "SQL Logging Enabled",
                description = "Hibernate SQL logging is enabled, which may leak database schema and query structure in logs.",
                severity = ConfigSeverity.MEDIUM,
                framework = "Hibernate",
                recommendation = "Set hibernate.show_sql=false in production. Use parameterized logging for debugging.",
                cweId = "CWE-532",
            )
        }

        // Session / Cookie
        if (key.contains("server.servlet.session.timeout", ignoreCase = true)) {
            val timeoutMinutes = value.toIntOrNull()
            if (timeoutMinutes != null && timeoutMinutes > 120) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-010",
                    title = "Session Timeout Too Long",
                    description = "Session timeout is set to $timeoutMinutes minutes. Long-lived sessions increase the attack window for session hijacking.",
                    severity = ConfigSeverity.MEDIUM,
                    framework = "Spring Boot",
                    recommendation = "Set session timeout to 30 minutes or less. Use refresh tokens for long-lived access.",
                    cweId = "CWE-613",
                )
            }
        }

        if (key.contains("server.servlet.session.cookie.http-only", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-011",
                title = "Session Cookie Missing HttpOnly Flag",
                description = "Session cookies are accessible to JavaScript, making them vulnerable to XSS-based session theft.",
                severity = ConfigSeverity.HIGH,
                framework = "Spring Boot",
                recommendation = "Set server.servlet.session.cookie.http-only=true.",
                cweId = "CWE-1004",
            )
        }

        if (key.contains("server.servlet.session.cookie.secure", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-012",
                title = "Session Cookie Missing Secure Flag",
                description = "Session cookies are transmitted over unencrypted HTTP connections.",
                severity = ConfigSeverity.HIGH,
                framework = "Spring Boot",
                recommendation = "Set server.servlet.session.cookie.secure=true.",
                cweId = "CWE-614",
            )
        }

        // CORS
        if (key.contains("cors.allowed-origins", ignoreCase = true) || key.contains("cors.allowedOrigins", ignoreCase = true)) {
            if (value == "*") {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-013",
                    title = "CORS Allows All Origins",
                    description = "Cross-Origin Resource Sharing is configured to allow all origins (*). This enables malicious websites to make requests to your API.",
                    severity = ConfigSeverity.HIGH,
                    framework = "Spring Boot",
                    recommendation = "Specify explicit allowed origins. Use a whitelist of trusted domains.",
                    cweId = "CWE-693",
                )
            }
        }

        // Log4j / Logging
        if (key.contains("log4j2.formatMsgNoLookups", ignoreCase = true) && value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-014",
                title = "Log4j Message Lookups Enabled",
                description = "Log4j message lookups (JNDI) are enabled. This is the root cause of Log4Shell (CVE-2021-44228).",
                severity = ConfigSeverity.CRITICAL,
                framework = "Log4j",
                recommendation = "Set log4j2.formatMsgNoLookups=true, or upgrade to Log4j 2.17.1+.",
                cweId = "CWE-502",
            )
        }

        // Database credentials in plaintext
        if (key.contains("password", ignoreCase = true) || key.contains("secret", ignoreCase = true)) {
            if (value.isNotBlank() && !value.contains("\${", ignoreCase = true) && !value.contains("ENC(", ignoreCase = true)) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-015",
                    title = "Hardcoded Password/Secret Detected",
                    description = "A password or secret key is hardcoded in the configuration file. This exposes credentials in version control.",
                    severity = ConfigSeverity.CRITICAL,
                    framework = "General",
                    recommendation = "Use environment variables, a secrets manager (AWS Secrets Manager, HashiCorp Vault), or Jasypt encryption.",
                    cweId = "CWE-798",
                )
            }
        }

        // File Upload
        if (key.contains("spring.servlet.multipart.max-file-size", ignoreCase = true)) {
            val sizeValue = value.uppercase()
            if (sizeValue.contains("GB") || sizeValue.contains("TB")) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-016",
                    title = "Excessive File Upload Size Limit",
                    description = "File upload size limit is set to $value. This enables Denial-of-Service via disk exhaustion.",
                    severity = ConfigSeverity.HIGH,
                    framework = "Spring Boot",
                    recommendation = "Limit file upload size to a reasonable value (e.g., 10MB) and validate file types.",
                    cweId = "CWE-400",
                )
            }
        }

        // Content Security Policy
        if (key.contains("security.headers.content-security-policy", ignoreCase = true) || key.contains("csp", ignoreCase = true)) {
            if (value == "none" || value.isBlank()) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-017",
                    title = "Content Security Policy Not Configured",
                    description = "No Content-Security-Policy header is configured. This allows XSS, data injection, and clickjacking attacks.",
                    severity = ConfigSeverity.MEDIUM,
                    framework = "Spring Boot",
                    recommendation = "Configure a restrictive CSP header. Start with 'default-src self' and expand as needed.",
                    cweId = "CWE-693",
                )
            }
        }

        // X-Frame-Options
        if (key.contains("frame-options", ignoreCase = true) && value.contains("disable", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-018",
                title = "X-Frame-Options Disabled",
                description = "X-Frame-Options header is disabled. The application can be embedded in an iframe, enabling clickjacking attacks.",
                severity = ConfigSeverity.MEDIUM,
                framework = "Spring Boot",
                recommendation = "Set X-Frame-Options to DENY or SAMEORIGIN.",
                cweId = "CWE-693",
            )
        }

        // HSTS
        if (key.contains("hsts", ignoreCase = true) || key.contains("strict-transport-security", ignoreCase = true)) {
            if (value.contains("disable", ignoreCase = true) || value.equals("false", ignoreCase = true)) {
                return finding(
                    fileName,
                    lineNumber,
                    key,
                    value,
                    id = "CONFIG-019",
                    title = "HSTS Disabled",
                    description = "HTTP Strict Transport Security is disabled. Browsers will not enforce HTTPS for this domain.",
                    severity = ConfigSeverity.HIGH,
                    framework = "Spring Boot",
                    recommendation = "Enable HSTS with a max-age of at least 31536000 seconds (1 year).",
                    cweId = "CWE-319",
                )
            }
        }

        // CORS credentials
        if (key.contains("cors.allow-credentials", ignoreCase = true) && value.equals("true", ignoreCase = true)) {
            // Only flag if origins is also wildcard — already caught above
        }

        // Jackson serialization — default typing (insecure)
        if (key.contains("spring.jackson.default-typing", ignoreCase = true) && !value.equals("false", ignoreCase = true)) {
            return finding(
                fileName,
                lineNumber,
                key,
                value,
                id = "CONFIG-020",
                title = "Jackson Default Typing Enabled",
                description = "Jackson polymorphic default typing is enabled. This can lead to deserialization of untrusted data (Remote Code Execution).",
                severity = ConfigSeverity.CRITICAL,
                framework = "Jackson",
                recommendation = "Disable default typing (set to false) or use explicit @JsonTypeInfo annotations.",
                cweId = "CWE-502",
            )
        }

        return null
    }

    private fun finding(
        fileName: String,
        lineNumber: Int,
        propertyName: String,
        currentValue: String,
        id: String,
        title: String,
        description: String,
        severity: ConfigSeverity,
        framework: String,
        recommendation: String,
        cweId: String? = null,
    ): ConfigAuditFinding =
        ConfigAuditFinding(
            id = id,
            title = title,
            description = "$description Current value: '$currentValue'.",
            severity = severity,
            framework = framework,
            fileName = fileName,
            lineNumber = lineNumber,
            propertyName = propertyName,
            recommendation = recommendation,
            cweId = cweId,
        )
}
