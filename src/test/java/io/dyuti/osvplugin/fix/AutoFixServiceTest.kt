package io.dyuti.osvplugin.fix

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for the AutoFixService and its ecosystem-specific fixers.
 */
class AutoFixServiceTest {
    // ═══════════════════════════════════════════════════════════════════
    // SemVer / findBestFixVersion
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `findBestFixVersion picks latest SemVer`() {
        val service = AutoFixService.getInstance()
        val vuln =
            Vulnerability(
                id = "GHSA-1234",
                cveIds = emptyList(),
                summary = "Test",
                details = "",
                severity = io.dyuti.osvplugin.api.model.OsVSeverity.HIGH,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = listOf("1.2.3", "1.2.4", "1.3.0", "1.0.0"),
                references = emptyList(),
                cweIds = emptyList(),
            )

        assertEquals("1.3.0", service.findBestFixVersion(vuln))
    }

    @Test
    fun `findBestFixVersion handles suffixes and prefixes`() {
        val service = AutoFixService.getInstance()
        val vuln =
            Vulnerability(
                id = "GHSA-1234",
                cveIds = emptyList(),
                summary = "Test",
                details = "",
                severity = io.dyuti.osvplugin.api.model.OsVSeverity.HIGH,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = listOf("v1.2.0", "v1.10.0", "1.2.3-beta"),
                references = emptyList(),
                cweIds = emptyList(),
            )

        // v1.10.0 > v1.2.0
        assertEquals("v1.10.0", service.findBestFixVersion(vuln))
    }

    @Test
    fun `findBestFixVersion returns null when empty`() {
        val service = AutoFixService.getInstance()
        val vuln =
            Vulnerability(
                id = "GHSA-1234",
                cveIds = emptyList(),
                summary = "Test",
                details = "",
                severity = io.dyuti.osvplugin.api.model.OsVSeverity.HIGH,
                cvssScore = null,
                affectedVersions = emptyList(),
                fixedVersions = emptyList(),
                references = emptyList(),
                cweIds = emptyList(),
            )

        assertNull(service.findBestFixVersion(vuln))
    }

    // ═══════════════════════════════════════════════════════════════════
    // Maven Fixer
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `MavenFixer updates direct dependency version literal`() {
        val pom =
            """
            <project>
              <dependencies>
                <dependency>
                  <groupId>org.example</groupId>
                  <artifactId>lib</artifactId>
                  <version>1.0.0</version>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent()

        val dep = Dependency("org.example:lib", "1.0.0", "Maven", "compile", false, 42)
        val result = MavenFixer.apply(pom, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("<version>2.0.0</version>"))
    }

    @Test
    fun `MavenFixer updates property reference in dependency`() {
        val pom =
            """
            <project>
              <properties>
                <lib.version>1.0.0</lib.version>
              </properties>
              <dependencies>
                <dependency>
                  <groupId>org.example</groupId>
                  <artifactId>lib</artifactId>
                  <version>${'$'}{lib.version}</version>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent()

        val dep = Dependency("org.example:lib", "1.0.0", "Maven", "compile", false, 42)
        val result = MavenFixer.apply(pom, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("<lib.version>2.0.0</lib.version>"))
        assertTrue(result.contains("<version>${'$'}{lib.version}</version>"))
    }

