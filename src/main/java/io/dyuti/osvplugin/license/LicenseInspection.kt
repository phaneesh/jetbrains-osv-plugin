// SPDX License Scanner Integration - Inspection
package io.dyuti.osvplugin.license

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.inspection.DependencyWithVulnerabilities
import io.dyuti.osvplugin.parser.CargoParser
import io.dyuti.osvplugin.parser.ComposerParser
import io.dyuti.osvplugin.parser.ConanParser
import io.dyuti.osvplugin.parser.DependencyParser
import io.dyuti.osvplugin.parser.GemfileParser
import io.dyuti.osvplugin.parser.GoParser
import io.dyuti.osvplugin.parser.GradleParser
import io.dyuti.osvplugin.parser.MavenParser
import io.dyuti.osvplugin.parser.MixParser
import io.dyuti.osvplugin.parser.NpmParser
import io.dyuti.osvplugin.parser.NugetParser
import io.dyuti.osvplugin.parser.PipParser
import io.dyuti.osvplugin.parser.PoetryParser
import io.dyuti.osvplugin.parser.PubspecParser
import io.dyuti.osvplugin.parser.RenvParser
import io.dyuti.osvplugin.parser.StackParser
import io.dyuti.osvplugin.parser.YarnParser

/**
 * License inspection tool
 */
class LicenseInspection : LocalInspectionTool() {
    companion object {
        private val LOG = Logger.getInstance(LicenseInspection::class.java)
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

    private val licenseScanner = LicenseScanner()

    override fun getDisplayName(): String = "License Compliance Check"

    override fun getShortName(): String = "LicenseInspection"

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                scanFileForLicenseIssues(file, holder)
            }
        }

    private fun scanFileForLicenseIssues(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
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
                    LOG.error("Error scanning licenses for $fileName", e)
                }
            }
        }
    }

    private fun reportLicenseConflict(
        holder: ProblemsHolder,
        conflict: LicenseConflict,
        file: PsiFile,
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
            },
        )
    }

    private fun reportUnknownLicense(
        holder: ProblemsHolder,
        dep: DependencyWithLicense,
        file: PsiFile,
    ) {
        val message = "Unknown License: ${dep.name} has unknown license (${dep.license})"

        holder.registerProblem(
            file,
            message,
            ProblemHighlightType.INFORMATION,
        )
    }
}
