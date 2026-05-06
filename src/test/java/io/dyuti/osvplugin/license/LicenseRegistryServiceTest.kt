// SPDX License Scanner Integration - Registry Service Tests
package io.dyuti.osvplugin.license

import io.dyuti.osvplugin.api.model.Dependency
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit tests for [LicenseRegistryService].
 */
class LicenseRegistryServiceTest {
    @Test
    fun `fetchLicense returns cached value when present`() {
        val service = LicenseRegistryService()
        val dep =
            Dependency(
                name = "com.example:test-lib",
                version = "1.0.0",
                ecosystem = "Maven",
                scope = "compile",
                transitive = false,
            )

        // First call caches UNKNOWN (no network mock)
        val first = service.fetchLicense(dep)
        // Second call should hit cache
        val second = service.fetchLicense(dep)

        assert(first == second)
    }

    @Test
    fun `parseMavenCoordinates handles groupId and artifactId`() {
        val service = LicenseRegistryService()

        val result1 = service.parseMavenCoordinates("com.example:my-lib")
        assert(result1 != null)
        assert(result1!!.first == "com.example")
        assert(result1.second == "my-lib")

        val result2 = service.parseMavenCoordinates("com.example:my-lib:1.0.0")
        assert(result2 != null)
        assert(result2!!.first == "com.example")
        assert(result2.second == "my-lib")
    }

    @Test
    fun `parseMavenCoordinates returns null for invalid formats`() {
        val service = LicenseRegistryService()

        assert(service.parseMavenCoordinates("single-token") == null)
        assert(service.parseMavenCoordinates("") == null)
        assert(
            service.parseMavenCoordinates("a:b:c:d:e") == null,
        )
    }

