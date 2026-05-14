// Tests for risk scoring — pure functions only, no IntelliJ dependencies
package io.dyuti.osvplugin.risk

import io.dyuti.osvplugin.api.model.OsVSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RiskScoringTest {
    private lateinit var service: RiskScoringService

    @BeforeEach
    fun setup() {
        service = RiskScoringService()
    }

    @Test
    fun `composite score combines all factors`() {
        // CVSS 9.0, EPSS 0.90, KEV=true, exploit=true
        val score = service.computeComposite(9.0, 0.90, true, true)
        // = (9/10*100*0.40) + (90*0.30) + (100*0.20) + (100*0.10)
        // = 36 + 27 + 20 + 10 = 93
        assertEquals(93.0, score, 0.1)
    }

    @Test
    fun `null cvss defaults to 5_0`() {
        val withNull = service.computeComposite(null, 0.0, false, false)
        val withDefault = service.computeComposite(5.0, 0.0, false, false)
        assertEquals(withDefault, withNull, 0.01)
    }

    @Test
    fun `null epss defaults to 0_0`() {
        val score = service.computeComposite(5.0, null, false, false)
        // = (50*0.40) + (0*0.30) + (30*0.20) + (30*0.10) = 20 + 0 + 6 + 3 = 29
        assertEquals(29.0, score, 0.1)
    }

    @Test
    fun `no kev and no exploit gives partial credit`() {
        val score = service.computeComposite(7.0, 0.5, false, false)
        // = (70*0.40) + (50*0.30) + (30*0.20) + (30*0.10) = 28 + 15 + 6 + 3 = 52
        assertTrue(score > 50.0, "Should be above MEDIUM threshold")
        assertTrue(score < 60.0, "Should be below HIGH threshold")
    }

    @Test
    fun `composite to level critical above 80`() {
        assertEquals(RiskLevel.CRITICAL, service.compositeToLevel(80.0))
        assertEquals(RiskLevel.CRITICAL, service.compositeToLevel(95.0))
    }

    @Test
    fun `composite to level high 60-79`() {
        assertEquals(RiskLevel.HIGH, service.compositeToLevel(60.0))
        assertEquals(RiskLevel.HIGH, service.compositeToLevel(79.9))
    }

    @Test
    fun `composite to level medium 40-59`() {
        assertEquals(RiskLevel.MEDIUM, service.compositeToLevel(40.0))
        assertEquals(RiskLevel.MEDIUM, service.compositeToLevel(59.0))
    }

    @Test
    fun `composite to level low 20-39`() {
        assertEquals(RiskLevel.LOW, service.compositeToLevel(20.0))
        assertEquals(RiskLevel.LOW, service.compositeToLevel(39.0))
    }

    @Test
    fun `composite to level minimal below 20`() {
        assertEquals(RiskLevel.MINIMAL, service.compositeToLevel(19.9))
        assertEquals(RiskLevel.MINIMAL, service.compositeToLevel(0.0))
    }

    @Test
    fun `risk level to severity mapping`() {
        assertEquals(OsVSeverity.CRITICAL, service.riskLevelToSeverity(RiskLevel.CRITICAL))
        assertEquals(OsVSeverity.HIGH, service.riskLevelToSeverity(RiskLevel.HIGH))
        assertEquals(OsVSeverity.MEDIUM, service.riskLevelToSeverity(RiskLevel.MEDIUM))
        assertEquals(OsVSeverity.LOW, service.riskLevelToSeverity(RiskLevel.LOW))
        assertEquals(OsVSeverity.LOW, service.riskLevelToSeverity(RiskLevel.MINIMAL))
    }

    @Test
    fun `epss priority descriptions`() {
        assertTrue(service.getEpssPriority(0.99).contains("Top 1%"))
        assertTrue(service.getEpssPriority(0.96).contains("Top 5%"))
        assertTrue(service.getEpssPriority(0.92).contains("Top 10%"))
        assertTrue(service.getEpssPriority(0.80).contains("Top 25%"))
        assertTrue(service.getEpssPriority(0.60).contains("Above average"))
        assertTrue(service.getEpssPriority(0.30).contains("Below average"))
        assertTrue(service.getEpssPriority(null).contains("unavailable"))
    }

    @Test
    fun `risk assessment model stores fields`() {
        val assessment =
            RiskAssessment(
                cveId = "CVE-2021-44228",
                cvssScore = 10.0,
                epssScore = 0.97,
                epssPercentile = 0.99,
                isCisaKev = true,
                hasPublicExploit = true,
                compositeScore = 93.0,
                riskLevel = RiskLevel.CRITICAL,
            )
        assertEquals("CVE-2021-44228", assessment.cveId)
        assertEquals(10.0, assessment.cvssScore)
        assertEquals(0.97, assessment.epssScore)
        assertTrue(assessment.isCisaKev)
        assertTrue(assessment.hasPublicExploit)
        assertEquals(RiskLevel.CRITICAL, assessment.riskLevel)
    }

    @Test
    fun `epss entry parses correctly`() {
        val entry = EpssEntry(cve = "CVE-2021-44228", epss = "0.97565", percentile = "1.00000")
        assertEquals("CVE-2021-44228", entry.cve)
        assertEquals("0.97565", entry.epss)
        assertEquals("1.00000", entry.percentile)
    }

    @Test
    fun `cisa kev entry parses correctly`() {
        val entry =
            CisaKevEntry(
                cveID = "CVE-2021-44228",
                vulnerabilityName = "Log4j2 RCE",
                vendorProject = "Apache",
                product = "Log4j2",
            )
        assertEquals("CVE-2021-44228", entry.cveID)
        assertEquals("Log4j2 RCE", entry.vulnerabilityName)
    }

    @Test
    fun `max composite score at perfect factors`() {
        val score = service.computeComposite(10.0, 1.0, true, true)
        assertEquals(100.0, score, 0.01)
    }

    @Test
    fun `min composite score at worst known factors`() {
        val score = service.computeComposite(0.0, 0.0, false, false)
        // = (0*0.40) + (0*0.30) + (30*0.20) + (30*0.10) = 0 + 0 + 6 + 3 = 9
        assertTrue(score > 0.0)
        assertTrue(score < 20.0)
    }
}
