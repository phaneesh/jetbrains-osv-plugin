// Tests for Ruby Gemfile.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GemfileParserTest {
    private val parser = GemfileParser()

    @Test
    fun `canHandle returns true for Gemfile-dot-lock`() {
        assertTrue(parser.canHandle("/project/Gemfile.lock"))
        assertTrue(parser.canHandle("/project/gems.locked"))
    }

    @Test
    fun `parse Gemfile lock with specs`() {
        val content =
            """
GEM
  remote: https://rubygems.org/
  specs:
    activesupport (7.1.0)
      concurrent-ruby (~> 1.0)
      minitest (>= 5.1)
    bundler (2.4.0)
    concurrent-ruby (1.2.2)

PLATFORMS
  ruby

DEPENDENCIES
  activesupport
            """.trimIndent()

        val deps = parser.parse("Gemfile.lock", content)
        // 3 top-level packages (activesupport, bundler, concurrent-ruby);
        // nested constraints under activesupport are skipped (no resolved version)
        assertEquals(3, deps.size)

        val rails = deps[0]
        assertEquals("activesupport", rails.name)
        assertEquals("7.1.0", rails.version)
        assertEquals("RubyGems", rails.ecosystem)

        assertEquals("concurrent-ruby", deps[2].name)
        assertEquals("1.2.2", deps[2].version)
    }

    @Test
    fun `parse Gemfile lock with no specs returns empty`() {
        val deps = parser.parse("Gemfile.lock", "GEM\n  remote: https://rubygems.org/")
        assertTrue(deps.isEmpty())
    }
}
