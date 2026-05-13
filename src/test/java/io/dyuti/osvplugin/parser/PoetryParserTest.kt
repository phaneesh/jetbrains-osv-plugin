// Tests for Python poetry.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PoetryParserTest {
    private val parser = PoetryParser()

    @Test fun `canHandle returns true for poetry-dot-lock`() {
        assertTrue(parser.canHandle("/project/poetry.lock"))
    }

    @Test fun `parse poetry lock with multiple packages`() {
        val content =
            """
[[package]]
name = "requests"
version = "2.31.0"
description = "Python HTTP for Humans"
optional = false
python-versions = ">=3.7"

[[package]]
name = "urllib3"
version = "2.1.0"
description = "HTTP library"
optional = false
python-versions = ">=3.8"
            """.trimIndent()
        val deps = parser.parse("poetry.lock", content)
        assertEquals(2, deps.size)
        assertEquals("requests", deps[0].name)
        assertEquals("2.31.0", deps[0].version)
        assertEquals("PyPI", deps[0].ecosystem)
        assertEquals("urllib3", deps[1].name)
        assertEquals("2.1.0", deps[1].version)
    }

    @Test fun `parse empty poetry lock`() {
        val deps = parser.parse("poetry.lock", "")
        assertTrue(deps.isEmpty())
    }
}
