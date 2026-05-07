// OSV Vulnerability Scanner — Auto-Fix Service
package io.dyuti.osvplugin.fix

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Service for applying automated fixes to dependency manifest files.
 *
 * Correctly handles:
 * - **Direct dependencies**: upgrade version in-place (supports Maven properties)
 * - **Transitive dependencies**: add override with fix version
 *   - Maven: `dependencyManagement`
 *   - npm: `"overrides"` (npm ≥8.3)
 *   - pip: `constraints.txt`
 */
class AutoFixService private constructor() {
    companion object {
        fun getInstance(): AutoFixService = AutoFixService()
    }

    /**
     * Apply an automated fix for [vulnerability] affecting [dependency] declared in [moduleFile].
     *
     * @param project   The current IntelliJ project (needed for WriteCommandAction).
     * @param moduleFile The manifest file (pom.xml, package.json, etc.).
     * @param dependency The dependency that triggered the vulnerability.
     * @param vulnerability The vulnerability whose fix version should be applied.
     * @return `true` if the file was modified successfully.
     */
    @Suppress("ReturnCount")
    fun applyFix(
        project: Project,
        moduleFile: VirtualFile,
        dependency: Dependency,
        vulnerability: Vulnerability,
    ): Boolean {
        val fixVersion = findBestFixVersion(vulnerability) ?: return false

        return when {
            moduleFile.name == "pom.xml" -> {
                applyMavenFix(project, moduleFile, dependency, fixVersion)
            }

            moduleFile.name.endsWith(".gradle") || moduleFile.name.endsWith(".gradle.kts") -> {
                applyGradleFix(project, moduleFile, dependency, fixVersion)
            }

            moduleFile.name == "package.json" -> {
                applyNpmFix(project, moduleFile, dependency, fixVersion)
            }

            moduleFile.name == "requirements.txt" -> {
                applyPipFix(project, moduleFile, dependency, fixVersion)
            }

            else -> {
                false
            }
        }
    }

