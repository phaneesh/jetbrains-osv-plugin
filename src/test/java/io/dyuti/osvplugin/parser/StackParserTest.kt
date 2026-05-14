// Tests for Haskell stack.yaml.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StackParserTest {
    private val parser = StackParser()

    @Test fun `canHandle returns true for stack lock and cabal freeze`() {
        assertTrue(parser.canHandle("/project/stack.yaml.lock"))
        assertTrue(parser.canHandle("/project/cabal.project.freeze"))
    }

    @Test fun `parse stack yaml lock hackage lines`() {
        val content =
            """
packages:
- completed:
    hackage: aeson-2.1.2.1@sha256:abc,1234
    pantry-tree:
      size: 5678
      sha256: def
  original:
    hackage: aeson-2.1.2.1
- completed:
    hackage: base-4.17.2.0@sha256:xyz,999
  original:
    hackage: base-4.17.2.0
            """.trimIndent()
        val deps = parser.parse("stack.yaml.lock", content)
        assertEquals(2, deps.size)
        assertEquals("aeson", deps[0].name)
        assertEquals("2.1.2.1", deps[0].version)
        assertEquals("base", deps[1].name)
        assertEquals("4.17.2.0", deps[1].version)
        assertEquals("Hackage", deps[0].ecosystem)
    }

    @Test fun `parse cabal project freeze constraints`() {
        val content =
            """
constraints: any.aeson ==2.1.2.1,
             any.base ==4.17.2.0,
             any.text ==2.0.2
            """.trimIndent()
        val deps = parser.parse("cabal.project.freeze", content)
        assertEquals(3, deps.size)
        assertEquals("aeson", deps[0].name)
        assertEquals("2.1.2.1", deps[0].version)
        assertEquals("text", deps[2].name)
    }
}
