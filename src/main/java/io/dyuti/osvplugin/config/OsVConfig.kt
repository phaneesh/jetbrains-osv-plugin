package io.dyuti.osvplugin.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import io.dyuti.osvplugin.api.model.OsVSeverity

@State(
    name = "OsVConfig",
    storages = [Storage("osv-config.xml")],
)
class OsVConfig : PersistentStateComponent<OsVConfig> {
    var minimumSeverity: OsVSeverity = OsVSeverity.MEDIUM
    var inspectionEnabled: Boolean = true
    var cacheTtl: Int = 1
    var rateLimitEnabled: Boolean = true
    var rateLimitRequestsPerHour: Int = 100
    var scanDirectDependencies: Boolean = true
    var scanTransitiveDependencies: Boolean = true
    var githubAdvisoryEnabled: Boolean = false
    var githubToken: String? = null
    var licenseScanningEnabled: Boolean = false
    var allowedLicenses: List<String> = listOf("MIT", "Apache-2.0", "BSD-3-Clause")
    var focusModeEnabled: Boolean = false
    var baseBranch: String = "main"
    var sarifExportPath: String? = null
    var osvApiUrl: String = "https://api.osv.dev/v1/query"

    var ignoredPackages: List<String> = listOf()

    // Privacy-preserving mode
    var privacyPreservingEnabled: Boolean = false
    var privacySalt: String? = null

    // Organization Management
    var orgManagementEnabled: Boolean = false
    var currentOrganization: String? = null

    // Jira Integration
    var jiraEnabled: Boolean = false
    var jiraBaseUrl: String? = null
    var jiraProjectKey: String? = null
    var jiraEmail: String? = null
    var jiraToken: String? = null

    override fun getState(): OsVConfig = this

    override fun loadState(state: OsVConfig) {
        minimumSeverity = state.minimumSeverity
        inspectionEnabled = state.inspectionEnabled
        cacheTtl = state.cacheTtl
        rateLimitEnabled = state.rateLimitEnabled
        rateLimitRequestsPerHour = state.rateLimitRequestsPerHour
        scanDirectDependencies = state.scanDirectDependencies
        scanTransitiveDependencies = state.scanTransitiveDependencies
        githubAdvisoryEnabled = state.githubAdvisoryEnabled
        githubToken = state.githubToken
        licenseScanningEnabled = state.licenseScanningEnabled
        allowedLicenses = state.allowedLicenses
        focusModeEnabled = state.focusModeEnabled
        baseBranch = state.baseBranch
        sarifExportPath = state.sarifExportPath
        osvApiUrl = state.osvApiUrl
        ignoredPackages = state.ignoredPackages
        orgManagementEnabled = state.orgManagementEnabled
        currentOrganization = state.currentOrganization
        jiraEnabled = state.jiraEnabled
        jiraBaseUrl = state.jiraBaseUrl
        jiraProjectKey = state.jiraProjectKey
        jiraEmail = state.jiraEmail
        jiraToken = state.jiraToken
    }

    companion object {
        fun getInstance(): OsVConfig = service<OsVConfig>()
    }
}
