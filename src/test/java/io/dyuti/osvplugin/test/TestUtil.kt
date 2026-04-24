package io.dyuti.osvplugin.test

/**
 * Test utility functions for the OSV plugin
 */
object TestUtil {
    
    /**
     * Get test fixture file content
     */
    fun getFixtureContent(filename: String): String {
        val resource = TestUtil::class.java.classLoader.getResource(filename)
        return resource?.readText() ?: throw IllegalArgumentException("Fixture not found: $filename")
    }
    
    /**
     * Get test fixture file path
     */
    fun getFixturePath(filename: String): String {
        val resource = TestUtil::class.java.classLoader.getResource(filename)
        return resource?.path ?: throw IllegalArgumentException("Fixture not found: $filename")
    }
}
