// OSV Vulnerability Scanner Quick Fix
package io.dyuti.osvplugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig

/**
 * Quick fix for OSV vulnerabilities
 */
class OsVQuickFix private constructor(
    private val dependency: Dependency,
    private val vulnerability: Vulnerability,
    private val fixType: FixType,
) : LocalQuickFix {
    enum class FixType {
        UPGRADE,
        SUPPRESS,
        IGNORE,
    }

    companion object {
        fun createUpgradeFix(
            dependency: Dependency,
            vulnerability: Vulnerability,
        ): OsVQuickFix = OsVQuickFix(dependency, vulnerability, FixType.UPGRADE)

        fun createSuppressFix(
            dependency: Dependency,
            vulnerability: Vulnerability,
        ): OsVQuickFix = OsVQuickFix(dependency, vulnerability, FixType.SUPPRESS)

        fun createIgnoreFix(
            dependency: Dependency,
            vulnerability: Vulnerability,
        ): OsVQuickFix = OsVQuickFix(dependency, vulnerability, FixType.IGNORE)
    }

    override fun getFamilyName(): String = "OSV Vulnerability Fix"

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor,
    ) {
        val file = descriptor.psiElement.containingFile ?: return
        val virtualFile = file.virtualFile ?: return

        when (fixType) {
            FixType.UPGRADE -> upgradeVersion(project, virtualFile)
            FixType.SUPPRESS -> suppressVulnerability(project, virtualFile)
            FixType.IGNORE -> ignorePackage(project)
        }
    }

    private fun upgradeVersion(
        project: Project,
        virtualFile: VirtualFile,
    ) {
        try {
            val latestFixedVersion = findLatestFixedVersion()

            if (latestFixedVersion == null) {
                Messages.showWarningDialog(
                    project,
                    "No fixed version available",
                    "OSV Vulnerability Fix",
                )
                return
            }

            val content = String(virtualFile.contentsToByteArray())
            val updatedContent =
                updateDependencyVersion(
                    content,
                    dependency.version,
                    latestFixedVersion,
                )

            if (updatedContent != null) {
                virtualFile.setBinaryContent(updatedContent.toByteArray(Charsets.UTF_8))
                virtualFile.refresh(false, false)

                Messages.showInfoMessage(
                    project,
                    "Successfully upgraded ${dependency.name} from ${dependency.version} to $latestFixedVersion",
                    "OSV Vulnerability Fix",
                )
            } else {
                Messages.showWarningDialog(
                    project,
                    "Could not find dependency ${dependency.name} in the file",
                    "OSV Vulnerability Fix",
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to upgrade: ${e.message}",
                "OSV Vulnerability Fix",
            )
        }
    }

    private fun findLatestFixedVersion(): String? = vulnerability.fixedVersions.maxOrNull()

    private fun updateDependencyVersion(
        content: String,
        oldVersion: String,
        newVersion: String,
    ): String? {
        val lines = content.lines()
        val updatedLines = mutableListOf<String>()
        var found = false

        for (line in lines) {
            val updatedLine =
                when {
                    isMavenDependency(line) -> {
                        val updated = replaceMavenVersion(line, oldVersion, newVersion)
                        if (updated != line) found = true
                        updated
                    }

                    isGradleDependency(line) -> {
                        val updated = replaceGradleVersion(line, oldVersion, newVersion)
                        if (updated != line) found = true
                        updated
                    }

                    isNpmDependency(line) -> {
                        val updated = replaceNpmVersion(line, oldVersion, newVersion)
                        if (updated != line) found = true
                        updated
                    }

                    isPipDependency(line) -> {
                        val updated = replacePipVersion(line, oldVersion, newVersion)
                        if (updated != line) found = true
                        updated
                    }

                    else -> {
                        line
                    }
                }
            updatedLines.add(updatedLine)
        }

        return if (found) updatedLines.joinToString("\n") else null
    }

    private fun isMavenDependency(line: String): Boolean =
        line.contains("<artifactId>${dependency.name}</artifactId>") ||
            (line.contains(dependency.name) && line.contains("<version>"))

    private fun replaceMavenVersion(
        line: String,
        oldVersion: String,
        newVersion: String,
    ): String =
        @Suppress("UNUSED_PARAMETER")
        line.replace(
            "<version>$oldVersion</version>",
            "<version>$newVersion</version>",
        )

    private fun isGradleDependency(line: String): Boolean =
        line.contains(dependency.name) &&
            (line.contains("'") || line.contains("\""))

    private fun replaceGradleVersion(
        line: String,
        oldVersion: String,
        newVersion: String,
    ): String {
        @Suppress("UNUSED_PARAMETER")
        val escapedName = Regex.escape(dependency.name)
        val singleQuotePattern =
            Regex("'([^:]+):$escapedName:$oldVersion'")
        val doubleQuotePattern =
            Regex("\"([^:]+):$escapedName:$oldVersion\"")

        var result =
            line.replace(singleQuotePattern) {
                "'${it.groups[1]?.value}:${dependency.name}:$newVersion'"
            }
        result =
            result.replace(doubleQuotePattern) {
                "\"${it.groups[1]?.value}:${dependency.name}:$newVersion\""
            }
        return result
    }

    private fun isNpmDependency(line: String): Boolean = line.contains("\"${dependency.name}\"") && line.contains(":")

    @Suppress("UNUSED_PARAMETER")
    private fun replaceNpmVersion(
        line: String,
        _oldVersion: String,
        newVersion: String,
    ): String {
        val escapedName = Regex.escape(dependency.name)
        val pattern = Regex("\"$escapedName\":\\s*\"[^\"]+\"")
        return pattern.replace(line) {
            "\"${dependency.name}\": \"$newVersion\""
        }
    }

    private fun isPipDependency(line: String): Boolean =
        line.contains(dependency.name) &&
            (line.contains("==") || line.contains(">="))

    @Suppress("UNUSED_PARAMETER")
    private fun replacePipVersion(
        line: String,
        _oldVersion: String,
        newVersion: String,
    ): String {
        val escapedName = Regex.escape(dependency.name)
        val pattern = Regex("($escapedName==)[^\\s]+")
        return pattern.replace(line) { "${it.groups[1]?.value}$newVersion" }
    }

    private fun suppressVulnerability(
        project: Project,
        virtualFile: VirtualFile,
    ) {
        val content = String(virtualFile.contentsToByteArray())

        val suppressionComment =
            "// OSV Suppressed: ${vulnerability.id} - ${vulnerability.summary}"
        val updatedContent =
            content.lines().joinToString("\n") { line ->
                if (line.contains(dependency.name)) {
                    "$line $suppressionComment"
                } else {
                    line
                }
            }

        if (updatedContent != content) {
            virtualFile.setBinaryContent(updatedContent.toByteArray(Charsets.UTF_8))
            virtualFile.refresh(false, false)

            Messages.showInfoMessage(
                project,
                "Suppressed vulnerability ${vulnerability.id} for ${dependency.name}",
                "OSV Vulnerability Fix",
            )
        }
    }

    private fun ignorePackage(project: Project) {
        @Suppress("DEPRECATION")
        val config =
            com.intellij.openapi.components.ServiceManager
                .getService(OsVConfig::class.java)
        val ignoredPackages = config.ignoredPackages.toMutableList()

        if (!ignoredPackages.contains(dependency.name)) {
            ignoredPackages.add(dependency.name)
            config.ignoredPackages = ignoredPackages

            Messages.showInfoMessage(
                project,
                "Added ${dependency.name} to ignored packages list",
                "OSV Vulnerability Fix",
            )
        }
    }
}
