package io.dyuti.osvplugin.utils

import io.dyuti.osvplugin.api.model.OsVSeverity
import io.dyuti.osvplugin.api.model.Vulnerability
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CacheManagerConcurrencyTest {
    private fun makeVuln(id: String): Vulnerability =
        Vulnerability(
            id = id,
            cveIds = emptyList(),
            summary = "",
            details = "",
            severity = OsVSeverity.HIGH,
            cvssScore = null,
            affectedVersions = emptyList(),
            fixedVersions = emptyList(),
            references = emptyList(),
            cweIds = emptyList(),
            packageName = "test-pkg",
        )

    @RepeatedTest(10)
    fun `concurrent cacheVulnerabilities does not corrupt state`() {
        val cacheManager = CacheManager()
        val latch = CountDownLatch(50)
        val threads =
            (1..50).map { i ->
                Thread {
                    cacheManager.cacheVulnerabilities("key-$i", listOf(makeVuln("CVE-$i")))
                    latch.countDown()
                }
            }
        threads.forEach { it.start() }
        latch.await()

        (1..50).forEach { i ->
            val result = cacheManager.getCachedVulnerabilities("key-$i")
            assertEquals(1, result?.size, "Expected 1 cached item for key-$i")
            assertEquals("CVE-$i", result!![0].id, "Wrong ID for key-$i")
        }
    }

    @RepeatedTest(10)
    fun `concurrent read and write does not throw`() {
        val cacheManager = CacheManager()
        val latch = CountDownLatch(100)
        val threads =
            (1..100).map { i ->
                Thread {
                    if (i % 2 == 0) {
                        cacheManager.cacheVulnerabilities("shared-key", listOf(makeVuln("CVE-$i")))
                    } else {
                        cacheManager.getCachedVulnerabilities("shared-key")
                    }
                    latch.countDown()
                }
            }
        threads.forEach { it.start() }
        latch.await()
        assertTrue(true) // no exception = pass
    }
}