    @Test
    fun `fetchLicense resolves Maven license from pom`() {
        val client = mock(OkHttpClient::class.java)
        val call1 = mock(Call::class.java)
        val call2 = mock(Call::class.java)
        val response1 = mock(Response::class.java)
        val response2 = mock(Response::class.java)
        val body1 = mock(ResponseBody::class.java)
        val body2 = mock(ResponseBody::class.java)

        val searchJson =
            """
            {
              "response": {
                "docs": [
                  { "latestVersion": "2.0.0" }
                ]
              }
            }
            """.trimIndent()

        val pomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
              <licenses>
                <license>
                  <name>Apache License 2.0</name>
                </license>
              </licenses>
            </project>
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call1, call2)
        whenever(call1.execute()).thenReturn(response1)
        whenever(call2.execute()).thenReturn(response2)
        whenever(response1.isSuccessful).thenReturn(true)
        whenever(response2.isSuccessful).thenReturn(true)
        whenever(response1.body).thenReturn(body1)
        whenever(response2.body).thenReturn(body2)
        whenever(body1.string()).thenReturn(searchJson)
        whenever(body2.string()).thenReturn(pomXml)
        whenever(response1.close()).then { }
        whenever(response2.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "com.example:my-lib",
                version = "1.0.0",
                ecosystem = "Maven",
                scope = "compile",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "Apache License 2.0") { "Expected 'Apache License 2.0' but got '$license'" }
    }

    @Test
    fun `fetchLicense resolves NPM license from registry`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)
        val response = mock(Response::class.java)
        val body = mock(ResponseBody::class.java)

        val npmJson =
            """
            {
              "dist-tags": { "latest": "3.0.0" },
              "versions": {
                "3.0.0": {
                  "license": "MIT"
                }
              }
            }
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body).thenReturn(body)
        whenever(body.string()).thenReturn(npmJson)
        whenever(response.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "lodash",
                version = "2.0.0",
                ecosystem = "npm",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "MIT") { "Expected 'MIT' but got '$license'" }
    }

    @Test
    fun `fetchLicense resolves NPM license from object type field`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)
        val response = mock(Response::class.java)
        val body = mock(ResponseBody::class.java)

        val npmJson =
            """
            {
              "dist-tags": { "latest": "1.0.0" },
              "versions": {
                "1.0.0": {
                  "license": { "type": "ISC", "url": "https://opensource.org/licenses/ISC" }
                }
              }
            }
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body).thenReturn(body)
        whenever(body.string()).thenReturn(npmJson)
        whenever(response.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "some-pkg",
                version = "1.0.0",
                ecosystem = "npm",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "ISC") { "Expected 'ISC' but got '$license'" }
    }

    @Test
    fun `fetchLicense resolves PyPI license from explicit field`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)
        val response = mock(Response::class.java)
        val body = mock(ResponseBody::class.java)

        val pypiJson =
            """
            {
              "info": {
                "license": "BSD-3-Clause",
                "classifiers": []
              }
            }
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body).thenReturn(body)
        whenever(body.string()).thenReturn(pypiJson)
        whenever(response.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "requests",
                version = "2.28.0",
                ecosystem = "PyPI",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "BSD-3-Clause") { "Expected 'BSD-3-Clause' but got '$license'" }
    }

    @Test
    fun `fetchLicense resolves PyPI license from classifiers`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)
        val response = mock(Response::class.java)
        val body = mock(ResponseBody::class.java)

        val pypiJson =
            """
            {
              "info": {
                "license": "",
                "classifiers": [
                  "Development Status :: 5 - Production/Stable",
                  "License :: OSI Approved :: MIT License",
                  "Programming Language :: Python :: 3"
                ]
              }
            }
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body).thenReturn(body)
        whenever(body.string()).thenReturn(pypiJson)
        whenever(response.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "urllib3",
                version = "1.26.0",
                ecosystem = "PyPI",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "MIT License") { "Expected 'MIT License' but got '$license'" }
    }

    @Test
    fun `fetchLicense returns UNKNOWN for unsupported ecosystem`() {
        val service = LicenseRegistryService()
        val dep =
            Dependency(
                name = "some-package",
                version = "1.0.0",
                ecosystem = "NuGet",
                scope = "compile",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "UNKNOWN")
    }

    @Test
    fun `fetchLicense returns UNKNOWN on network failure`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenThrow(RuntimeException("Network error"))

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "lodash",
                version = "1.0.0",
                ecosystem = "npm",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "UNKNOWN")
    }

    @Test
    fun `fetchLicense returns UNKNOWN when registry returns 404`() {
        val client = mock(OkHttpClient::class.java)
        val call = mock(Call::class.java)
        val response = mock(Response::class.java)

        whenever(client.newCall(any<Request>())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(false)
        whenever(response.code).thenReturn(404)
        whenever(response.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "non-existent-package-12345",
                version = "1.0.0",
                ecosystem = "npm",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "UNKNOWN")
    }

    @Test
    fun `fetchLicense resolves Gradle the same as Maven`() {
        val client = mock(OkHttpClient::class.java)
        val call1 = mock(Call::class.java)
        val call2 = mock(Call::class.java)
        val response1 = mock(Response::class.java)
        val response2 = mock(Response::class.java)
        val body1 = mock(ResponseBody::class.java)
        val body2 = mock(ResponseBody::class.java)

        val searchJson =
            """
            {
              "response": {
                "docs": [
                  { "latestVersion": "4.0.0" }
                ]
              }
            }
            """.trimIndent()

        val pomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
              <licenses>
                <license>
                  <name>MIT License</name>
                </license>
              </licenses>
            </project>
            """.trimIndent()

        whenever(client.newCall(any<Request>())).thenReturn(call1, call2)
        whenever(call1.execute()).thenReturn(response1)
        whenever(call2.execute()).thenReturn(response2)
        whenever(response1.isSuccessful).thenReturn(true)
        whenever(response2.isSuccessful).thenReturn(true)
        whenever(response1.body).thenReturn(body1)
        whenever(response2.body).thenReturn(body2)
        whenever(body1.string()).thenReturn(searchJson)
        whenever(body2.string()).thenReturn(pomXml)
        whenever(response1.close()).then { }
        whenever(response2.close()).then { }

        val service = LicenseRegistryService(client)
        val dep =
            Dependency(
                name = "org.example:gradle-lib",
                version = "1.0.0",
                ecosystem = "Gradle",
                scope = "compile",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "MIT License") { "Expected 'MIT License' but got '$license'" }
    }
}
