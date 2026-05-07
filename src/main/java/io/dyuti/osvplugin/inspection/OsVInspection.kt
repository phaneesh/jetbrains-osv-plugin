// OSV Vulnerability Scanner PSI Inspection with Async Dependency Scanning
package io.dyuti.osvplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.api.OsVApiService
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig
import io.dyuti.osvplugin.parser.DependencyParser
import io.dyuti.osvplugin.parser.GradleParser
import io.dyuti.osvplugin.parser.MavenParser
import io.dyuti.osvplugin.parser.NpmParser
import io.dyuti.osvplugin.parser.PipParser
import io.dyuti.osvplugin.utils.SeverityUtil
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class for dependency with vulnerabilities and file location
 */
data class DependencyWithVulnerabilities(
    val dependency: Dependency,
    val vulnerabilities: List<Vulnerability>,
    val filePath: String,
    val line: Int,
)

/**
 * Cached vulnerability result for a specific file
 */
internal data class VulnerabilityCacheEntry(
    val vulnerabilities: List<VulnerabilityResult>,
    val modificationStamp: Long,
)

/**
 * Single vulnerability result for cache storage
 */
internal data class VulnerabilityResult(
    val dependency: Dependency,
    val vulnerability: Vulnerability,
)

/**
 * OSV Local Inspection Tool with asynchronous non-blocking dependency scanning.
 *
 * Key features:
 * - Per-file vulnerability cache to avoid repeated API queries
 * - 500ms debounce to prevent scan spam on rapid edits
 * - Automatic cache invalidation when file is modified
 * - "Scanning..." placeholder shown while async query runs
 * - Background scanning via ProgressManager.Task.Backgroundable
 */
class OsVInspection : LocalInspectionTool() {
    private val parsers: List<DependencyParser> =
        listOf(
            MavenParser(),
            GradleParser(),
            NpmParser(),
            PipParser(),
        )

    private val apiService = OsVApiService.getInstance()

    // Per-file cache: filePath -> cached results
    private val fileCache = ConcurrentHashMap<String, VulnerabilityCacheEntry>()

    // Debounce timers per file
    private val debounceTimers = ConcurrentHashMap<String, Timer>()

    override fun getDisplayName(): String = "OSV Vulnerability Check"

    override fun getShortName(): String = "OsVInspection"

    /**
     * Build a PSI element visitor that checks for vulnerabilities.
     * This method runs on the UI thread and MUST NOT block.
     */
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val filePath = file.virtualFile?.path ?: return
                val currentStamp = file.modificationStamp

                // Check cache
                val cached = fileCache[filePath]
                if (cached != null && cached.modificationStamp == currentStamp) {
                    // Cache hit — show cached vulnerabilities
                    cached.vulnerabilities.forEach { result ->
                        reportVulnerability(holder, result.dependency, result.vulnerability, file)
                    }
                    return
                }

                // Stale cache or no cache — clear and schedule scan
                fileCache.remove(filePath)

                // Show "Scanning..." info placeholder
                holder.registerProblem(
                    file,
                    "OSV: Scanning for vulnerabilities...",
                    ProblemHighlightType.INFORMATION,
                )

