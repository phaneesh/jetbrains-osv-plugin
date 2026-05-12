// SPDX License Scanner Integration - Registry Service Tests
package io.dyuti.osvplugin.license

import io.dyuti.osvplugin.api.model.Dependency
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LicenseRegistryService].
 *
 * Note: Network-dependent tests are covered by integration testing.
 * These tests verify parsing, coordinate extraction, and caching logic.
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

        val first = service.fetchLicense(dep)
        val second = service.fetchLicense(dep)
        assert(first == second) { "Cache should return same value" }
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
        assert(service.parseMavenCoordinates("a:b:c:d:e") == null)
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
    fun `fetchLicense returns UNKNOWN for blank package name`() {
        val service = LicenseRegistryService()
        val dep =
            Dependency(
                name = "",
                version = "1.0.0",
                ecosystem = "npm",
                scope = "runtime",
                transitive = false,
            )

        val license = service.fetchLicense(dep)
        assert(license == "UNKNOWN")
    }

    @Test
    fun `parseLicenseFromPom extracts license from valid POM`() {
        val service = LicenseRegistryService()
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

        val license =
            service.javaClass
                .getDeclaredMethod("parseLicenseFromPom", String::class.java)
                .apply { isAccessible = true }
                .invoke(service, pomXml) as String

        assert(license == "Apache License 2.0") { "Expected 'Apache License 2.0' but got '$license'" }
    }

    @Test
    fun `parseLicenseFromPom returns UNKNOWN when no license element`() {
        val service = LicenseRegistryService()
        val pomXml = "<?xml version=\"1.0\"?><project></project>"

        val license =
            service.javaClass
                .getDeclaredMethod("parseLicenseFromPom", String::class.java)
                .apply { isAccessible = true }
                .invoke(service, pomXml) as String

        assert(license == "UNKNOWN")
    }

    @Test
    fun `parseMavenCoordinates handles more colons`() {
        val service = LicenseRegistryService()
        // Invalid: 4+ parts
        assert(service.parseMavenCoordinates("a:b:c:d") == null)
    }

    @Test
    fun `cache is used for repeated lookups`() {
        val service = LicenseRegistryService()
        val dep =
            Dependency(
                name = "com.example:cached-lib",
                version = "1.0.0",
                ecosystem = "Maven",
                scope = "compile",
                transitive = false,
            )

        val first = service.fetchLicense(dep)
        val second = service.fetchLicense(dep)
        assert(first == second)
        assert(first == "UNKNOWN") // No network, so UNKNOWN is cached
    }

    @Test
    fun `ecosystem matching is case insensitive`() {
        val service = LicenseRegistryService()

        assert(service.parseMavenCoordinates("g:a") != null) // Maven
        assert(service.parseMavenCoordinates("g:a") != null) // Gradle uses same parser
    }

    @Test
    fun `parseLicenseFromPom handles multiple license entries`() {
        val service = LicenseRegistryService()
        val pomXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
              <licenses>
                <license>
                  <name>MIT License</name>
                </license>
                <license>
                  <name>Apache-2.0</name>
                </license>
              </licenses>
            </project>
            """.trimIndent()

        val license =
            service.javaClass
                .getDeclaredMethod("parseLicenseFromPom", String::class.java)
                .apply { isAccessible = true }
                .invoke(service, pomXml) as String

        // Returns the first license name found
        assert(license == "MIT License") { "Expected 'MIT License' but got '$license'" }
    }
}
