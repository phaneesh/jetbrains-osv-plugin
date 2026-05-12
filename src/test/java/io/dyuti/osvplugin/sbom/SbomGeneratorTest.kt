package io.dyuti.osvplugin.sbom

import com.google.gson.Gson
import io.dyuti.osvplugin.api.model.Dependency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SbomGeneratorTest {
    private val generator = SbomGenerator("test-app", "2.0.0")
    private val gson = Gson()

    private fun dep(
        name: String,
        version: String,
        ecosystem: String,
        scope: String = "compile",
    ) = Dependency(name, version, ecosystem, scope, false)

    // ─── CycloneDX ───────────────────────────────────────────────

    @Test
    fun `cyclonedx JSON contains bomFormat and specVersion`() {
        val json = generator.generate(listOf(dep("test:pkg", "1.0", "Maven")), SbomFormat.CYCLONEDX_JSON)
        val bom = gson.fromJson(json, CycloneDxBom::class.java)
        assertEquals("CycloneDX", bom.bomFormat)
        assertEquals("1.5", bom.specVersion)
        assertNotNull(bom.serialNumber)
        assertEquals(1, bom.version)
    }

    @Test
    fun `cyclonedx contains metadata with tool info`() {
        val json = generator.generate(listOf(dep("a:b", "1.0", "Maven")), SbomFormat.CYCLONEDX_JSON)
        val bom = gson.fromJson(json, CycloneDxBom::class.java)
        assertNotNull(bom.metadata.timestamp)
        assertEquals(1, bom.metadata.tools.size)
        assertEquals("jetbrains-osv-plugin", bom.metadata.tools[0].name)
    }

    @Test
    fun `cyclonedx contains all dependencies as components`() {
        val deps =
            listOf(
                dep("org:spring", "5.3.0", "Maven", "compile"),
                dep("lodash", "4.17.0", "Npm", "runtime"),
                dep("requests", "2.28.0", "Pypi", "compile"),
            )
        val json = generator.generate(deps, SbomFormat.CYCLONEDX_JSON)
        val bom = gson.fromJson(json, CycloneDxBom::class.java)
        assertEquals(3, bom.components.size)

        val compNames = bom.components.map { it.name }
        assertTrue(compNames.contains("org:spring"))
        assertTrue(compNames.contains("lodash"))
        assertTrue(compNames.contains("requests"))
    }

    @Test
    fun `cyclonedx component has correct purl`() {
        val deps = listOf(dep("com.google:guava", "31.0", "Maven", "compile"))
        val json = generator.generate(deps, SbomFormat.CYCLONEDX_JSON)
        val bom = gson.fromJson(json, CycloneDxBom::class.java)
        assertEquals("pkg:maven/com.google/guava@31.0", bom.components[0].purl)
    }

    @Test
    fun `cyclonedx handles empty dependency list`() {
        val json = generator.generate(emptyList(), SbomFormat.CYCLONEDX_JSON)
        val bom = gson.fromJson(json, CycloneDxBom::class.java)
        assertTrue(bom.components.isEmpty())
    }

    // ─── SPDX JSON ───────────────────────────────────────────────

    @Test
    fun `spdx JSON contains spdxVersion and document info`() {
        val json = generator.generate(listOf(dep("x:y", "1.0", "Maven")), SbomFormat.SPDX_JSON)
        val doc = gson.fromJson(json, SpdxDocument::class.java)
        assertEquals("SPDX-2.3", doc.spdxVersion)
        assertEquals("SPDXRef-DOCUMENT", doc.spdxId)
        assertEquals("test-app", doc.name)
        assertTrue(doc.documentNamespace.contains("test-app"))
    }

    @Test
    fun `spdx JSON contains creation info`() {
        val json = generator.generate(listOf(dep("x:y", "1.0", "Maven")), SbomFormat.SPDX_JSON)
        val doc = gson.fromJson(json, SpdxDocument::class.java)
        assertNotNull(doc.creationInfo.created)
        assertTrue(doc.creationInfo.creators.any { it.contains("jetbrains-osv-plugin") })
    }

    @Test
    fun `spdx JSON contains packages with purl refs`() {
        val deps =
            listOf(
                dep("org:spring", "5.3.0", "Maven"),
                dep("lodash", "4.17.0", "Npm"),
            )
        val json = generator.generate(deps, SbomFormat.SPDX_JSON)
        val doc = gson.fromJson(json, SpdxDocument::class.java)
        assertEquals(2, doc.packages.size)

        val pkg0 = doc.packages[0]
        assertEquals("org:spring", pkg0.name)
        assertNotNull(pkg0.externalRefs)
        assertTrue(pkg0.externalRefs!!.isNotEmpty())
        assertEquals("purl", pkg0.externalRefs!![0].referenceType)
    }

    @Test
    fun `spdx JSON contains DESCRIBES relationships`() {
        val deps = listOf(dep("a:b", "1.0", "Maven"), dep("c:d", "2.0", "Maven"))
        val json = generator.generate(deps, SbomFormat.SPDX_JSON)
        val doc = gson.fromJson(json, SpdxDocument::class.java)
        assertEquals(2, doc.relationships.size)
        assertTrue(doc.relationships.all { it.relationshipType == "DESCRIBES" })
        assertTrue(doc.relationships.all { it.spdxElementId == "SPDXRef-DOCUMENT" })
    }

    // ─── SPDX Tag-Value ──────────────────────────────────────────

    @Test
    fun `spdx tag-value contains SPDXVersion header`() {
        val tv = generator.generate(listOf(dep("x:y", "1.0", "Maven")), SbomFormat.SPDX_TAGVALUE)
        assertTrue(tv.contains("SPDXVersion: SPDX-2.3"))
        assertTrue(tv.contains("SPDXID: SPDXRef-DOCUMENT"))
        assertTrue(tv.contains("DocumentName: test-app"))
    }

    @Test
    fun `spdx tag-value contains package blocks`() {
        val deps = listOf(dep("p1", "1.0", "Maven"), dep("p2", "2.0", "Npm"))
        val tv = generator.generate(deps, SbomFormat.SPDX_TAGVALUE)
        assertTrue(tv.contains("PackageName: p1"))
        assertTrue(tv.contains("PackageName: p2"))
        assertTrue(tv.contains("PackageVersion: 1.0"))
        assertTrue(tv.contains("PackageVersion: 2.0"))
    }

    @Test
    fun `spdx tag-value contains externalRef purls`() {
        val deps = listOf(dep("com.google:guava", "31.0", "Maven"))
        val tv = generator.generate(deps, SbomFormat.SPDX_TAGVALUE)
        assertTrue(tv.contains("ExternalRef: PACKAGE-MANAGER purl pkg:maven/com.google/guava@31.0"))
    }

    @Test
    fun `spdx tag-value contains relationship lines`() {
        val deps = listOf(dep("a", "1", "Maven"), dep("b", "2", "Maven"))
        val tv = generator.generate(deps, SbomFormat.SPDX_TAGVALUE)
        val lines = tv.lines().filter { it.startsWith("Relationship:") }
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.contains("DESCRIBES") })
    }

    // ─── PURL construction ──────────────────────────────────────

    @Test
    fun `purl for maven with group artifact`() {
        val purl = generator.toPurl(dep("com.example:lib", "1.0", "Maven"))
        assertEquals("pkg:maven/com.example/lib@1.0", purl)
    }

    @Test
    fun `purl for gradle uses maven type`() {
        val purl = generator.toPurl(dep("org.gradle:plugin", "2.0", "Gradle"))
        assertEquals("pkg:maven/org.gradle/plugin@2.0", purl)
    }

    @Test
    fun `purl for npm`() {
        val purl = generator.toPurl(dep("lodash", "4.17.21", "Npm"))
        assertEquals("pkg:npm/lodash@4.17.21", purl)
    }

    @Test
    fun `purl for pypi`() {
        val purl = generator.toPurl(dep("requests", "2.28.1", "Pypi"))
        assertEquals("pkg:pypi/requests@2.28.1", purl)
    }

    @Test
    fun `purl for maven without colon preserves name`() {
        val purl = generator.toPurl(dep("single-name", "1.0", "Maven"))
        assertEquals("pkg:maven/single-name@1.0", purl)
    }

    @Test
    fun `purl for unknown ecosystem falls through`() {
        val purl = generator.toPurl(dep("foo-bar", "1.0", "CustomEcosystem"))
        assertEquals("pkg:customecosystem/foo-bar@1.0", purl)
    }

    // ─── edge cases ─────────────────────────────────────────────

    @Test
    fun `all three formats produce non-empty output`() {
        val deps = listOf(dep("x", "1", "Maven"))
        SbomFormat.entries.forEach { fmt ->
            val out = generator.generate(deps, fmt)
            assertTrue(out.isNotBlank(), "Format $fmt produced empty output")
        }
    }

    @Test
    fun `empty deps in spdx tag-value still has header`() {
        val tv = generator.generate(emptyList(), SbomFormat.SPDX_TAGVALUE)
        assertTrue(tv.contains("SPDXVersion: SPDX-2.3"))
        assertFalse(tv.contains("PackageName:"))
    }
}