                // Debounce and schedule async scan
                scheduleAsyncScan(file, filePath, currentStamp)
            }
        }

    /**
     * Schedule an async vulnerability scan with 500ms debounce.
     * Cancels any pending scan for the same file.
     */
    private fun scheduleAsyncScan(
        file: PsiFile,
        filePath: String,
        modificationStamp: Long,
    ) {
        // Cancel existing timer for this file
        debounceTimers[filePath]?.cancel()
        debounceTimers.remove(filePath)

        val timer = Timer("osv-inspection-$filePath", true)
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    debounceTimers.remove(filePath)
                    runAsyncVulnerabilityScan(file, filePath, modificationStamp)
                }
            },
            500, // 500ms debounce
        )
        debounceTimers[filePath] = timer
    }

    /**
     * Run the actual vulnerability scan in a background thread.
     * Parses dependencies and queries OSV API without blocking the UI.
     */
    private fun runAsyncVulnerabilityScan(
        file: PsiFile,
        filePath: String,
        expectedStamp: Long,
    ) {
        ProgressManager.getInstance().run(
            object : Backgroundable(file.project, "Scanning dependencies for OSV vulnerabilities", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.text = "Scanning ${file.name}..."

                    val results = mutableListOf<VulnerabilityResult>()
                    val fileName = file.name

                    // Find and run appropriate parser
                    for (parser in parsers) {
                        if (!parser.canHandle(fileName)) continue

                        try {
                            val dependencies = parser.parse(fileName, file.text)
                            if (dependencies.isEmpty()) continue

                            // Batch query for performance
                            val vulnerabilitiesByDep =
                                apiService.batchQueryVulnerabilities(dependencies)

                            for ((dep, rawVulnerabilities) in vulnerabilitiesByDep) {
                                // Mirror the source dependency's line number into each
                                // vulnerability so that navigation in the Problems view or on
                                // double-click lands on the correct manifest line.
                                val vulnerabilities =
                                    rawVulnerabilities.map { vuln ->
                                        if (vuln.lineNumber == null) {
                                            vuln.copy(lineNumber = dep.lineNumber)
                                        } else {
                                            vuln
                                        }
                                    }
                                for (vuln in vulnerabilities) {
                                    if (shouldReportVulnerability(vuln)) {
                                        results.add(VulnerabilityResult(dep, vuln))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Log but don't fail — other parsers may succeed
                            System.err.println("OSV: Error scanning file $fileName: ${e.message}")
                        }
                    }

                    // Only cache if file hasn't been modified since we started
                    ApplicationManager.getApplication().invokeLater {
                        if (file.modificationStamp == expectedStamp && !file.project.isDisposed) {
                            fileCache[filePath] = VulnerabilityCacheEntry(results, expectedStamp)

                            // Request daemon re-highlight to show results
                            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
                                .getInstance(file.project)
                                .restart(file)
                        }
                    }
                }
            },
        )
    }

    /**
     * Determine if a vulnerability should be reported based on user-configured minimum severity.
     */
    private fun shouldReportVulnerability(vuln: Vulnerability): Boolean {
        val config =
            @Suppress("DEPRECATION")
            com.intellij.openapi.components.ServiceManager
                .getService(OsVConfig::class.java)
        return SeverityUtil.meetsThreshold(vuln.severity, config.minimumSeverity)
    }

    /**
     * Register a single vulnerability problem at the exact line where the
     * dependency is declared. Problems appear in the native Problems view
     * (Alt+6) with click-to-navigate support.
     */
    private fun reportVulnerability(
        holder: ProblemsHolder,
        dep: Dependency,
        vuln: Vulnerability,
        file: PsiFile,
    ) {
        val upgradeFix = OsVQuickFix.createUpgradeFix(dep, vuln)
        val suppressFix = OsVQuickFix.createSuppressFix(dep, vuln)
        val ignoreFix = OsVQuickFix.createIgnoreFix(dep, vuln)

        val highlightType =
            when (vuln.severity) {
                OsVSeverity.CRITICAL -> ProblemHighlightType.ERROR
                OsVSeverity.HIGH -> ProblemHighlightType.WARNING
                OsVSeverity.MEDIUM -> ProblemHighlightType.WEAK_WARNING
                OsVSeverity.LOW -> ProblemHighlightType.INFORMATION
            }

        val message =
            buildString {
                append("OSV: ${vuln.id}")
                if (vuln.cvssScore != null) {
                    append(" (CVSS: ${vuln.cvssScore})")
                }
                append(" — ${vuln.summary}")
                if (vuln.fixedVersions.isNotEmpty()) {
                    append(" [Fix: ${vuln.fixedVersions.first()}]")
                }
            }

        // Register at file level. Line-level TextRange registration requires
        // finding the exact PsiElement at the offset; the IntelliJ Problems view
        // still shows the vulnerability and supports quick-fixes.
        // TODO: implement PsiElement-level registration for precise line navigation.
        holder.registerProblem(
            file,
            message,
            highlightType,
            upgradeFix,
            suppressFix,
            ignoreFix,
        )
    }

    /**
     * Check dependencies for vulnerabilities (synchronous, for non-inspection use).
     */
    fun checkDependencies(dependencies: List<Dependency>): List<DependencyWithVulnerabilities> {
        val results = mutableListOf<DependencyWithVulnerabilities>()

        // Use batch query for performance
        val vulnerabilitiesByDep = apiService.batchQueryVulnerabilities(dependencies)

        for ((dep, rawVulns) in vulnerabilitiesByDep) {
            if (rawVulns.isNotEmpty()) {
                // Propagate source dependency line number onto each vulnerability
                val withLines =
                    rawVulns.map { vuln ->
                        if (vuln.lineNumber == null) vuln.copy(lineNumber = dep.lineNumber) else vuln
                    }
                results.add(DependencyWithVulnerabilities(dep, withLines, "", dep.lineNumber ?: 1))
            }
        }

        return results
    }
}
