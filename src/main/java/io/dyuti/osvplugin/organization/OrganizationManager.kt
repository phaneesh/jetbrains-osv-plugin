// Organization Management Service
package io.dyuti.osvplugin.organization

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import io.dyuti.osvplugin.api.model.Vulnerability
import java.util.concurrent.ConcurrentHashMap

/**
 * Organization Manager for multi-tenant support
 * Manages multiple organizations and their configurations
 */
class OrganizationManager(private val project: Project) {
    
    private val organizations = ConcurrentHashMap<String, Organization>()
    private var currentOrganization: Organization? = null
    
    companion object {
        @JvmStatic
        fun getInstance(project: Project): OrganizationManager {
            return ServiceManager.getService(project, OrganizationManager::class.java)
                ?: OrganizationManager(project)
        }
    }
    
    /**
     * Add a new organization
     */
    fun addOrganization(org: Organization): Organization {
        organizations[org.id] = org
        if (currentOrganization == null) {
            currentOrganization = org
        }
        return org
    }
    
    /**
     * Get organization by ID
     */
    fun getOrganization(orgId: String): Organization? {
        return organizations[orgId]
    }
    
    /**
     * Get current active organization
     */
    fun getCurrentOrganization(): Organization? {
        return currentOrganization ?: organizations.values.firstOrNull()
    }
    
    /**
     * Set current organization
     */
    fun setCurrentOrganization(orgId: String): Boolean {
        val org = organizations[orgId] ?: return false
        currentOrganization = org
        return true
    }
    
    /**
     * Remove organization
     */
    fun removeOrganization(orgId: String): Boolean {
        if (currentOrganization?.id == orgId) {
            currentOrganization = null
        }
        return organizations.remove(orgId) != null
    }
    
    /**
     * Get all organizations
     */
    fun getAllOrganizations(): List<Organization> {
        return organizations.values.toList()
    }
    
    /**
     * Check if organization exists
     */
    fun hasOrganization(orgId: String): Boolean {
        return organizations.containsKey(orgId)
    }
    
    /**
     * Filter vulnerabilities by organization
     */
    fun filterByOrganization(
        vulnerabilities: List<Vulnerability>,
        orgId: String?
    ): List<Vulnerability> {
        val org = orgId?.let { getOrganization(it) } ?: return vulnerabilities
        
        // Filter based on organization policies
        return vulnerabilities.filter { vuln ->
            !org.isIgnored(vuln.id) && 
            org.licensePolicy.isLicenseCompliant(vuln)
        }
    }
    
    /**
     * Get organization teams
     */
    fun getOrganizationTeams(orgId: String): List<Team> {
        return getOrganization(orgId)?.teams ?: emptyList()
    }
    
    /**
     * Get organization members
     */
    fun getOrganizationMembers(orgId: String): List<Member> {
        return getOrganization(orgId)?.members ?: emptyList()
    }
    
    /**
     * Check user permissions
     */
    fun hasPermission(orgId: String, memberId: String, permission: Permission): Boolean {
        val org = getOrganization(orgId) ?: return false
        val member = org.members.find { it.id == memberId } ?: return false
        return member.permissions.contains(permission)
    }
}

/**
 * Organization data class
 */
data class Organization(
    val id: String,
    val name: String,
    val description: String = "",
    val members: List<Member> = emptyList(),
    val teams: List<Team> = emptyList(),
    val licensePolicy: LicensePolicy = LicensePolicy(),
    val ignoreRules: List<String> = emptyList()
) {
    fun isIgnored(vulnId: String): Boolean {
        return ignoreRules.any { vulnId.contains(it, ignoreCase = true) }
    }
}

/**
 * Team data class
 */
data class Team(
    val id: String,
    val name: String,
    val members: List<String> = emptyList(),
    val permissions: List<Permission> = emptyList(),
    val scope: Scope = Scope.PROJECT
)

/**
 * Member data class
 */
data class Member(
    val id: String,
    val name: String,
    val email: String,
    val role: Role = Role.VIEWER,
    val permissions: List<Permission> = emptyList()
)

/**
 * License Policy for organization
 */
data class LicensePolicy(
    val allowedLicenses: List<String> = listOf("MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause"),
    val deniedLicenses: List<String> = listOf("GPL-2.0", "GPL-3.0", "AGPL-3.0", "SSPL-1.0"),
    val requireLicenseApproval: Boolean = false,
    val strictMode: Boolean = false
) {
    fun isLicenseCompliant(vuln: Vulnerability): Boolean {
        if (strictMode) {
            // In strict mode, only allow explicitly approved licenses
            return allowedLicenses.any { vuln.id.contains(it, ignoreCase = true) }
        }
        // In normal mode, deny only explicitly denied licenses
        return !deniedLicenses.any { vuln.id.contains(it, ignoreCase = true) }
    }
    
    fun isLicenseAllowed(license: String): Boolean {
        return allowedLicenses.any { license.contains(it, ignoreCase = true) } &&
            !deniedLicenses.any { license.contains(it, ignoreCase = true) }
    }
}

/**
 * Permission enum
 */
enum class Permission {
    VIEW,
    ANALYZE,
    CONFIGURE,
    APPROVE,
    ADMIN
}

/**
 * Role enum
 */
enum class Role {
    VIEWER,      // Read-only access
    ANALYST,     // Can view and analyze
    MANAGER,     // Can configure and approve
    ADMIN        // Full access
}

/**
 * Scope enum
 */
enum class Scope {
    PROJECT,     // Project-level access
    MODULE,      // Module-level access
    FILE,        // File-level access
    GLOBAL       // Global access
}
