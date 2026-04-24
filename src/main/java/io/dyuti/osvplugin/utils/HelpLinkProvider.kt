// Context-aware help provider with Snyk Learn-style links
package io.dyuti.osvplugin.utils

import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * Context-aware help provider with Snyk Learn-style links
 */
object HelpLinkProvider {
    
    /**
     * Get help link for vulnerability
     */
    fun getHelpLink(vulnerability: Vulnerability): String {
        return when {
            vulnerability.id.startsWith("GHSA") -> "https://github.com/advisories/${vulnerability.id}"
            vulnerability.id.startsWith("CVE-") -> "https://cve.mitre.org/cgi-bin/cvename.cgi?name=${vulnerability.id}"
            else -> vulnerability.references.firstOrNull() ?: "https://osv.dev"
        }
    }
    
    /**
     * Get CWE info link
     */
    fun getCweLink(cweId: String): String {
        return "https://cwe.mitre.org/data/definitions/${cweId.replace("CWE-", "")}.html"
    }
    
    /**
     * Get severity-based help content
     */
    fun getSeverityHelp(severity: String): String {
        return when (severity.uppercase()) {
            "CRITICAL" -> """
                Critical: Immediate action required
                This vulnerability allows attackers to:
                - Execute arbitrary code
                - Bypass authentication
                - Access sensitive data
                
                Recommended: Update immediately or apply workaround
            """.trimIndent()
            "HIGH" -> """
                High: Urgent remediation needed
                This vulnerability may allow:
                - Remote code execution
                - Privilege escalation
                - Data exposure
                
                Recommended: Plan remediation within 7 days
            """.trimIndent()
            "MEDIUM" -> """
                Medium: Schedule remediation
                This vulnerability poses moderate risk:
                - Information disclosure
                - Denial of service
                - Limited privilege escalation
                
                Recommended: Address in next maintenance window
            """.trimIndent()
            "LOW" -> """
                Low: Minor security issue
                This vulnerability has limited impact:
                - Minor information disclosure
                - Accessibility issues
                - Low-severity exposure
                
                Recommended: Address when convenient
            """.trimIndent()
            else -> "Review this vulnerability and plan appropriate remediation"
        }
    }
    
    /**
     * Get remediation steps
     */
    fun getRemediationSteps(vulnerability: Vulnerability): List<String> {
        return when {
            vulnerability.fixedVersions.isNotEmpty() -> listOf(
                "1. Upgrade to version: ${vulnerability.fixedVersions.firstOrNull() ?: "latest"}",
                "2. Run dependency update: gradle build --refresh-dependencies",
                "3. Verify build succeeds",
                "4. Run tests to ensure compatibility"
            )
            vulnerability.affectedVersions.isNotEmpty() -> listOf(
                "1. Check affected versions: ${vulnerability.affectedVersions.joinToString(", ")}",
                "2. Identify version in use",
                "3. Upgrade to patched version",
                "4. Verify remediation"
            )
            else -> listOf(
                "1. Review vulnerability details",
                "2. Check if your code is affected",
                "3. Apply recommended workaround",
                "4. Monitor for updates"
            )
        }
    }
    
    /**
     * Get learning resources
     */
    fun getLearningResources(): List<LearningResource> {
        return listOf(
            LearningResource(
                title = "Understanding OWASP Top 10",
                url = "https://owasp.org/www-project-top-ten/",
                type = "article"
            ),
            LearningResource(
                title = "Secure Dependency Management",
                url = "https://docs.github.com/en/code-security/supply-chain-security",
                type = "tutorial"
            ),
            LearningResource(
                title = "CVE Identification Guide",
                url = "https://cve.mitre.org/",
                type = "guide"
            ),
            LearningResource(
                title = "License Compliance Best Practices",
                url = "https://spdx.dev/",
                type = "article"
            )
        )
    }
    
    /**
     * Get quick fix context help
     */
    fun getQuickFixHelp(fixType: String): String {
        return when (fixType.uppercase()) {
            "UPGRADE" -> """
                Upgrade the vulnerable dependency to a patched version.
                
                Benefits:
                - Fixes the security vulnerability
                - May include new features
                - May include performance improvements
                
                Considerations:
                - May introduce breaking changes
                - Test thoroughly before deploying
            """.trimIndent()
            "SUPPRESS" -> """
                Suppress the vulnerability warning for this specific case.
                
                Use when:
                - The vulnerability is not exploitable in your context
                - You're using a workaround
                - The dependency is not actually used
                
                Note: This only suppresses the warning, doesn't fix the issue.
            """.trimIndent()
            "IGNORE" -> """
                Ignore this package globally in your project settings.
                
                Use when:
                - The package is only used in development
                - The package is from a trusted source
                - You have alternative security controls
                
                Note: This applies to all instances of this package.
            """.trimIndent()
            else -> "Apply the recommended fix for this vulnerability"
        }
    }
}

/**
 * Data class for learning resource
 */
data class LearningResource(
    val title: String,
    val url: String,
    val type: String = "article"
)
