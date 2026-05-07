// OSV Vulnerability Scanner Quick Fix — Document-based refactoring with undo support
package io.dyuti.osvplugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import io.dyuti.osvplugin.config.OsVConfig

/**
 * Quick fix for OSV vulnerabilities using document-level editing.
 *
 * All changes are wrapped in [WriteCommandAction] for undo support (Ctrl+Z).
 * Uses [FileDocumentManager] to get the [Document] for safe text edits,
 * then saves the document.
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

    override fun getName(): String =
        when (fixType) {
            FixType.UPGRADE -> "Upgrade ${dependency.name} to fixed version"
            FixType.SUPPRESS -> "Suppress ${vulnerability.id}"
            FixType.IGNORE -> "Ignore ${dependency.name}"
        }

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor,
    ) {
        val file = descriptor.psiElement.containingFile ?: return

        when (fixType) {
            FixType.UPGRADE -> upgradeVersion(project, file)
            FixType.SUPPRESS -> suppressVulnerability(project, file)
            FixType.IGNORE -> ignorePackage(project)
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // UPGRADE — document-level version replacement with undo support
    // ───────────────────────────────────────────────────────────────────────────

    private fun upgradeVersion(
        project: Project,
        file: PsiFile,
    ) {
        val latestFixedVersion = findLatestFixedVersion()

        if (latestFixedVersion == null) {
            Messages.showWarningDialog(
                project,
                "No fixed version available for ${dependency.name}",
                "OSV Vulnerability Fix",
            )
            return
        }

        try {
            when {
                file.name == "pom.xml" -> {
                    upgradeMavenVersion(project, file, latestFixedVersion)
                }

                file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts") -> {
                    upgradeGradleVersion(project, file, latestFixedVersion)
                }

                file.name == "package.json" -> {
                    upgradeNpmVersion(project, file, latestFixedVersion)
                }

                file.name == "requirements.txt" -> {
                    upgradePipVersion(project, file, latestFixedVersion)
                }

                else -> {
                    Messages.showWarningDialog(
                        project,
                        "Unsupported file type: ${file.name}",
                        "OSV Vulnerability Fix",
                    )
                }
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

    /**
     * Maven: Match the exact <dependency> block by <artifactId> and update <version>.
     * Operates on [Document] so the change is undoable.
     */
    private fun upgradeMavenVersion(
        project: Project,
        file: PsiFile,
        newVersion: String,
    ) {
        val document = getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(
            project,
            "Upgrade ${dependency.name} to $newVersion",
            null,
            Runnable {
                val text = document.text
                val depName = dependency.name
                val oldVersion = dependency.version

                // Find <dependency> block containing the artifactId
                val depBlockPattern =
                    Regex(
                        "(<dependency[^>]*>\\s*(?:(?!</dependency>).)*?<artifactId>\\s*$depName\\s*</artifactId>" +
                            "(?:(?!</dependency>).)*?<version>\\s*$oldVersion\\s*</version>" +
                            "(?:(?!</dependency>).)*?</dependency>)",
                        RegexOption.DOT_MATCHES_ALL,
                    )
                val match = depBlockPattern.find(text)

                if (match != null) {
                    val block = match.groupValues[0]
                    val updatedBlock =
                        block.replace(
                            Regex("(<version>\\s*)$oldVersion(\\s*</version>)"),
                            "$1$newVersion$2",
                        )
                    document.replaceString(match.range.first, match.range.last + 1, updatedBlock)
                    FileDocumentManager.getInstance().saveDocument(document)

                    Messages.showInfoMessage(
                        project,
                        "Upgraded $depName to $newVersion",
                        "OSV Vulnerability Fix",
                    )
                } else {
                    // Fallback: try broader search for <version> near the artifactId
                    val versionPattern =
                        Regex(
                            "(<artifactId>\\s*$depName\\s*</artifactId>(?:(?!</dependency>).){0,500}?)" +
                                "<version>\\s*$oldVersion\\s*</version>",
                            RegexOption.DOT_MATCHES_ALL,
                        )
                    val fallbackMatch = versionPattern.find(text)
                    if (fallbackMatch != null) {
                        val start = fallbackMatch.range.first + fallbackMatch.groupValues[1].length
                        val end = fallbackMatch.range.last + 1
                        val oldText = text.substring(start, end)
                        val newText = oldText.replace(oldVersion, newVersion)
                        document.replaceString(start, end, newText)
                        FileDocumentManager.getInstance().saveDocument(document)

                        Messages.showInfoMessage(
                            project,
                            "Upgraded $depName to $newVersion",
                            "OSV Vulnerability Fix",
                        )
                    } else {
                        Messages.showWarningDialog(
                            project,
                            "Could not find dependency $depName ($oldVersion) in pom.xml",
                            "OSV Vulnerability Fix",
                        )
                    }
                }
            },
        )
    }

    /**
     * Gradle: Match dependency declaration string (single or double quotes)
     * and replace version component.
     */
    private fun upgradeGradleVersion(
        project: Project,
        file: PsiFile,
        newVersion: String,
    ) {
        val document = getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(
            project,
            "Upgrade ${dependency.name} to $newVersion",
            null,
            Runnable {
                val text = document.text
                val depName = dependency.name
                val oldVersion = dependency.version

                // Match 'group:artifact:version' or "group:artifact:version"
                val gradlePattern =
                    Regex(
                        "(['\"])((?:(?!\\1).)*?:$depName:$oldVersion)\\1",
                    )
                val match = gradlePattern.find(text)

                if (match != null) {
                    val oldDepText = match.groupValues[2]
                    val newDepText = oldDepText.replace(":$oldVersion", ":$newVersion")
                    val fullNew = "${match.groupValues[1]}$newDepText${match.groupValues[1]}"
                    document.replaceString(match.range.first, match.range.last + 1, fullNew)
                    FileDocumentManager.getInstance().saveDocument(document)

                    Messages.showInfoMessage(
                        project,
                        "Upgraded $depName to $newVersion",
                        "OSV Vulnerability Fix",
                    )
                } else {
                    Messages.showWarningDialog(
                        project,
                        "Could not find dependency $depName in ${file.name}",
                        "OSV Vulnerability Fix",
                    )
                }
            },
        )
    }

    /**
     * npm: Match "packageName": "version" in dependencies or devDependencies.
     */
    private fun upgradeNpmVersion(
        project: Project,
        file: PsiFile,
        newVersion: String,
    ) {
        val document = getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(
            project,
            "Upgrade ${dependency.name} to $newVersion",
            null,
            Runnable {
                val text = document.text
                val depName = dependency.name
                val oldVersion = dependency.version

                val npmPattern =
                    Regex(
                        "(\"$depName\"\\s*:\\s*\")$oldVersion(\"",
                    )
                val match = npmPattern.find(text)

                if (match != null) {
                    document.replaceString(match.range.first, match.range.last + 1, "$1$newVersion\"")
                    FileDocumentManager.getInstance().saveDocument(document)

                    Messages.showInfoMessage(
                        project,
                        "Upgraded $depName to $newVersion in package.json",
                        "OSV Vulnerability Fix",
                    )
                } else {
                    Messages.showWarningDialog(
                        project,
                        "Could not find $depName in package.json",
                        "OSV Vulnerability Fix",
                    )
                }
            },
        )
    }

    /**
     * pip: Match various version specifiers (== >= <= ~= != < >).
     */
    private fun upgradePipVersion(
        project: Project,
        file: PsiFile,
        newVersion: String,
    ) {
        val document = getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(
            project,
            "Upgrade ${dependency.name} to $newVersion",
            null,
            Runnable {
                val text = document.text
                val depName = dependency.name
                val oldVersion = dependency.version
                val escapedName = Regex.escape(depName)

                val pipPattern =
                    Regex(
                        "($escapedName(?:==|>=|<=|~=|!=|>|<))$oldVersion",
                    )
                val match = pipPattern.find(text)

                if (match != null) {
                    document.replaceString(match.range.first, match.range.last + 1, "$1$newVersion")
                    FileDocumentManager.getInstance().saveDocument(document)

                    Messages.showInfoMessage(
                        project,
                        "Upgraded $depName to $newVersion in requirements.txt",
                        "OSV Vulnerability Fix",
                    )
                } else {
                    Messages.showWarningDialog(
                        project,
                        "Could not find $depName in requirements.txt",
                        "OSV Vulnerability Fix",
                    )
                }
            },
        )
    }

    // ───────────────────────────────────────────────────────────────────────────
    // SUPPRESS — add comment/document marker
    // ───────────────────────────────────────────────────────────────────────────

    private fun suppressVulnerability(
        project: Project,
        file: PsiFile,
    ) {
        val document = getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(
            project,
            "Suppress ${vulnerability.id}",
            null,
            Runnable {
                val commentText =
                    when {
                        file.name == "pom.xml" -> {
                            "<!-- OSV Suppressed: ${vulnerability.id} - ${vulnerability.summary} -->\n"
                        }

                        file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts") -> {
                            "// OSV Suppressed: ${vulnerability.id} - ${vulnerability.summary}\n"
                        }

                        file.name == "package.json" -> {
                            "// OSV Suppressed: ${vulnerability.id} - ${vulnerability.summary}\n"
                        }

                        else -> {
                            "# OSV Suppressed: ${vulnerability.id} - ${vulnerability.summary}\n"
                        }
                    }

                // Insert at the beginning of the file
                document.insertString(0, commentText)
                FileDocumentManager.getInstance().saveDocument(document)

                Messages.showInfoMessage(
                    project,
                    "Suppressed ${vulnerability.id} for ${dependency.name}",
                    "OSV Vulnerability Fix",
                )
            },
        )
    }

    // ───────────────────────────────────────────────────────────────────────────
    // IGNORE — no file changes
    // ───────────────────────────────────────────────────────────────────────────

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

    // ───────────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────────

    /** Obtains the [Document] for a PSI file. */
    private fun getDocument(file: PsiFile): Document? {
        val virtualFile = file.virtualFile ?: return null
        return FileDocumentManager.getInstance().getDocument(virtualFile)
    }
}