    @Test
    fun `MavenFixer adds dependencyManagement for transitive dependency`() {
        val pom =
            """
            <project>
              <dependencies>
                <dependency>
                  <groupId>org.other</groupId>
                  <artifactId>other-lib</artifactId>
                  <version>3.0.0</version>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent()

        // This dependency is NOT declared directly — simulate transitive
        val dep = Dependency("org.example:lib", "1.0.0", "Maven", "compile", true, null)
        val result = MavenFixer.apply(pom, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("<dependencyManagement>"))
        assertTrue(result.contains("<groupId>org.example</groupId>"))
        assertTrue(result.contains("<artifactId>lib</artifactId>"))
        assertTrue(result.contains("<version>2.0.0</version>"))
    }

    @Test
    fun `MavenFixer updates existing dependencyManagement version`() {
        val pom =
            """
            <project>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.example</groupId>
                    <artifactId>lib</artifactId>
                    <version>1.0.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """.trimIndent()

        val dep = Dependency("org.example:lib", "1.0.0", "Maven", "compile", true, null)
        val result = MavenFixer.apply(pom, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("<version>2.0.0</version>"))
    }

    @Test
    fun `MavenFixer parseMavenCoords returns null for invalid format`() {
        assertNull(MavenFixer.parseMavenCoords("invalid"))
        assertNull(MavenFixer.parseMavenCoords(""))
    }

    // ═══════════════════════════════════════════════════════════════════
    // Gradle Fixer
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `GradleFixer updates direct dependency version`() {
        val gradle =
            """
            dependencies {
                implementation 'org.example:lib:1.0.0'
            }
            """.trimIndent()

        val dep = Dependency("org.example:lib", "1.0.0", "Gradle", "compile", false, 42)
        val result = GradleFixer.apply(gradle, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("'org.example:lib:2.0.0'"))
    }

    @Test
    fun `GradleFixer adds resolutionStrategy for transitive dependency`() {
        val gradle =
            """
            dependencies {
                implementation 'org.other:other-lib:3.0.0'
            }
            """.trimIndent()

        val dep = Dependency("org.example:lib", "1.0.0", "Gradle", "compile", true, null)
        val result = GradleFixer.apply(gradle, dep, "2.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("configurations.all"))
        assertTrue(result.contains("force \"org.example:lib:2.0.0\""))
    }

    // ═══════════════════════════════════════════════════════════════════
    // npm Fixer
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `NpmFixer updates direct dependency version`() {
        val pkg =
            """
            {
              "name": "test",
              "dependencies": {
                "lodash": "^4.17.0"
              }
            }
            """.trimIndent()

        val dep = Dependency("lodash", "4.17.0", "npm", "runtime", false, 10)
        val result = NpmFixer.apply(pkg, dep, "4.17.21")

        assertNotNull(result)
        assertTrue(result!!.contains("\"lodash\": \"^4.17.21\""))
    }

    @Test
    fun `NpmFixer adds overrides for transitive dependency`() {
        val pkg =
            """
            {
              "name": "test",
              "dependencies": {
                "express": "^4.18.0"
              }
            }
            """.trimIndent()

        // lodash is NOT in dependencies — simulate transitive
        val dep = Dependency("lodash", "4.17.0", "npm", "runtime", true, null)
        val result = NpmFixer.apply(pkg, dep, "4.17.21")

        assertNotNull(result)
        assertTrue(result!!.contains("\"overrides\""))
        assertTrue(result.contains("\"lodash\": \"4.17.21\""))
    }

    // ═══════════════════════════════════════════════════════════════════
    // pip Fixer
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `PipFixer updates direct dependency in requirements txt`() {
        val req =
            """
            django==3.2.0
            requests>=2.25.0
            """.trimIndent()

        val dep = Dependency("django", "3.2.0", "PyPI", "runtime", false, 1)
        val result = PipFixer.applyRequirementsTxt(req, dep, "4.0.0")

        assertNotNull(result)
        assertTrue(result!!.contains("django==4.0.0"))
        assertTrue(result.contains("requests>=2.25.0"))
    }

    @Test
    fun `PipFixer returns null when dependency not in requirements`() {
        val req = "requests>=2.25.0\n"
        val dep = Dependency("django", "3.2.0", "PyPI", "runtime", true, null)
        val result = PipFixer.applyRequirementsTxt(req, dep, "4.0.0")

        assertNull(result)
    }

    @Test
    fun `PipFixer adds new constraint`() {
        val dep = Dependency("django", "3.2.0", "PyPI", "runtime", true, null)
        val result = PipFixer.applyConstraintsTxt(null, dep, "4.0.0")

        assertTrue(result.contains("django>=4.0.0"))
        assertTrue(result.contains("# OSV security fix for django"))
    }

    @Test
    fun `PipFixer updates existing constraint`() {
        val existing =
            """
            django>=3.2.0
            requests>=2.0.0
            """.trimIndent()

        val dep = Dependency("django", "3.2.0", "PyPI", "runtime", true, null)
        val result = PipFixer.applyConstraintsTxt(existing, dep, "4.0.0")

        assertTrue(result.contains("django>=4.0.0"))
        assertTrue(result.contains("requests>=2.0.0"))
    }

    // ═══════════════════════════════════════════════════════════════════
    // SemVer Comparator
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `SemVerComparator sorts versions correctly`() {
        val cmp = SemVerComparator()
        val versions = listOf("1.2.3", "1.10.0", "1.2.10", "2.0.0", "0.9.0")
        val sorted = versions.sortedWith(cmp)

        assertEquals(listOf("0.9.0", "1.2.3", "1.2.10", "1.10.0", "2.0.0"), sorted)
    }

    @Test
    fun `SemVerComparator handles prefixes`() {
        val cmp = SemVerComparator()
        assertTrue(cmp.compare("v1.0.0", "v2.0.0") < 0)
        assertTrue(cmp.compare("^1.5.0", "^1.10.0") < 0)
    }
}
