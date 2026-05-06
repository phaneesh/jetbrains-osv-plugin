package io.dyuti.osvplugin.utils

import io.dyuti.osvplugin.api.model.OsVSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for SeverityUtil
 */
class SeverityUtilTest {
    @Test
    fun `getColor returns correct color for CRITICAL`() {
        val color = SeverityUtil.getColor(OsVSeverity.CRITICAL)
        assertEquals(220, color.red)
        assertEquals(53, color.green)
        assertEquals(69, color.blue)
    }

    @Test
    fun `getColor returns correct color for HIGH`() {
        val color = SeverityUtil.getColor(OsVSeverity.HIGH)
        assertEquals(255, color.red)
        assertEquals(193, color.green)
        assertEquals(7, color.blue)
    }

    @Test
    fun `getColor returns correct color for MEDIUM`() {
        val color = SeverityUtil.getColor(OsVSeverity.MEDIUM)
        assertEquals(255, color.red)
        assertEquals(165, color.green)
        assertEquals(0, color.blue)
    }

    @Test
    fun `getColor returns correct color for LOW`() {
        val color = SeverityUtil.getColor(OsVSeverity.LOW)
        assertEquals(108, color.red)
        assertEquals(117, color.green)
        assertEquals(125, color.blue)
    }

    @Test
    fun `getPriority returns correct priority for each severity`() {
        assertEquals(1, SeverityUtil.getPriority(OsVSeverity.CRITICAL))
        assertEquals(2, SeverityUtil.getPriority(OsVSeverity.HIGH))
        assertEquals(3, SeverityUtil.getPriority(OsVSeverity.MEDIUM))
        assertEquals(4, SeverityUtil.getPriority(OsVSeverity.LOW))
    }

    @Test
    fun `getSeverityIcon returns correct icon for each severity`() {
        val icon = SeverityUtil.getSeverityIcon(OsVSeverity.CRITICAL)
        assertEquals(16, icon.iconWidth)
        assertEquals(16, icon.iconHeight)
    }

    @Test
    fun `meetsThreshold returns true when severity meets threshold`() {
        assert(SeverityUtil.meetsThreshold(OsVSeverity.CRITICAL, OsVSeverity.MEDIUM))
        assert(SeverityUtil.meetsThreshold(OsVSeverity.HIGH, OsVSeverity.MEDIUM))
        assert(SeverityUtil.meetsThreshold(OsVSeverity.MEDIUM, OsVSeverity.MEDIUM))
    }

    @Test
    fun `meetsThreshold returns false when severity does not meet threshold`() {
        assert(!SeverityUtil.meetsThreshold(OsVSeverity.LOW, OsVSeverity.HIGH))
        assert(!SeverityUtil.meetsThreshold(OsVSeverity.LOW, OsVSeverity.CRITICAL))
    }

    @Test
    fun `getSeverityDescription returns description for each severity`() {
        val description = SeverityUtil.getSeverityDescription(OsVSeverity.CRITICAL)
        assert(description.contains("Critical"))
        assert(description.contains("immediate remediation"))
    }
}
