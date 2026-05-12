package io.dyuti.osvplugin.historical

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability

/**
 * Callback invoked when a vulnerability scan completes.
 * Used to decouple the scan panel from the historical trend panel.
 */
fun interface ScanCompletionListener {
    fun onScanCompleted(
        vulnerabilities: List<Vulnerability>,
        dependencies: List<Dependency>,
    )
}
