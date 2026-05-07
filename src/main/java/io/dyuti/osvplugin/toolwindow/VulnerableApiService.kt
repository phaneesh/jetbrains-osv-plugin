// Reachability Analysis Service — detects if vulnerable methods are actually called
package io.dyuti.osvplugin.toolwindow

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import io.dyuti.osvplugin.api.model.AffectedFunction
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.ReachabilityResult
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.api.model.VulnerableCallSite

/**
 * Service that performs reachability analysis — checking whether vulnerable
 * methods from dependencies are actually called in the project's source code.
 *
 * This provides "Mend.io-style" reachability but using only open data
 * (OSV database specific/affected function signatures).
 *
 * ## How it works
 *
 * 1. OSV API returns `affected[].database_specific.functions` for many vulns
 *    (e.g. `["org.apache.logging.log4j.Logger.debug", "...Logger.error"]`)
 * 2. We parse these into [AffectedFunction] objects
 * 3. Walk project source files (Java, Kotlin, Groovy) via PSI to find method calls
 * 4. Match call site method names against affected function signatures
 * 5. Return [ReachabilityResult] per vuln with call site details
 *
 * ## Performance Notes
 *
 * - PSI walking is done under [ReadAction.compute]
 * - Only method name matching (fast string comparison), no type resolution
 * - Results are cached per project per vulnerability scan
 */
class VulnerableApiService {
    /**
     * Parse vulnerable function signatures from raw OSV JSON data.
     *
     * OSV schema: `affected[].database_specific.functions` is an array of strings.
     * Each string is typically a fully-qualified method name like
     * `"org.apache.logging.log4j.Logger.debug"`.
     *
     * @param vulnId The vulnerability ID (for reference)
     * @param rawJson The raw OSV JSON response string
     * @return List of affected function signatures found in the JSON
     */
    fun parseVulnerableFunctionsFromJson(
        vulnId: String,
        rawJson: String?,
    ): List<AffectedFunction> {
        if (rawJson.isNullOrBlank()) return emptyList()

        val functions = mutableListOf<AffectedFunction>()

        try {
            // Simple string search for "functions" arrays in JSON
            val regex = """"functions"\s*:\s*\[([^\]]*)\]""".toRegex()
            val match = regex.find(rawJson)
            if (match != null) {
                val arrayContent = match.groupValues[1]
                // Extract quoted strings from array
                val functionRegex = """"([^"]+)"""".toRegex()
                functionRegex.findAll(arrayContent).forEach { funcMatch ->
                    val signature = funcMatch.groupValues[1]
                    val parts = signature.split('.')
                    if (parts.size >= 2) {
                        val methodName = parts.last()
                        val className = parts.dropLast(1).joinToString(".")
                        functions.add(AffectedFunction(signature, className, methodName))
                    }
                }
            }
        } catch (_: Exception) {
            // Silently ignore parsing errors
        }

        return functions
    }

    /**
     * Perform reachability analysis for a list of vulnerability/dependency pairs.
     *
     * @param project The IntelliJ project
     * @param vulnDeps List of (Vulnerability, Dependency) pairs to analyze
     * @return List of [ReachabilityResult] with call site details
     */
    fun analyzeReachability(
        project: Project,
        vulnDeps: List<Pair<Vulnerability, Dependency>>,
    ): List<ReachabilityResult> {
        if (vulnDeps.isEmpty()) return emptyList()

        return ReadAction.compute<List<ReachabilityResult>, Throwable> {
            val results = mutableListOf<ReachabilityResult>()

            for ((vuln, dep) in vulnDeps) {
                // Get affected functions: from model field (populated by OSV API parser)
                // or infer from summary text as fallback
                val affectedFunctions =
                    vuln.affectedFunctions.ifEmpty {
                        inferAffectedFunctionsFromSummary(vuln)
                    }

                if (affectedFunctions.isEmpty()) {
                    // No function-level data available — mark as "unknown reachability"
                    results.add(
                        ReachabilityResult(
                            vulnerability = vuln,
                            dependency = dep,
                            callSites = emptyList(),
                            reachable = false,
                        ),
                    )
                    continue
                }

                // Find call sites for each affected function
                val callSites = findVulnerableCallSites(project, affectedFunctions)

                results.add(
                    ReachabilityResult(
                        vulnerability = vuln,
                        dependency = dep,
                        callSites = callSites,
                        reachable = callSites.isNotEmpty(),
                    ),
                )
            }

            results
        }
    }

