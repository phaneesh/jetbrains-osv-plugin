// Tests for JavaScript yarn.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YarnParserTest {
    private val parser = YarnParser()

    @Test fun `canHandle returns true for yarn-dot-lock`() {
        assertTrue(parser.canHandle("/project/yarn.lock"))
    }

    @Test fun `parse yarn lock v1`() {
        val content =
            """
# yarn lockfile v1

package-a@^1.0.0:
  version "1.2.3"
  resolved "https://registry.yarnpkg.com/package-a/-/package-a-1.2.3.tgz"

"@scope/package@^2.0.0":
  version "2.1.0"
  resolved "https://registry.yarnpkg.com/@scope/package/-/package-2.1.0.tgz"

lodash@^4.17.0:
  version "4.17.21"
  resolved "https://registry.yarnpkg.com/lodash/-/lodash-4.17.21.tgz"
            """.trimIndent()
        val deps = parser.parse("yarn.lock", content)
        assertEquals(3, deps.size)
        assertEquals("package-a", deps[0].name)
        assertEquals("1.2.3", deps[0].version)
        assertEquals("npm", deps[0].ecosystem)
        assertEquals("@scope/package", deps[1].name)
        assertEquals("2.1.0", deps[1].version)
        assertEquals("lodash", deps[2].name)
    }
}
