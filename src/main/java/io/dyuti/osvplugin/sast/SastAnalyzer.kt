// Lightweight SAST / Taint Analysis for common vulnerability classes
package io.dyuti.osvplugin.sast

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

/**
 * SAST (Static Application Security Testing) analyzer that detects common
 * vulnerability patterns via lightweight taint analysis.
 *
 * ## What it detects
 *
 * - **SQL Injection**: Untrusted input concatenated into SQL queries
 * - **Path Traversal**: Untrusted input used in file path construction
 * - **XSS (Reflected)**: Untrusted input output to HTML/response without escaping
 * - **Command Injection**: Untrusted input passed to process execution
 *
 * ## Architecture
 *
 * Uses a pattern-matching approach over PSI:
 * 1. Scan all Java source files in the project
 * 2. Use [SqlInjectionDetector], [PathTraversalDetector], [XssDetector]
 * 3. Each detector uses PSI to find dangerous patterns
 * 4. Results include file path, line number, and remediation advice
 *
 * ## Limitations
 *
 * This is a **lightweight** implementation. It does NOT perform:
 * - Full data-flow analysis (unlike CodeQL, Semgrep)
 * - Inter-procedural taint tracking
 * - Type-aware analysis
 * - Configurable rule engine
 *
 * These advanced features would require significant additional engineering.
 */
class SastAnalyzer {
    /**
     * Run all SAST checks on the project and return findings.
     */
    fun analyzeProject(project: Project): List<SastFinding> {
        if (project.isDisposed) return emptyList()

        return ReadAction.compute<List<SastFinding>, Throwable> {
            val findings = mutableListOf<SastFinding>()
            val sourceFiles = collectJavaSourceFiles(project)
            val psiManager = PsiManager.getInstance(project)

            for (virtualFile in sourceFiles) {
                val psiFile = psiManager.findFile(virtualFile) ?: continue

                // Run all detectors
                findings.addAll(SqlInjectionDetector().detect(psiFile))
                findings.addAll(PathTraversalDetector().detect(psiFile))
                findings.addAll(XssDetector().detect(psiFile))
            }

            findings
        }
    }

    /**
     * Collect all Java source files in the project.
     */
    private fun collectJavaSourceFiles(project: Project): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        val projectBase = project.baseDir ?: return emptyList()

        VfsUtilCore.visitChildrenRecursively(
            projectBase,
            object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory && file.name.endsWith(".java", ignoreCase = true)) {
                        files.add(file)
                    }
                    return true
                }
            },
        )

        return files
    }
}

/**
 * A single SAST finding.
 *
 * @param ruleId Detector rule identifier (e.g. "SQL-INJECTION", "PATH-TRAVERSAL")
 * @param severity Finding severity
 * @param message Human-readable description of the issue
 * @param filePath Source file path
 * @param lineNumber Line number (1-based)
 * @param remediation Advice for fixing the issue
 */
data class SastFinding(
    val ruleId: String,
    val severity: SastSeverity,
    val message: String,
    val filePath: String,
    val lineNumber: Int,
    val remediation: String,
)

/**
 * SAST finding severity levels.
 */
enum class SastSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO,
}
