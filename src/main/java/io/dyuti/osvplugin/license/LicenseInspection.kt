// SPDX License Scanner Integration - Inspection
package io.dyuti.osvplugin.license

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.inspection.DependencyWithVulnerabilities
import io.dyuti.osvplugin.parser.DependencyParser
import io.dyuti.osvplugin.parser.GradleParser
import io.dyuti.osvplugin.parser.MavenParser
import io.dyuti.osvplugin.parser.NpmParser
import io.dyuti.osvplugin.parser.PipParser

/**
 * License inspection tool
 */
class LicenseInspection : LocalInspectionTool() {
    
    private val parsers: List<DependencyParser> = listOf(
        MavenParser(),
        GradleParser(),
        NpmParser(),
        PipParser()
    )
    
    private val licenseScanner = LicenseScanner()
    
    override fun getDisplayName(): String = "License Compliance Check"
    
    override fun getShortName(): String = "LicenseInspection"
    
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                scanFileForLicenseIssues(file, holder)
            }
        }
    }
    
    private fun scanFileForLicenseIssues(file: PsiFile, holder: ProblemsHolder) {
        val fileName = file.name
        
        // Find appropriate parser for this file
        parsers.forEach { parser ->
            if (parser.canHandle(fileName)) {
                try {
                    // Parse dependencies
                    val dependencies = parser.parse(fileName, file.text)
                    
                    // Scan for licenses
                    val dependenciesWithLicense = licenseScanner.scanDependencies(dependencies)
                    
                    // Check for conflicts
                    val conflicts = licenseScanner.checkLicenseConflicts(dependenciesWithLicense)
                    
                    // Report conflicts
                    conflicts.forEach { conflict ->
                        reportLicenseConflict(holder, conflict, file)
                    }
                    
                    // Report unknown licenses
                    dependenciesWithLicense.forEach { dep ->
                        if (dep.license == "UNKNOWN") {
                            reportUnknownLicense(holder, dep, file)
                        }
                    }
                    
                } catch (e: Exception) {
                    System.err.println("Error scanning licenses for $fileName: ${e.message}")
                }
            }
        }
    }
    
    private fun reportLicenseConflict(
        holder: ProblemsHolder,
        conflict: LicenseConflict,
        file: PsiFile
    ) {
        val message = "License Conflict: ${conflict.getSummary()}"
        
        holder.registerProblem(
            file,
            message,
            when (conflict.severity) {
                Severity.CRITICAL -> ProblemHighlightType.ERROR
                Severity.HIGH -> ProblemHighlightType.WARNING
                Severity.MEDIUM -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                Severity.LOW -> ProblemHighlightType.INFORMATION
            }
        )
    }
    
    private fun reportUnknownLicense(
        holder: ProblemsHolder,
        dep: DependencyWithLicense,
        file: PsiFile
    ) {
        val message = "Unknown License: ${dep.name} has unknown license (${dep.license})"
        
        holder.registerProblem(
            file,
            message,
            ProblemHighlightType.INFORMATION
        )
    }
}
