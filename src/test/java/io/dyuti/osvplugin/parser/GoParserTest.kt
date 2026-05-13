// Tests for Go modules parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoParserTest {
    private val parser = GoParser()

    @Test
    fun `canHandle returns true for go-dot-mod`() {
        assertTrue(parser.canHandle("/project/go.mod"))
    }

    @Test
    fun `parse go-mod with require block`() {
        val content =
            """
            module github.com/example/project

            go 1.21

            require (
                github.com/gin-gonic/gin v1.9.1
                github.com/stretchr/testify v1.8.4
                golang.org/x/crypto v0.17.0
            )
            """.trimIndent()

        val deps = parser.parse("go.mod", content)
        assertEquals(3, deps.size)

        val gin = deps[0]
        assertEquals("github.com/gin-gonic/gin", gin.name)
        assertEquals("v1.9.1", gin.version)
        assertEquals("Go", gin.ecosystem)
        assertEquals(false, gin.transitive)

        val crypto = deps[2]
        assertEquals("golang.org/x/crypto", crypto.name)
        assertEquals("v0.17.0", crypto.version)
    }

    @Test
    fun `parse go-mod with single require and indirect`() {
        val content =
            """
            module github.com/example/project

            require github.com/pkg/errors v0.9.1
            require github.com/sirupsen/logrus v1.9.3 // indirect
            """.trimIndent()

        val deps = parser.parse("go.mod", content)
        assertEquals(2, deps.size)

        val pkgErrors = deps[0]
        assertEquals("github.com/pkg/errors", pkgErrors.name)
        assertEquals("v0.9.1", pkgErrors.version)
        assertEquals(false, pkgErrors.transitive)

        val logrus = deps[1]
        assertEquals("github.com/sirupsen/logrus", logrus.name)
        assertEquals("v1.9.3", logrus.version)
        assertEquals(true, logrus.transitive)
    }

    @Test
    fun `parse go-mod with test qualifier`() {
        val content =
            """
            require github.com/onsi/gomega v1.30.0 // test
            """.trimIndent()

        val deps = parser.parse("go.mod", content)
        assertEquals(1, deps.size)
        assertEquals("test", deps[0].scope)
    }

    @Test
    fun `parse go-mod ignores module and replace directives`() {
        val content =
            """
            module github.com/example/project

            go 1.21

            require github.com/spf13/cobra v1.8.0

            replace github.com/spf13/cobra => github.com/spf13/cobra v1.8.0
            """.trimIndent()

        val deps = parser.parse("go.mod", content)
        assertEquals(1, deps.size)
        assertEquals("github.com/spf13/cobra", deps[0].name)
    }
}
