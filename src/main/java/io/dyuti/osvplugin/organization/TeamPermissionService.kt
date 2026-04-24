// Team Permission Service
package io.dyuti.osvplugin.organization

/**
 * Team Permission Service for managing team-based access control
 */
class TeamPermissionService(private val organizationManager: OrganizationManager) {
    
    /**
     * Check if member can view organization
     */
    fun canViewOrg(orgId: String, memberId: String): Boolean {
        return hasPermission(orgId, memberId, Permission.VIEW)
    }
    
    /**
     * Check if member can analyze vulnerabilities
     */
    fun canAnalyze(orgId: String, memberId: String): Boolean {
        return hasPermission(orgId, memberId, Permission.ANALYZE)
    }
    
    /**
     * Check if member can configure settings
     */
    fun canConfigure(orgId: String, memberId: String): Boolean {
        return hasPermission(orgId, memberId, Permission.CONFIGURE)
    }
    
    /**
     * Check if member can approve license exceptions
     */
    fun canApprove(orgId: String, memberId: String): Boolean {
        return hasPermission(orgId, memberId, Permission.APPROVE)
    }
    
    /**
     * Check if member is admin
     */
    fun isAdmin(orgId: String, memberId: String): Boolean {
        return hasPermission(orgId, memberId, Permission.ADMIN)
    }
    
    /**
     * Check if member has specific permission
     */
    fun hasPermission(orgId: String, memberId: String, permission: Permission): Boolean {
        return organizationManager.hasPermission(orgId, memberId, permission)
    }
    
    /**
     * Get member's effective permissions
     */
    fun getEffectivePermissions(orgId: String, memberId: String): Set<Permission> {
        val member = organizationManager.getOrganization(orgId)?.members?.find { it.id == memberId }
            ?: return emptySet()
        
        // Member's direct permissions
        val permissions = member.permissions.toMutableSet()
        
        // Add permissions from teams
        val teams = organizationManager.getOrganizationTeams(orgId)
        for (team in teams) {
            if (member.id in team.members) {
                permissions.addAll(team.permissions)
            }
        }
        
        // Admin gets all permissions
        if (member.role == Role.ADMIN) {
            permissions.addAll(Permission.values().toList())
        }
        
        return permissions
    }
    
    /**
     * Get members by permission
     */
    fun getMembersByPermission(orgId: String, permission: Permission): List<Member> {
        return organizationManager.getOrganization(orgId)?.members?.filter { member ->
            hasPermission(orgId, member.id, permission)
        } ?: emptyList()
    }
    
    /**
     * Check if user has any admin-level access
     */
    fun hasAdminAccess(orgId: String, memberId: String): Boolean {
        return isAdmin(orgId, memberId) || canConfigure(orgId, memberId)
    }
}
