// Tests for C/C++ conan.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConanParserTest {
    private val parser = ConanParser()

    @Test fun `canHandle returns true for conan-dot-lock`() {
        assertTrue(parser.canHandle("/project/conan.lock"))
    }

    @Test fun `parse conan lock requires array`() {
        val content =
            """
{"version": "0.5", "requires": ["zlib/1.3#rev1", "openssl/3.1.2#rev2", "boost/1.82.0#rev3"]}
            """.trimIndent()
        val deps = parser.parse("conan.lock", content)
        assertEquals(3, deps.size)
        assertEquals("zlib", deps[0].name)
        assertEquals("1.3", deps[0].version)
        assertEquals("ConanCenter", deps[0].ecosystem)
        assertEquals("openssl", deps[1].name)
        assertEquals("3.1.2", deps[1].version)
        assertEquals("boost", deps[2].name)
        assertEquals("1.82.0", deps[2].version)
    }

    @Test fun `parse empty conan lock`() {
        val deps = parser.parse("conan.lock", "{\"version\": \"0.5\", \"requires\": []}")
        assertTrue(deps.isEmpty())
    }
}
