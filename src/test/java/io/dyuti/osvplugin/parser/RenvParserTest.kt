// Tests for R renv.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RenvParserTest {
    private val parser = RenvParser()

    @Test fun `canHandle returns true for renv-dot-lock`() {
        assertTrue(parser.canHandle("/project/renv.lock"))
    }

    @Test fun `parse renv lock with CRAN packages`() {
        val content =
            """
{"R":{"Version":"4.3.1"},"Packages":{"dplyr":{"Package":"dplyr","Version":"1.1.3","Source":"Repository","Repository":"CRAN"},"ggplot2":{"Package":"ggplot2","Version":"3.4.3","Source":"Repository","Repository":"CRAN"}}}
            """.trimIndent()
        val deps = parser.parse("renv.lock", content)
        assertEquals(2, deps.size)
        assertEquals("dplyr", deps[0].name)
        assertEquals("1.1.3", deps[0].version)
        assertEquals("CRAN", deps[0].ecosystem)
        assertEquals("ggplot2", deps[1].name)
    }

    @Test fun `parse renv lock with GitHub package uses RemoteSha`() {
        val content =
            """
{"R":{"Version":"4.3.1"},"Packages":{"devtools":{"Package":"devtools","Version":null,"Source":"GitHub","RemoteSha":"abc1234"}}}
            """.trimIndent()
        val deps = parser.parse("renv.lock", content)
        assertEquals(1, deps.size)
        assertEquals("devtools", deps[0].name)
        assertEquals("abc1234", deps[0].version)
        assertEquals("GitHub", deps[0].ecosystem)
    }
}
