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
    var cacheTtl: Int = 24
    var rateLimitEnabled: Boolean = true
    var rateLimitRequestsPerHour: Int = 1000
    var scanDirectDependencies: Boolean = true
    var scanTransitiveDependencies: Boolean = true
    var githubAdvisoryEnabled: Boolean = false
    var licenseScanningEnabled: Boolean = false
    var allowedLicenses: List<String> = listOf("MIT", "Apache-2.0", "BSD-3-Clause")
    var focusModeEnabled: Boolean = false
    var baseBranch: String = "main"
    var sarifExportPath: String? = null
    var osvApiUrl: String = "https://api.osv.dev/v1/query"

    var ignoredPackages: List<String> = listOf()

    // Privacy-preserving mode
    var privacyPreservingEnabled: Boolean = false

    // Organization Management
    var orgManagementEnabled: Boolean = false
    var currentOrganization: String? = null

    // Jira Integration
    var jiraEnabled: Boolean = false
    var jiraBaseUrl: String? = null
    var jiraProjectKey: String? = null
    var jiraEmail: String? = null

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
    }

    companion object {
        private const val GITHUB_TOKEN_KEY = "osv.github.token"
        private const val JIRA_TOKEN_KEY = "osv.jira.token"
        private const val PRIVACY_SALT_KEY = "osv.privacy.salt"

        private val fallbackStore = java.util.concurrent.ConcurrentHashMap<String, String?>()

        fun getInstance(): OsVConfig = service<OsVConfig>()

        fun getGithubToken(): String? = getSecurePassword(GITHUB_TOKEN_KEY)

        fun setGithubToken(token: String?) = setSecurePassword(GITHUB_TOKEN_KEY, token)

        fun getJiraToken(): String? = getSecurePassword(JIRA_TOKEN_KEY)

        fun setJiraToken(token: String?) = setSecurePassword(JIRA_TOKEN_KEY, token)

        fun getPrivacySalt(): String? = getSecurePassword(PRIVACY_SALT_KEY)

        fun setPrivacySalt(salt: String?) = setSecurePassword(PRIVACY_SALT_KEY, salt)

        private fun getSecurePassword(key: String): String? =
            try {
                val app =
                    com.intellij.openapi.application.ApplicationManager
                        .getApplication()
                if (app != null) {
                    val attrs = com.intellij.credentialStore.CredentialAttributes("io.dyuti.osvplugin", key)
                    com.intellij.ide.passwordSafe.PasswordSafe.instance
                        .getPassword(attrs)
                } else {
                    fallbackStore[key]
                }
            } catch (_: Exception) {
                fallbackStore[key]
            }

        private fun setSecurePassword(
            key: String,
            value: String?,
        ) {
            try {
                val app =
                    com.intellij.openapi.application.ApplicationManager
                        .getApplication()
                if (app != null) {
                    val attrs = com.intellij.credentialStore.CredentialAttributes("io.dyuti.osvplugin", key)
                    com.intellij.ide.passwordSafe.PasswordSafe.instance
                        .setPassword(attrs, value)
                } else {
                    if (value != null) fallbackStore[key] = value else fallbackStore.remove(key)
                }
            } catch (_: Exception) {
                if (value != null) fallbackStore[key] = value else fallbackStore.remove(key)
            }
        }
    }
}
