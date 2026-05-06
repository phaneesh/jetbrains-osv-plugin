// OSV IntelliJ Plugin - OSV Vulnerability Scanner
package io.dyuti.osvplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.dyuti.osvplugin.config.OsVConfig

/**
 * Main plugin entry point
 */
@State(
    name = "OsVPlugin",
    storages = [Storage("osv-plugin.xml")],
)
class OsVPlugin : PersistentStateComponent<OsVConfig> {
    private var config: OsVConfig = OsVConfig()

    override fun getState(): OsVConfig = config

    override fun loadState(state: OsVConfig) {
        config = state
    }

    companion object {
        // Plugin instance management is handled by IntelliJ Platform
    }
}