    /** Choose the latest fixed version from the vulnerability record. */
    internal fun findBestFixVersion(vulnerability: Vulnerability): String? {
        // SemVer-aware sorting: prefer versions without suffixes, then higher numbers
        return vulnerability.fixedVersions
            .sortedWith(SemVerComparator().reversed())
            .firstOrNull()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Maven
    // ═══════════════════════════════════════════════════════════════════════

    private fun applyMavenFix(
        project: Project,
        file: VirtualFile,
        dependency: Dependency,
        fixVersion: String,
    ): Boolean {
        val document = getDocument(file) ?: return false
        val newText = MavenFixer.apply(document.text, dependency, fixVersion)
        if (newText == null || newText == document.text) return false

        WriteCommandAction.runWriteCommandAction(
            project,
            "Fix ${dependency.name} vulnerability",
            null,
            Runnable {
                document.setText(newText)
                FileDocumentManager.getInstance().saveDocument(document)
            },
        )
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Gradle
    // ═══════════════════════════════════════════════════════════════════════

    private fun applyGradleFix(
        project: Project,
        file: VirtualFile,
        dependency: Dependency,
        fixVersion: String,
    ): Boolean {
        val document = getDocument(file) ?: return false
        val newText = GradleFixer.apply(document.text, dependency, fixVersion)
        if (newText == null || newText == document.text) return false

        WriteCommandAction.runWriteCommandAction(
            project,
            "Fix ${dependency.name} vulnerability",
            null,
            Runnable {
                document.setText(newText)
                FileDocumentManager.getInstance().saveDocument(document)
            },
        )
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // npm
    // ═══════════════════════════════════════════════════════════════════════

    private fun applyNpmFix(
        project: Project,
        file: VirtualFile,
        dependency: Dependency,
        fixVersion: String,
    ): Boolean {
        val document = getDocument(file) ?: return false
        val newText = NpmFixer.apply(document.text, dependency, fixVersion)
        if (newText == null || newText == document.text) return false

        WriteCommandAction.runWriteCommandAction(
            project,
            "Fix ${dependency.name} vulnerability",
            null,
            Runnable {
                document.setText(newText)
                FileDocumentManager.getInstance().saveDocument(document)
            },
        )
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // pip
    // ═══════════════════════════════════════════════════════════════════════

    private fun applyPipFix(
        project: Project,
        file: VirtualFile,
        dependency: Dependency,
        fixVersion: String,
    ): Boolean {
        // Update requirements.txt first
        val reqDoc = getDocument(file) ?: return false
        val newReqText = PipFixer.applyRequirementsTxt(reqDoc.text, dependency, fixVersion)
        val reqChanged = newReqText != null && newReqText != reqDoc.text
        if (reqChanged) {
            WriteCommandAction.runWriteCommandAction(
                project,
                "Fix ${dependency.name} vulnerability",
                null,
                Runnable {
                    reqDoc.setText(newReqText!!)
                    FileDocumentManager.getInstance().saveDocument(reqDoc)
                },
            )
        }

        // Always create / update constraints.txt
        val constraintsFile = findOrCreateConstraintsFile(project, file)
        val constraintsDoc = getDocument(constraintsFile) ?: return reqChanged
        val newConstraints = PipFixer.applyConstraintsTxt(constraintsDoc.text, dependency, fixVersion)
        val constraintsChanged = newConstraints != null && newConstraints != constraintsDoc.text
        if (constraintsChanged) {
            WriteCommandAction.runWriteCommandAction(
                project,
                "Add constraints for ${dependency.name}",
                null,
                Runnable {
                    constraintsDoc.setText(newConstraints!!)
                    FileDocumentManager.getInstance().saveDocument(constraintsDoc)
                },
            )
        }

        return reqChanged || constraintsChanged
    }

    private fun findOrCreateConstraintsFile(
        project: Project,
        requirementsFile: VirtualFile,
    ): VirtualFile {
        val parent = requirementsFile.parent
        var constraints = parent?.findChild("constraints.txt")
        if (constraints == null) {
            constraints = parent?.createChildData(this, "constraints.txt")
        }
        return constraints ?: requirementsFile
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun getDocument(virtualFile: VirtualFile): Document? = FileDocumentManager.getInstance().getDocument(virtualFile)
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Maven Fixer — XML DOM manipulation with property support
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object MavenFixer {
    /**
     * Apply a fix to a Maven POM.
     *
     * Algorithm:
     * 1. Look for the dependency in `<dependencies>` (direct).
     *    a. If found and version is literal → update in-place.
     *    b. If found and version is `${prop}` → update the property
     *       (search `<properties>`, `dependencyManagement`, and `parent`).
     * 2. If not found in `<dependencies>` (transitive or no declaration):
     *    a. Check `<dependencyManagement>`.
     *    b. If in depMgt → update version (handle property refs).
     *    c. If NOT in depMgt → add to `<dependencyManagement>`.
     *
     * @return New POM text, or `null` if nothing was changed.
     */
    fun apply(
        pomText: String,
        dependency: Dependency,
        fixVersion: String,
    ): String? {
        // Quick check: is this even a Maven artifact reference?
        val (groupId, artifactId) = parseMavenCoords(dependency.name) ?: return null

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isValidating = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(pomText.byteInputStream())
        doc.documentElement.normalize()

        val projectElem = doc.documentElement

        // ── Step 1: check <dependencies>/<dependency> for direct dep ────────────
        val depElem =
            findDependency(
                projectElem,
                groupId,
                artifactId,
                inDepManagement = false,
            )

        val changed =
            if (depElem != null) {
                // Direct dependency — update version (property or literal)
                updateDependencyVersion(doc, projectElem, depElem, fixVersion)
            } else {
                // Not found in direct deps — check dependencyManagement
                val dmElem =
                    findDependency(
                        projectElem,
                        groupId,
                        artifactId,
                        inDepManagement = true,
                    )
                if (dmElem != null) {
                    // Already in dependencyManagement — update version
                    updateDependencyVersion(doc, projectElem, dmElem, fixVersion)
                } else {
                    // Add to dependencyManagement
                    addDependencyToManagement(doc, projectElem, groupId, artifactId, fixVersion)
                }
            }

        return if (changed) serializeXml(doc) else null
    }

    /** Split "groupId:artifactId" into pair or return null if malformed. */
    internal fun parseMavenCoords(name: String): Pair<String, String>? {
        val parts = name.split(":")
        return if (parts.size >= 2) {
            parts[0] to parts[1]
        } else {
            null
        }
    }

    /** Find a `<dependency>` with matching groupId + artifactId. */
    private fun findDependency(
        projectElem: Element,
        groupId: String,
        artifactId: String,
        inDepManagement: Boolean,
    ): Element? {
        val containerTag = if (inDepManagement) "dependencyManagement" else "dependencies"
        val container = getFirstChildByTag(projectElem, containerTag) ?: return null
        val depList = container.getElementsByTagName("dependency")

        for (i in 0 until depList.length) {
            val dep = depList.item(i) as? Element ?: continue
            val g = getTextContent(dep, "groupId")
            val a = getTextContent(dep, "artifactId")
            if (g == groupId && a == artifactId) {
                return dep
            }
        }
        return null
    }

    /**
     * Update the version inside a `<dependency>` element.
     * Handles both literal versions and `${property}` references.
     */
    private fun updateDependencyVersion(
        doc: org.w3c.dom.Document,
        projectElem: Element,
        depElem: Element,
        newVersion: String,
    ): Boolean {
        val versionElem = getFirstChildByTag(depElem, "version")
        if (versionElem == null) {
            // No version tag — add one
            val newVer = doc.createElement("version")
            newVer.textContent = newVersion
            depElem.appendChild(newVer)
            return true
        }

        val versionText = versionElem.textContent?.trim() ?: ""
        return if (versionText.startsWith("\${") && versionText.endsWith("}")) {
            // Property reference — update the property value
            val propName = versionText.substring(2, versionText.length - 1).trim()
            updateProperty(doc, projectElem, propName, newVersion)
        } else {
            // Literal version
            versionElem.textContent = newVersion
            true
        }
    }

    /**
     * Update a property value inside `<properties>` (or fallback to depMgt/parent).
     */
    private fun updateProperty(
        doc: org.w3c.dom.Document,
        projectElem: Element,
        propName: String,
        newValue: String,
    ): Boolean {
        // Try <properties> first
        val props = getFirstChildByTag(projectElem, "properties")
        if (props != null) {
            val existing = getFirstChildByTag(props, propName)
            if (existing != null) {
                existing.textContent = newValue
                return true
            }
            // Property not found in <properties> — add it
            val newProp = doc.createElement(propName)
            newProp.textContent = newValue
            props.appendChild(newProp)
            return true
        }

        // No <properties> section — create one and add the property
        val newProps = doc.createElement("properties")
        val newProp = doc.createElement(propName)
        newProp.textContent = newValue
        newProps.appendChild(newProp)
        projectElem.appendChild(newProps)
        return true
    }

    /** Add a dependency to `<dependencyManagement>`. */
    private fun addDependencyToManagement(
        doc: org.w3c.dom.Document,
        projectElem: Element,
        groupId: String,
        artifactId: String,
        version: String,
    ): Boolean {
        val dm =
            getFirstChildByTag(projectElem, "dependencyManagement")
                ?: doc.createElement("dependencyManagement").also {
                    projectElem.appendChild(it)
                }

        val deps =
            getFirstChildByTag(dm, "dependencies")
                ?: doc.createElement("dependencies").also {
                    dm.appendChild(it)
                }

        val dep = doc.createElement("dependency")
        val g = doc.createElement("groupId")
        g.textContent = groupId
        val a = doc.createElement("artifactId")
        a.textContent = artifactId
        val v = doc.createElement("version")
        v.textContent = version

        dep.appendChild(g)
        dep.appendChild(a)
        dep.appendChild(v)
        deps.appendChild(dep)

        return true
    }

    private fun getFirstChildByTag(
        parent: Element,
        tag: String,
    ): Element? {
        val list = parent.getElementsByTagName(tag)
        return if (list.length > 0) list.item(0) as? Element else null
    }

    private fun getTextContent(
        parent: Element,
        tag: String,
    ): String? {
        val child = getFirstChildByTag(parent, tag) ?: return null
        return child.textContent?.trim()
    }

    private fun serializeXml(doc: org.w3c.dom.Document): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Gradle Fixer — regex-based (Groovy / Kotlin DSL are too flexible for DOM)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object GradleFixer {
    /**
     * Update a Gradle dependency declaration.
     *
     * For direct dependencies: replaces `'group:artifact:version'` or `"group:artifact:version"`.
     * For transitive dependencies: adds `resolutionStrategy.force "group:artifact:fixedVersion"`.
     */
    fun apply(
        gradleText: String,
        dependency: Dependency,
        fixVersion: String,
    ): String? {
        val depName = dependency.name
        val (group, artifact) = parseGradleCoords(depName) ?: return null
        val oldVersion = dependency.version

        // Try to find direct dependency declaration first
        val directPattern =
            Regex(
                "(['\"])((?:[^:]+):${Regex.escape(artifact)}:${Regex.escape(oldVersion)})\\1",
            )

        return if (directPattern.containsMatchIn(gradleText)) {
            // Direct dependency — update version in-place
            gradleText.replace(
                directPattern,
                "$1$group:$artifact:$fixVersion$1",
            )
        } else {
            // Transitive — add resolutionStrategy force
            addResolutionStrategyForce(gradleText, group, artifact, fixVersion)
        }
    }

    private fun parseGradleCoords(name: String): Pair<String, String>? {
        val parts = name.split(":")
        return if (parts.size >= 2) parts[0] to parts[1] else null
    }

    private fun addResolutionStrategyForce(
        text: String,
        group: String,
        artifact: String,
        version: String,
    ): String {
        val forceLine = "    force \"$group:$artifact:$version\""
        val configBlockPattern =
            Regex(
                "(configurations\\s*\\{[^}]*?\\s*all\\s*\\{[^}]*?)(\\s*\\})",
                RegexOption.DOT_MATCHES_ALL,
            )

        return if (configBlockPattern.containsMatchIn(text)) {
            // Add inside existing configurations.all block
            text.replace(
                configBlockPattern,
                "$1\n$forceLine\n$2",
            )
        } else {
            // Add a new configurations.all block at the end
            "$text\n\nconfigurations.all {\n$forceLine\n}\n"
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// npm Fixer — JSON manipulation with "overrides" support
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object NpmFixer {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Fix an npm dependency.
     *
     * - Direct dependency: update version in `"dependencies"` or `"devDependencies"`.
     * - Transitive dependency: add override in `"overrides"` (npm ≥8.3).
     */
    fun apply(
        packageJsonText: String,
        dependency: Dependency,
        fixVersion: String,
    ): String? {
        val json = JsonParser.parseString(packageJsonText).asJsonObject
        val depName = dependency.name
        val depVersion = dependency.version

        // Sanitise version for JSON (remove leading ^ / ~ for exact comparison)
        val exactOldVersion = depVersion.trimStart('^', '~', '>', '<', '=')

        // Check direct dependencies first
        val directChanged =
            updateDirectDependency(json, depName, exactOldVersion, fixVersion)

        return if (directChanged) {
            gson.toJson(json)
        } else {
            // Transitive — add to overrides
            addOverride(json, depName, fixVersion)
            gson.toJson(json)
        }
    }

    private fun updateDirectDependency(
        json: JsonObject,
        depName: String,
        oldVersion: String,
        newVersion: String,
    ): Boolean {
        for (section in listOf("dependencies", "devDependencies", "peerDependencies")) {
            json.getAsJsonObject(section)?.let { deps ->
                if (deps.has(depName)) {
                    val current = deps.get(depName).asString
                    // Update version (preserve prefix if any, e.g. ^ or ~)
                    val prefix = current.takeWhile { it in setOf('^', '~', '>', '<', '=') }
                    deps.addProperty(depName, "$prefix$newVersion")
                    return true
                }
            }
        }
        return false
    }

    private fun addOverride(
        json: JsonObject,
        depName: String,
        fixVersion: String,
    ) {
        var overrides = json.getAsJsonObject("overrides")
        if (overrides == null) {
            overrides = JsonObject()
            json.add("overrides", overrides)
        }
        overrides.addProperty(depName, fixVersion)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// pip Fixer — requirements.txt + constraints.txt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object PipFixer {
    /**
     * Update a direct dependency in requirements.txt.
     */
    fun applyRequirementsTxt(
        text: String,
        dependency: Dependency,
        fixVersion: String,
    ): String? {
        val depName = dependency.name
        val oldVersion = dependency.version
        val pattern =
            Regex(
                "^[ \\t]*${Regex.escape(depName)}(==|>=|<=|~=|!=|>|<)${Regex.escape(oldVersion)}",
                RegexOption.MULTILINE,
            )
        return if (pattern.containsMatchIn(text)) {
            text.replace(pattern, "$depName\$1$fixVersion")
        } else {
            null // Not a direct dep in requirements.txt
        }
    }

    /**
     * Add or update a constraint in constraints.txt.
     * Used for transitive dependency fixes.
     */
    fun applyConstraintsTxt(
        text: String?,
        dependency: Dependency,
        fixVersion: String,
    ): String {
        val lines = text?.lines()?.toMutableList() ?: mutableListOf()
        val depName = dependency.name
        val constraintPattern =
            Regex(
                "^[ \\t]*${Regex.escape(depName)}(==|>=|<=|~=|!=|>|<)[^\\s]*",
                RegexOption.MULTILINE,
            )

        // Check if constraint already exists
        for ((index, line) in lines.withIndex()) {
            if (constraintPattern.matches(line)) {
                // Update existing
                lines[index] = "$depName>=$fixVersion"
                return lines.joinToString("\n")
            }
        }

        // Add new constraint
        if (lines.isNotEmpty() && lines.last().isNotBlank()) {
            lines.add("")
        }
        lines.add("# OSV security fix for $depName")
        lines.add("$depName>=$fixVersion")
        return lines.joinToString("\n")
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SemVer Comparator
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** Simple SemVer comparator for sorting version strings. */
class SemVerComparator : Comparator<String> {
    override fun compare(
        a: String,
        b: String,
    ): Int {
        val pa = parse(a)
        val pb = parse(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }

    private fun parse(version: String): List<Int> {
        // Strip common prefixes/suffixes
        val cleaned =
            version
                .trimStart('v', 'V', '=', '^', '~', '>', '<')
                .substringBefore('-')
                .substringBefore('+')
        return cleaned.split('.').mapNotNull { it.toIntOrNull() }
    }
}