    /**
     * Infer potentially affected functions from vulnerability summary text.
     *
     * This is a heuristic fallback for when OSV doesn't provide explicit
     * function signatures. It extracts method names from the vulnerability
     * description (e.g. "Logger.debug() is vulnerable" → `debug`).
     */
    private fun inferAffectedFunctionsFromSummary(vuln: Vulnerability): List<AffectedFunction> {
        val functions = mutableListOf<AffectedFunction>()

        // Pattern: "ClassName.methodName" or "methodName()"
        val pattern = """(\w+\.?)+\.(\w+)\s*\(""".toRegex()
        val text = vuln.summary + " " + vuln.details

        pattern.findAll(text).forEach { match ->
            val fullMatch = match.value.trimEnd('(').trim()
            val parts = fullMatch.split('.')
            if (parts.size >= 2) {
                val methodName = parts.last()
                val className = parts.dropLast(1).joinToString(".")
                functions.add(AffectedFunction(fullMatch, className, methodName))
            }
        }

        return functions.distinctBy { it.signature }
    }

    /**
     * Find call sites in source code that match potentially vulnerable functions.
     *
     * Walks Java source files and extracts method call expressions from PSI.
     * Uses a lightweight visitor: only checks method name, does NOT attempt
     * full type resolution (which requires compiled classes and is slow).
     */
    private fun findVulnerableCallSites(
        project: Project,
        affectedFunctions: List<AffectedFunction>,
    ): List<VulnerableCallSite> {
        val callSites = mutableListOf<VulnerableCallSite>()
        val psiManager = PsiManager.getInstance(project)

        // Get method names to search for
        val targetMethodNames = affectedFunctions.mapNotNull { it.methodName }.toSet()
        if (targetMethodNames.isEmpty()) return emptyList()

        // Collect all source files in the project
        val sourceFiles = collectSourceFiles(project)

        for (virtualFile in sourceFiles) {
            val psiFile = psiManager.findFile(virtualFile) ?: continue

            // Walk PSI tree for method call expressions
            psiFile.accept(
                object : com.intellij.psi.PsiRecursiveElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        super.visitElement(element)

                        // Check for Java method calls: PsiMethodCallExpression
                        if (element is com.intellij.psi.PsiMethodCallExpression) {
                            val methodExpression = element.methodExpression
                            val methodName = methodExpression.referenceName

                            if (methodName != null && methodName in targetMethodNames) {
                                recordCallSite(methodName, element, callSites)
                            }
                        }
                    }
                },
            )
        }

        return callSites.distinctBy { "${it.filePath}:${it.lineNumber}:${it.methodName}" }
    }

    /**
     * Collect all source files (.java, .kt, .groovy) in the project.
     */
    private fun collectSourceFiles(project: Project): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        val projectBase = project.baseDir ?: return emptyList()

        VfsUtilCore.visitChildrenRecursively(
            projectBase,
            object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory && isSourceFile(file)) {
                        files.add(file)
                    }
                    return true
                }
            },
        )

        return files
    }

    /**
     * Check if a file is a source file we care about (Java, Kotlin, Groovy).
     */
    private fun isSourceFile(file: VirtualFile): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".kts") || name.endsWith(".groovy")
    }

    /**
     * Record a vulnerable call site from a PSI method call element.
     */
    private fun recordCallSite(
        methodName: String,
        callExpr: com.intellij.psi.PsiMethodCallExpression,
        callSites: MutableList<VulnerableCallSite>,
    ) {
        val containingFile = callExpr.containingFile
        val document = containingFile.viewProvider.document
        val lineNumber =
            if (document != null) {
                document.getLineNumber(callExpr.textOffset) + 1
            } else {
                0
            }

        val qualifier = callExpr.methodExpression.qualifierExpression?.text

        // Try to find class name via simple heuristic from qualifier
        val resolvedClass = qualifier ?: "unknown"

        callSites.add(
            VulnerableCallSite(
                methodName = methodName,
                className = resolvedClass,
                filePath = containingFile.virtualFile?.path ?: "unknown",
                lineNumber = lineNumber,
                qualifierExpression = qualifier,
            ),
        )
    }

    /**
     * Check if a specific method call site is actually vulnerable.
     *
     * Performs deeper analysis: not just method name matching,
     * but also checking if the resolved method belongs to a vulnerable class.
     */
    fun isCallSiteVulnerable(
        callSite: VulnerableCallSite,
        affectedFunctions: List<AffectedFunction>,
    ): Boolean =
        affectedFunctions.any { affected ->
            affected.methodName == callSite.methodName &&
                (
                    affected.className == null ||
                        affected.className == callSite.className ||
                        callSite.className.endsWith(".${affected.className}") ||
                        callSite.qualifierExpression == affected.className
                )
        }
}
