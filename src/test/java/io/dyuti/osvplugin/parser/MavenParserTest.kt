package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MavenParserTest {
    @Test
    fun parses_basic_pom_xml() {
        val pomXml = """<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.20</version>
        </dependency>
    </dependencies>
</project>"""

        val parser = MavenParser()
        val dependencies = parser.parse("pom.xml", pomXml)

        assertNotNull(dependencies)
        assertEquals(1, dependencies.size)
        assertEquals("org.springframework:spring-core", dependencies[0].name)
        assertEquals("5.3.20", dependencies[0].version)
    }

    @Test
    fun parses_pom_xml_with_multiple_dependencies() {
        val pomXml = """<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.20</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.12.0</version>
        </dependency>
    </dependencies>
</project>"""

        val parser = MavenParser()
        val dependencies = parser.parse("pom.xml", pomXml)

        assertEquals(2, dependencies.size)
        assertEquals("org.springframework:spring-core", dependencies[0].name)
        assertEquals("org.apache.commons:commons-lang3", dependencies[1].name)
    }

    @Test
    fun parses_pom_xml_with_different_scope() {
        val pomXml = """<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>"""

        val parser = MavenParser()
        val dependencies = parser.parse("pom.xml", pomXml)

        assertEquals(1, dependencies.size)
        assertEquals("junit:junit", dependencies[0].name)
        assertEquals("test", dependencies[0].scope)
    }

    @Test
    fun parses_pom_xml_with_properties() {
        val pomXml = """<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <properties>
        <spring.version>5.3.20</spring.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.20</version>
        </dependency>
    </dependencies>
</project>"""

        val parser = MavenParser()
        val dependencies = parser.parse("pom.xml", pomXml)

        assertEquals(1, dependencies.size)
        assertEquals("org.springframework:spring-core", dependencies[0].name)
    }

    @Test
    fun canHandle_returns_true_for_pomxml() {
        val parser = MavenParser()
        assertEquals(true, parser.canHandle("pom.xml"))
    }

    @Test
    fun canHandle_returns_false_for_non_pom_files() {
        val parser = MavenParser()
        assertEquals(false, parser.canHandle("build.gradle"))
        assertEquals(false, parser.canHandle("package.json"))
        assertEquals(false, parser.canHandle("requirements.txt"))
    }
}
