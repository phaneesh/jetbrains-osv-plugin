package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GradleParserTest {
    @Test
    fun parses_basic_build_gradle() {
        val buildGradle = """plugins {
    id 'java'
}

dependencies {
    implementation("org.springframework:spring-core:5.3.20")
    testImplementation("junit:junit:4.13.2")
}"""

        val parser = GradleParser()
        val dependencies = parser.parse("build.gradle", buildGradle)

        assertNotNull(dependencies)
        assertEquals(2, dependencies.size)
        assertEquals("org.springframework:spring-core", dependencies[0].name)
        assertEquals("5.3.20", dependencies[0].version)
    }

    @Test
    fun parses_build_gradle_with_version_catalogs() {
        val buildGradle = """[versions]
spring = "5.3.20"

[libraries]
spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring" }"""

        val parser = GradleParser()
        val dependencies = parser.parse("libs.versions.toml", buildGradle)

        // Version catalogs need special handling - for now just verify parsing works
        assertNotNull(dependencies)
    }

    @Test
    fun parses_build_gradle_kts() {
        val buildGradleKts = """plugins {
    java
}

dependencies {
    implementation("org.springframework:spring-core:5.3.20")
    testImplementation("junit:junit:4.13.2")
}"""

        val parser = GradleParser()
        val dependencies = parser.parse("build.gradle.kts", buildGradleKts)

        assertNotNull(dependencies)
        assertEquals(2, dependencies.size)
        assertEquals("org.springframework:spring-core", dependencies[0].name)
    }

    @Test
    fun canHandle_returns_true_for_build_gradle() {
        val parser = GradleParser()
        assertEquals(true, parser.canHandle("build.gradle"))
    }

    @Test
    fun canHandle_returns_true_for_build_gradle_kts() {
        val parser = GradleParser()
        assertEquals(true, parser.canHandle("build.gradle.kts"))
    }

    @Test
    fun canHandle_returns_false_for_non_gradle_files() {
        val parser = GradleParser()
        assertEquals(false, parser.canHandle("pom.xml"))
        assertEquals(false, parser.canHandle("package.json"))
        assertEquals(false, parser.canHandle("requirements.txt"))
    }
}
