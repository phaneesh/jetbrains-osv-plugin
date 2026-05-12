package io.dyuti.osvplugin.api

import io.dyuti.osvplugin.api.model.Dependency
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OsVApiServicePerformanceTest {
    private val mockWebServer = MockWebServer()
    private lateinit var apiService: OsVApiService

    @BeforeEach
    fun setup() {
        mockWebServer.start()
        val httpClient = HttpClient.newBuilder().build()
        apiService =
            OsVApiService(
                httpClient = httpClient,
                baseUrl = mockWebServer.url("/v1/query").toString(),
            )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `batchQuery completes within 30 seconds for 100 dependencies`() {
        repeat(100) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"vulns":[]}""")
                    .setBodyDelay(50L, TimeUnit.MILLISECONDS),
            )
        }

        val dependencies =
            (1..100).map {
                Dependency("test-pkg-$it", "1.0.0", "Maven", "compile", false)
            }

        val start = System.currentTimeMillis()
        val result = apiService.batchQueryVulnerabilities(dependencies)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(100, result.size)
        assertTrue(elapsed < 30000, "Expected < 30s, took ${elapsed}ms")
    }

    @Test
    fun `batchQuery handles 20 concurrent requests with delay`() {
        repeat(20) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"vulns":[]}""")
                    .setHeadersDelay(100L, TimeUnit.MILLISECONDS),
            )
        }

        val dependencies =
            (1..20).map {
                Dependency("test-pkg-$it", "1.0.0", "Maven", "compile", false)
            }

        val result = apiService.batchQueryVulnerabilities(dependencies)
        assertEquals(20, result.size)
    }
}
