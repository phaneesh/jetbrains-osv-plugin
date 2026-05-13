// IDE-facing SBOM export service
package io.dyuti.osvplugin.sbom

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import io.dyuti.osvplugin.api.model.Dependency
import java.io.File

/**
 * Service that exports SBOM files from the IDE.
 *
 * Wraps [SbomGenerator] with file-system operations and project-aware paths.
 */
class SbomExporter(
    private val project: Project,
) {
    private val generator =
        SbomGenerator(
            appName = project.name,
            appVersion = "1.0.0",
        )

    /**
     * Export an SBOM to a file in the project's root directory.
     *
     * @return The exported [File]
     */
    fun export(
        dependencies: List<Dependency>,
        format: SbomFormat,
        filename: String? = null,
    ): File {
        val content = generator.generate(dependencies, format)
        val ext =
            when (format) {
                SbomFormat.CYCLONEDX_JSON -> "cdx.json"
                SbomFormat.SPDX_JSON -> "spdx.json"
                SbomFormat.SPDX_TAGVALUE -> "spdx.tv"
            }
        val name = filename ?: "sbom-${project.name}-$ext"
        val outputDir = File(project.basePath ?: ".", "sbom-output")
        outputDir.mkdirs()
        val file = File(outputDir, name)
        file.writeText(content)

        // Refresh VFS so file appears in Project view immediately —
        // must run on EDT to avoid "write thread only" access errors
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            } catch (_: Exception) {
                // VFS refresh is best-effort; file exists on disk regardless
            }
        }

        return file
    }

    /**
     * Convenience: export to all three formats at once.
     *
     * @return Map of format → exported file
     */
    fun exportAll(dependencies: List<Dependency>): Map<SbomFormat, File> =
        SbomFormat.entries.associateWith { format ->
            val ext =
                when (format) {
                    SbomFormat.CYCLONEDX_JSON -> "cdx.json"
                    SbomFormat.SPDX_JSON -> "spdx.json"
                    SbomFormat.SPDX_TAGVALUE -> "spdx.tv"
                }
            export(dependencies, format, "sbom-${project.name}.$ext")
        }

    /**
     * Build an SBOM from the last scan's parsed dependencies.
     * Falls back to re-parsing if no cached dependencies.
     */
    fun exportFromParsedDependencies(
        parsedDeps: Map<*, List<Dependency>>,
        format: SbomFormat,
    ): File {
        val allDeps = parsedDeps.values.flatten().distinctBy { "${it.name}:${it.version}" }
        return export(allDeps, format)
    }
}
