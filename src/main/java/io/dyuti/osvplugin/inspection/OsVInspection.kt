// OSV Vulnerability Scanner PSI Inspection with Async Dependency Scanning
package io.dyuti.osvplugin.inspection

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.api.OsVApiService
import io.dyuti.osvplugin.api.model.*
import io.dyuti.osvplugin.config.OsVConfig
import io.dyuti.osvplugin.parser.*
import io.dyuti.osvplugin.utils.SeverityUtil
import java.util.*
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
open class OsVInspection : LocalInspectionTool() {
    companion object {
        private val LOG = Logger.getInstance(OsVInspection::class.java)
    }

    private val parsers: List<DependencyParser> =
        listOf(
            MavenParser(),
            GradleParser(),
            NpmParser(),
            PipParser(),
            GoParser(),
            CargoParser(),
            ComposerParser(),
            GemfileParser(),
            PubspecParser(),
            NugetParser(),
            StackParser(),
            MixParser(),
            RenvParser(),
            ConanParser(),
            YarnParser(),
            PoetryParser(),
        )

    private val apiService by lazy {
        try {
            OsVApiService.getInstance()
        } catch (_: Exception) {
            OsVApiService()
        }
    }

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
                            LOG.error("OSV: Error scanning file $fileName", e)
                        }
                    }

                    // Only cache if file hasn't been modified since we started
                    ApplicationManager.getApplication().invokeLater {
                        if (file.modificationStamp == expectedStamp && !file.project.isDisposed) {
                            fileCache[filePath] = VulnerabilityCacheEntry(results, expectedStamp)

                            // Restart highlighting so the daemon re-runs buildVisitor()
                            // and cached vulnerabilities are painted in the editor.
                            if (!file.project.isDisposed) {
                                DaemonCodeAnalyzer.getInstance(file.project).restart()
                            }
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
            try {
                val app =
                    ApplicationManager
                        .getApplication()
                if (app != null) app.getService(OsVConfig::class.java) else OsVConfig()
            } catch (_: Exception) {
                OsVConfig()
            }
        return SeverityUtil.meetsThreshold(vuln.severity, config.minimumSeverity)
    }

    /**
     * Register a single vulnerability problem at the exact line where the
     * dependency is declared.  Uses either the leaf [PsiElement] at that
     * offset (for structured files) or a [TextRange] covering the whole line
     * (for plain-text files) so the red underline appears in the editor *and*
     * the entry is navigable from the Problems view (Alt+6).
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
                append("OSV: ${vuln.displayId()}")
                if (vuln.cvssScore != null) {
                    append(" (CVSS: ${vuln.cvssScore})")
                }
                append(" — ${vuln.summary}")
                if (vuln.fixedVersions.isNotEmpty()) {
                    append(" [Fix: ${vuln.formatFixVersions()}]")
                }
            }

        // Try to find the most specific element / text-range for the line.
        val (targetElement, rangeInElement) = resolveHighlightTarget(file, dep)

        if (targetElement != null && rangeInElement != null) {
            // Specific line range — shows red underline + gutter marker
            holder.registerProblem(
                targetElement,
                message,
                highlightType,
                rangeInElement,
                upgradeFix,
                suppressFix,
                ignoreFix,
            )
        } else {
            // Fallback: file-level problem (Problems view only, no editor redline)
            holder.registerProblem(
                file,
                message,
                highlightType,
                upgradeFix,
                suppressFix,
                ignoreFix,
            )
        }
    }

    /**
     * Resolve a concrete [PsiElement] and [TextRange] for the dependency's
     * declared line so the inspection paints a red underline in the editor.
     *
     * Looks for the dependency's name inside the line text itself (the user
     * literally sees the same text) so that the match is recognisable and
     * independent of PSI structure.
     */
    private fun resolveHighlightTarget(
        file: PsiFile,
        dep: Dependency,
    ): Pair<PsiElement?, TextRange?> {
        val lineNumber = dep.lineNumber
        if (lineNumber == null || lineNumber < 1) return Pair(null, null)

        val doc =
            PsiDocumentManager.getInstance(file.project).getDocument(file)
                ?: return Pair(null, null)
        if (lineNumber > doc.lineCount) return Pair(null, null)

        val lineIdx = lineNumber - 1
        val lineStart = doc.getLineStartOffset(lineIdx)
        val lineEnd = doc.getLineEndOffset(lineIdx).coerceAtMost(doc.textLength)
        val lineText = doc.getText(TextRange.create(lineStart, lineEnd))
        if (lineText.isBlank()) return Pair(null, null)

        // Candidate search terms ordered from most specific to least specific
        val searchTerms =
            buildList {
                add(dep.name.substringAfterLast(':')) // artifact-id
                add(dep.name.substringBeforeLast(':')) // group-id
                add(dep.name)
                add(dep.name.substringAfterLast('/')) // npm scoped
            }.filter { it.isNotBlank() && it.length >= 2 }.distinct()

        // 1) Try to find the dependency name on this exact line
        for (term in searchTerms) {
            val idx = lineText.indexOf(term)
            if (idx < 0) continue
            val absStart = lineStart + idx
            val absEnd = (absStart + term.length).coerceAtMost(file.textLength)
            val matchRange = TextRange(absStart, absEnd)

            // Walk up from a leaf to find an element covering the match
            var element: PsiElement? = file.findElementAt(absStart)
            while (element != null && element != file) {
                val er = element.textRange
                if (er != null && er.startOffset <= absStart && er.endOffset >= absEnd) {
                    val rangeInElement = TextRange(absStart - er.startOffset, absEnd - er.startOffset)
                    return Pair(element, rangeInElement.takeIf { !it.isEmpty })
                }
                element = element.parent
            }
            // No PSI element covers the text — still return file-level with exact range
            return Pair(file, matchRange)
        }

        // 2) Fallback: first non-whitespace token on the line
        val firstNonWs = lineText.indexOfFirst { !it.isWhitespace() }
        if (firstNonWs >= 0) {
            val offset = lineStart + firstNonWs
            var element: PsiElement? = file.findElementAt(offset)
            while (element != null && element != file) {
                val er = element.textRange
                if (er != null && er.startOffset >= lineStart && er.endOffset <= lineEnd) {
                    val rangeInElement = TextRange(0, er.length)
                    return Pair(element, rangeInElement)
                }
                element = element.parent
            }
        }

        // 3) Last resort: file with full line range (editor may not show redline)
        return Pair(file, TextRange(lineStart, lineEnd).takeIf { !it.isEmpty })
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

/**
 * Language-specific subclasses to satisfy IntelliJ's shortName enforcement
 * (getShortName() must match the ep.shortName in plugin.xml).
 * Each extends the core [OsVInspection] logic with the correct short name.
 */
class OsVInspectionXml : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionXml"
}

class OsVInspectionGroovy : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionGroovy"
}

class OsVInspectionKotlin : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionKotlin"
}

class OsVInspectionJson : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionJson"
}

class OsVInspectionPlainText : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionPlainText"
}

class OsVInspectionPython : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionPython"
}

class OsVInspectionJavaScript : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionJavaScript"
}

class OsVInspectionTypeScript : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionTypeScript"
}

class OsVInspectionGo : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionGo"
}

class OsVInspectionYaml : OsVInspection() {
    override fun getShortName(): String = "OsVInspectionYaml"
}
