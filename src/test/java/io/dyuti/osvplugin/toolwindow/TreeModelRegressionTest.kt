package io.dyuti.osvplugin.toolwindow

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TreeModelRegressionTest {
    @Test
    fun `treeModel groups vulnerabilities by severity`() {
        val builder = OsVTreeModelBuilder()

        val vulns =
            mapOf(
                "pom.xml" to
                    listOf(
                        Vulnerability(
                            id = "CVE-1",
                            cveIds = emptyList(),
                            summary = "",
                            details = "",
                            severity = OsVSeverity.CRITICAL,
                            cvssScore = null,
                            affectedVersions = emptyList(),
                            fixedVersions = emptyList(),
                            references = emptyList(),
                            cweIds = emptyList(),
                            packageName = "pkg-a",
                        ),
                        Vulnerability(
                            id = "CVE-2",
                            cveIds = emptyList(),
                            summary = "",
                            details = "",
                            severity = OsVSeverity.HIGH,
                            cvssScore = null,
                            affectedVersions = emptyList(),
                            fixedVersions = emptyList(),
                            references = emptyList(),
                            cweIds = emptyList(),
                            packageName = "pkg-b",
                        ),
                        Vulnerability(
                            id = "CVE-3",
                            cveIds = emptyList(),
                            summary = "",
                            details = "",
                            severity = OsVSeverity.CRITICAL,
                            cvssScore = null,
                            affectedVersions = emptyList(),
                            fixedVersions = emptyList(),
                            references = emptyList(),
                            cweIds = emptyList(),
                            packageName = "pkg-c",
                        ),
                    ),
            )

        val model = builder.buildModelStringKey(vulns)
        val root = model.root as javax.swing.tree.DefaultMutableTreeNode

        assertEquals(1, root.childCount, "Expected 1 module node")
        val moduleNode = root.getChildAt(0) as javax.swing.tree.DefaultMutableTreeNode
        assertEquals(2, moduleNode.childCount, "Expected 2 severity groups (CRITICAL + HIGH)")
    }

    @Test
    fun `buildModel with empty map produces root with no children`() {
        val builder = OsVTreeModelBuilder()
        val model = builder.buildModelStringKey(emptyMap())
        val root = model.root as javax.swing.tree.DefaultMutableTreeNode

        assertEquals(0, root.childCount)
    }

    @Test
    fun `buildModel with multiple modules creates one module node per file`() {
        val builder = OsVTreeModelBuilder()

        val vulns =
            mapOf(
                "pom.xml" to
                    listOf(
                        Vulnerability(
                            id = "V-1",
                            cveIds = emptyList(),
                            summary = "",
                            details = "",
                            severity = OsVSeverity.MEDIUM,
                            cvssScore = null,
                            affectedVersions = emptyList(),
                            fixedVersions = emptyList(),
                            references = emptyList(),
                            cweIds = emptyList(),
                            packageName = "pkg-a",
                        ),
                    ),
                "build.gradle" to
                    listOf(
                        Vulnerability(
                            id = "V-2",
                            cveIds = emptyList(),
                            summary = "",
                            details = "",
                            severity = OsVSeverity.LOW,
                            cvssScore = null,
                            affectedVersions = emptyList(),
                            fixedVersions = emptyList(),
                            references = emptyList(),
                            cweIds = emptyList(),
                            packageName = "pkg-b",
                        ),
                    ),
            )

        val model = builder.buildModelStringKey(vulns)
        val root = model.root as javax.swing.tree.DefaultMutableTreeNode

        assertEquals(2, root.childCount)
    }
}
