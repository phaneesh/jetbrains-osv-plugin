package io.dyuti.osvplugin.inspection

import io.dyuti.osvplugin.api.model.Dependency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for OsVInspection
 */
class OsVInspectionTest {
    
    private val inspection = OsVInspection()
    
    @Test
    fun `inspection has correct display name`() {
        assertEquals("OSV Vulnerability Check", inspection.displayName)
    }
    
    @Test
    fun `inspection has correct short name`() {
        assertEquals("OsVInspection", inspection.shortName)
    }
    
    @Test
    fun `checkDependencies returns list for empty input`() {
        val dependencies = emptyList<Dependency>()
        
        val results = inspection.checkDependencies(dependencies)
        
        assertEquals(0, results.size)
    }
}
