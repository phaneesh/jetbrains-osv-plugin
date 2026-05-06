// SPDX License Scanner Integration - Settings
package io.dyuti.osvplugin.license

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * License policy configuration
 */
@State(
    name = "OsVLicensePolicy",
    storages = [Storage("osv-license-policy.xml")],
)
class LicensePolicyConfig : PersistentStateComponent<LicensePolicyConfig> {
    var allowedLicenses: List<String> = emptyList()
    var blockedLicenses: List<String> = emptyList()
    var copyleftAllowList: List<String> = emptyList()
    var strictMode: Boolean = true

    companion object {
        @Suppress("DEPRECATION")
        fun getInstance(): LicensePolicyConfig =
            com.intellij.openapi.components.ServiceManager
                .getService(LicensePolicyConfig::class.java)
    }

    override fun getState(): LicensePolicyConfig = this

    override fun loadState(state: LicensePolicyConfig) {
        XmlSerializerUtil.copyBean(state, this)
        // Ensure strictMode is properly loaded
        strictMode = state.strictMode
    }

    /**
     * Check if license is allowed
     */
    fun isLicenseAllowed(license: String): Boolean {
        if (blockedLicenses.contains(license)) return false
        if (allowedLicenses.isEmpty()) return true
        return allowedLicenses.contains(license)
    }

    /**
     * Check if license is blocked
     */
    fun isLicenseBlocked(license: String): Boolean = blockedLicenses.contains(license)

    /**
     * Check if copyleft license is allowed
     */
    fun isCopyleftAllowed(license: String): Boolean {
        if (copyleftAllowList.isEmpty()) return false
        return copyleftAllowList.contains(license)
    }
}
