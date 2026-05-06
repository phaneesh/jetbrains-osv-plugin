// OSV Vulnerability Scanner PSI Inspection with Full Dependency Scanning
package io.dyuti.osvplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
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
 * OSV Local Inspection Tool with full PSI traversal
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

    override fun getDisplayName(): String = "OSV Vulnerability Check"

    override fun getShortName(): String = "OsVInspection"

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                // Scan the file for dependencies
                scanFileForDependencies(file, holder)
            }
        }

    private fun scanFileForDependencies(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        val fileName = file.name
        val fileContent = file.text

        // Find appropriate parser for this file
        parsers.forEach { parser ->
            if (parser.canHandle(fileName)) {
                try {
                    // Parse dependencies from file
                    val dependencies = parser.parse(fileName, fileContent)

                    // Check each dependency for vulnerabilities
                    dependencies.forEach { dep ->
                        // Query vulnerabilities (with caching)
                        val vulnerabilities =
                            apiService.queryVulnerabilities(
                                dep.name,
                                dep.ecosystem,
                                dep.version,
                            )

                        // Report vulnerabilities if any found
                        vulnerabilities.forEach { vuln ->
                            if (shouldReportVulnerability(vuln)) {
                                reportVulnerability(holder, dep, vuln, file)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error but don't fail the inspection
                    System.err.println("Error scanning file $fileName: ${e.message}")
                }
            }
        }
    }

    private fun shouldReportVulnerability(vuln: Vulnerability): Boolean {
        // Only report if severity meets minimum threshold
        val config =
            @Suppress("DEPRECATION")
            com.intellij.openapi.components.ServiceManager
                .getService(OsVConfig::class.java)
        return SeverityUtil.meetsThreshold(vuln.severity, config.minimumSeverity)
    }

    private fun reportVulnerability(
        holder: ProblemsHolder,
        dep: Dependency,
        vuln: Vulnerability,
        file: PsiFile,
    ) {
        // Create quick fixes for the vulnerability
        val upgradeFix = OsVQuickFix.createUpgradeFix(dep, vuln)
        val suppressFix = OsVQuickFix.createSuppressFix(dep, vuln)
        val ignoreFix = OsVQuickFix.createIgnoreFix(dep, vuln)

        // Create problem descriptor
        val message = "Vulnerability: ${vuln.id} - ${vuln.summary}"

        holder.registerProblem(
            file,
            message,
            ProblemHighlightType.WARNING,
            upgradeFix,
            suppressFix,
            ignoreFix,
        )
    }

    /**
     * Check dependencies for vulnerabilities
     */
    fun checkDependencies(dependencies: List<Dependency>): List<DependencyWithVulnerabilities> {
        val results = mutableListOf<DependencyWithVulnerabilities>()

        dependencies.forEach { dep ->
            // Query vulnerabilities (with caching)
            val vulnerabilities =
                apiService.queryVulnerabilities(
                    dep.name,
                    dep.ecosystem,
                    dep.version,
                )

            // Add to results if vulnerabilities found
            if (vulnerabilities.isNotEmpty()) {
                results.add(DependencyWithVulnerabilities(dep, vulnerabilities, "", 1))
            }
        }

        return results
    }
}
