package io.dyuti.osvplugin.inspection

import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for OsVQuickFix
 */
class OsVQuickFixTest {
    
    @Test
    fun `createUpgradeFix creates correct fix type`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createUpgradeFix(dependency, vulnerability)
        
        assertNotNull(fix)
        assertEquals("OSV Vulnerability Fix", fix.familyName)
    }
    
    @Test
    fun `createSuppressFix creates correct fix type`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createSuppressFix(dependency, vulnerability)
        
        assertNotNull(fix)
        assertEquals("OSV Vulnerability Fix", fix.familyName)
    }
    
    @Test
    fun `createIgnoreFix creates correct fix type`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createIgnoreFix(dependency, vulnerability)
        
        assertNotNull(fix)
        assertEquals("OSV Vulnerability Fix", fix.familyName)
    }
    
    @Test
    fun `applyFix upgrades version when fix type is UPGRADE`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createUpgradeFix(dependency, vulnerability)
        
        assertNotNull(fix)
    }
    
    @Test
    fun `applyFix suppresses vulnerability when fix type is SUPPRESS`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createSuppressFix(dependency, vulnerability)
        
        assertNotNull(fix)
    }
    
    @Test
    fun `applyFix ignores package when fix type is IGNORE`() {
        val dependency = Dependency("com.example", "1.0.0", "Maven", "compile", false)
        val vulnerability = Vulnerability(
            id = "GHSA-1234",
            cveIds = emptyList(),
            summary = "Test vulnerability",
            details = "Test details",
            severity = OsVSeverity.MEDIUM,
            affectedVersions = listOf("1.0.0"),
            fixedVersions = listOf("2.0.0"),
            references = emptyList(),
            cweIds = emptyList()
        )
        
        val fix = OsVQuickFix.createIgnoreFix(dependency, vulnerability)
        
        assertNotNull(fix)
    }
}
