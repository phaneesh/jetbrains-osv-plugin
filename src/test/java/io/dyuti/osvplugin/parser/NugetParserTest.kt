// Tests for .NET packages.lock.json parser
package io.dyuti.osvplugin.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NugetParserTest {
    private val parser = NugetParser()

    @Test
    fun `canHandle returns true for packages-dot-lock-dot-json`() {
        assertTrue(parser.canHandle("/project/packages.lock.json"))
        assertTrue(parser.canHandle("/project/packages.config"))
    }

    @Test
    fun `parse packages lock json with direct and transitive`() {
        val content =
            """
            {
              "version": 2,
              "dependencies": {
                ".NETCoreApp,Version=v8.0": {
                  "Newtonsoft.Json": {
                    "type": "Direct",
                    "requested": "[13.0.3, )",
                    "resolved": "13.0.3"
                  },
                  "System.Text.Json": {
                    "type": "Transitive",
                    "resolved": "8.0.0"
                  }
                }
              }
            }
            """.trimIndent()

        val deps = parser.parse("packages.lock.json", content)
        assertEquals(2, deps.size)

        val newtonsoft = deps[0]
        assertEquals("Newtonsoft.Json", newtonsoft.name)
        assertEquals("13.0.3", newtonsoft.version)
        assertEquals("NuGet", newtonsoft.ecosystem)
        assertEquals(false, newtonsoft.transitive)

        val systemText = deps[1]
        assertEquals("System.Text.Json", systemText.name)
        assertEquals(true, systemText.transitive)
    }

    @Test
    fun `parse packages config xml`() {
        val content =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <packages>
              <package id="Newtonsoft.Json" version="13.0.3" targetFramework="net48" />
              <package id="Serilog" version="3.1.1" targetFramework="net48" />
            </packages>
            """.trimIndent()

        val deps = parser.parse("packages.config", content)
        assertEquals(2, deps.size)
        assertEquals("Newtonsoft.Json", deps[0].name)
        assertEquals("13.0.3", deps[0].version)
        assertEquals("Serilog", deps[1].name)
    }
}
