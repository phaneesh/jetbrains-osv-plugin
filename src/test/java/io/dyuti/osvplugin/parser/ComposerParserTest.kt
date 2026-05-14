// Tests for PHP composer.lock parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposerParserTest {
    private val parser = ComposerParser()

    @Test
    fun `canHandle returns true for composer-dot-lock`() {
        assertTrue(parser.canHandle("/project/composer.lock"))
    }

    @Test
    fun `parse composer lock with packages and packages-dev`() {
        val content =
            """
            {
              "packages": [
                {"name": "symfony/console", "version": "6.3.4", "type": "library"},
                {"name": "monolog/monolog", "version": "3.5.0", "type": "library"}
              ],
              "packages-dev": [
                {"name": "phpunit/phpunit", "version": "10.4.0", "type": "library"}
              ]
            }
            """.trimIndent()

        val deps = parser.parse("composer.lock", content)
        assertEquals(3, deps.size)

        val symfony = deps[0]
        assertEquals("symfony/console", symfony.name)
        assertEquals("6.3.4", symfony.version)
        assertEquals("Packagist", symfony.ecosystem)
        assertEquals("runtime", symfony.scope)

        val phpunit = deps[2]
        assertEquals("phpunit/phpunit", phpunit.name)
        assertEquals("test", phpunit.scope)
    }

    @Test
    fun `parse empty composer lock returns empty list`() {
        val deps = parser.parse("composer.lock", "{}")
        assertTrue(deps.isEmpty())
    }
}
