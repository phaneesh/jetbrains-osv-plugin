package io.dyuti.osvplugin.api

import io.dyuti.osvplugin.api.model.Dependency
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockOsVApiServiceTest {
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
    fun `queryVulnerabilities returns parsed vulnerabilities`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"vulns":[
                        {
                            "id":"GHSA-xxx",
                            "summary":"Test vuln",
                            "severity":[{"type":"CVSS_V3","score":"7.5"}],
                            "affected":[{
                                "package":{"name":"test-pkg","ecosystem":"Maven"},
                                "ranges":[{"type":"ECOSYSTEM","events":[
                                    {"introduced":"0"},{"fixed":"1.0.1"}
                                ]}]
                            }],
                            "packageName":"test-pkg"
                        }
                    ]}""",
                ),
        )

        val result = apiService.queryVulnerabilities("test-pkg", "Maven", "1.0.0")

        assertEquals(1, result.size)
        assertEquals("GHSA-xxx", result[0].id)
        assertEquals("Test vuln", result[0].summary)
        assertNotNull(result[0].packageName)
        assertEquals("test-pkg", result[0].packageName)
    }

    @Test
    fun `queryVulnerabilities throws OsVApiException on 404`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val exception =
            org.junit.jupiter.api.assertThrows<OsVApiException> {
                apiService.queryVulnerabilities("unknown-pkg", "Maven", "1.0.0")
            }
        assertTrue(exception.message!!.contains("404"))
    }

    @Test
    fun `batchQueryVulnerabilities handles multiple responses`() {
        repeat(3) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"vulns":[]}"""),
            )
        }

        val deps =
            listOf(
                Dependency("pkg-1", "1.0.0", "Maven", "compile", false),
                Dependency("pkg-2", "2.0.0", "Maven", "compile", false),
                Dependency("pkg-3", "3.0.0", "Maven", "compile", false),
            )

        val result = apiService.batchQueryVulnerabilities(deps)

        assertEquals(3, result.size)
        assertTrue(result.values.all { it.isEmpty() })
    }

    @Test
    fun `queryVulnerabilities parses severity correctly`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"vulns":[
                        {
                            "id":"GHSA-critical",
                            "summary":"Critical bug",
                            "severity":[{"type":"CVSS_V3","score":"9.8"}],
                            "affected":[{
                                "package":{"name":"my-pkg","ecosystem":"Maven"},
                                "ranges":[{"type":"ECOSYSTEM","events":[
                                    {"introduced":"0"},{"fixed":"2.0.0"}
                                ]}]
                            }],
                            "packageName":"my-pkg"
                        }
                    ]}""",
                ),
        )

        val result = apiService.queryVulnerabilities("my-pkg", "Maven", "1.0.0")

        assertEquals(1, result.size)
        assertEquals(io.dyuti.osvplugin.api.model.OsVSeverity.CRITICAL, result[0].severity)
    }
}
