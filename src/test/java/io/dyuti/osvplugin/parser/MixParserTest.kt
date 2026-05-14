// Tests for Elixir mix.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MixParserTest {
    private val parser = MixParser()

    @Test fun `canHandle returns true for mix-dot-lock`() {
        assertTrue(parser.canHandle("/project/mix.lock"))
    }

    @Test fun `parse mix lock hex dependencies`() {
        val content = """
%{
  "decimal": {:hex, :decimal, "2.1.1", "...", [:mix], [], "hexpm", "..."},
  "ecto": {:hex, :ecto, "3.11.0", "...", [:mix], [{:decimal, "~> 2.0"}], "hexpm", "..."},
  "phoenix": {:git, "https://github.com/phoenixframework/phoenix.git", "abc123", []},
}
        """.trimIndent()
        val deps = parser.parse("mix.lock", content)
        assertEquals(2, deps.size) // git dep is skipped
        assertEquals("decimal", deps[0].name)
        assertEquals("2.1.1", deps[0].version)
        assertEquals("Hex", deps[0].ecosystem)
        assertEquals("ecto", deps[1].name)
        assertEquals("3.11.0", deps[1].version)
    }
}
