// Cross-language PURL generation for all 12 ecosystems
package io.dyuti.osvplugin.sbom

import io.dyuti.osvplugin.api.model.Dependency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CrossLanguagePurlTest {
    private val generator = SbomGenerator()

    private fun dep(
        name: String,
        version: String,
        ecosystem: String,
    ): Dependency = Dependency(name = name, version = version, ecosystem = ecosystem, scope = "runtime", transitive = true)

    @Test
    fun `purl for all ecosystems`() {
        val testCases =
            listOf(
                // Java
                "maven/org.springframework/spring-core@5.3.21" to dep("org.springframework:spring-core", "5.3.21", "Maven"),
                "maven/com.google.guava/guava@32.1.0" to dep("com.google.guava:guava", "32.1.0", "Maven"),
                "maven/io.netty/netty-common@4.1.96" to dep("io.netty:netty-common", "4.1.96", "Gradle"),
                // JavaScript
                "npm/lodash@4.17.21" to dep("lodash", "4.17.21", "npm"),
                "npm/@angular/core@16.0.0" to dep("@angular/core", "16.0.0", "npm"),
                // Python
                "pypi/requests@2.31.0" to dep("requests", "2.31.0", "PyPI"),
                "pypi/django@4.2.7" to dep("django", "4.2.7", "PyPI"),
                // Go
                "golang/github.com/gin-gonic/gin@1.9.1" to dep("github.com/gin-gonic/gin", "1.9.1", "Go"),
                // Rust
                "cargo/serde@1.0.190" to dep("serde", "1.0.190", "crates.io"),
                "cargo/tokio@1.34.0" to dep("tokio", "1.34.0", "crates.io"),
                // PHP
                "composer/symfony::console@6.3.4" to dep("symfony/console", "6.3.4", "Packagist"),
                // Ruby
                "gem/activesupport@7.1.0" to dep("activesupport", "7.1.0", "RubyGems"),
                // Dart
                "pub/async@2.11.0" to dep("async", "2.11.0", "Pub"),
                // .NET
                "nuget/Newtonsoft.Json@13.0.3" to dep("Newtonsoft.Json", "13.0.3", "NuGet"),
                // Haskell
                "hackage/aeson@2.1.2.1" to dep("aeson", "2.1.2.1", "Hackage"),
                // Elixir
                "hex/decimal@2.1.1" to dep("decimal", "2.1.1", "Hex"),
                // R
                "cran/dplyr@1.1.3" to dep("dplyr", "1.1.3", "CRAN"),
                "cran/ggplot2@3.4.3" to dep("ggplot2", "3.4.3", "CRAN"),
                // C/C++
                "conan/zlib@1.3" to dep("zlib", "1.3", "ConanCenter"),
            )

        testCases.forEach { (expected, dependency) ->
            assertEquals(
                "pkg:$expected",
                generator.toPurl(dependency),
                "PURL mismatch for ${dependency.ecosystem} ${dependency.name}",
            )
        }
    }
}
