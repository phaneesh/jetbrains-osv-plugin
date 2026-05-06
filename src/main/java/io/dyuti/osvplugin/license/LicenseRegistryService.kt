// SPDX License Scanner Integration - Registry Service
package io.dyuti.osvplugin.license

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.dyuti.osvplugin.api.model.Dependency
import io.dyuti.osvplugin.utils.CacheManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.NodeList
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Service for querying real package registries to determine dependency licenses.
 *
 * Supports:
 * - Maven Central (search.maven.org + repo1.maven.org)
 * - NPM Registry (registry.npmjs.org)
 * - PyPI (pypi.org)
 */
class LicenseRegistryService(
    httpClient: OkHttpClient? = null,
) {
    private val httpClient: OkHttpClient =
        httpClient
            ?: OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

    private val gson = Gson()
    private val cacheManager = CacheManager.getInstance()

    companion object {
        fun getInstance(): LicenseRegistryService = LicenseRegistryService()

        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 24 hours

        // Maven Central search URL
        private const val MAVEN_SEARCH_URL = "https://search.maven.org/solrsearch/select"

        // Maven Central repository URL
        private const val MAVEN_REPO_URL = "https://repo1.maven.org/maven2"

        // NPM registry URL
        private const val NPM_REGISTRY_URL = "https://registry.npmjs.org"

        // PyPI registry URL
        private const val PYPI_REGISTRY_URL = "https://pypi.org/pypi"
    }

    /**
     * Fetch the SPDX license identifier for a dependency by querying the appropriate registry.
     *
     * Returns "UNKNOWN" if no license can be determined.
     */
    fun fetchLicense(dependency: Dependency): String {
        val cacheKey = "license:${dependency.ecosystem}:${dependency.name}"
        cacheManager.getString(cacheKey)?.let { return it }

        val license =
            when (dependency.ecosystem.lowercase()) {
                "maven", "gradle" -> fetchMavenLicense(dependency)
                "npm" -> fetchNpmLicense(dependency)
                "pypi" -> fetchPypiLicense(dependency)
                else -> "UNKNOWN"
            }

        cacheManager.cacheString(cacheKey, license, CACHE_TTL_MS)
        return license
    }

    /**
     * Query Maven Central for a dependency's license.
     *
     * Dependency name is expected in "groupId:artifactId" format.
     */
    private fun fetchMavenLicense(dependency: Dependency): String {
        val (groupId, artifactId) = parseMavenCoordinates(dependency.name) ?: return "UNKNOWN"

        val searchUrl =
            buildString {
                append(MAVEN_SEARCH_URL)
                append("?q=g:\"$groupId\"+AND+a:\"$artifactId\"")
                append("&rows=1&wt=json")
            }

        val latestVersion =
            try {
                val response = executeGet(searchUrl) ?: return "UNKNOWN"
                val json = JsonParser.parseString(response).asJsonObject
                val docs =
                    json
                        .getAsJsonObject("response")
                        ?.getAsJsonArray("docs")
                        ?: return "UNKNOWN"

                if (docs.size() == 0) return "UNKNOWN"

                val doc = docs[0].asJsonObject
                doc.getAsJsonPrimitive("latestVersion")?.asString
                    ?: doc.getAsJsonPrimitive("v")?.asString
                    ?: return "UNKNOWN"
            } catch (e: Exception) {
                return "UNKNOWN"
            }

        val pomUrl = "$MAVEN_REPO_URL/${groupId.replace('.', '/')}/$artifactId/$latestVersion/$artifactId-$latestVersion.pom"

        return try {
            val pomContent = executeGet(pomUrl) ?: return "UNKNOWN"
            parseLicenseFromPom(pomContent)
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * Parse Maven coordinates from a dependency name.
     */
    internal fun parseMavenCoordinates(name: String): Pair<String, String>? {
        val parts = name.split(':')
        return when (parts.size) {
            2 -> parts[0] to parts[1]

            3 -> parts[0] to parts[1]

            // groupId:artifactId:version
            else -> null
        }
    }

    /**
     * Extract license name(s) from a pom.xml string.
     *
     * Returns the first <name> found inside <licenses><license>.
     */
    private fun parseLicenseFromPom(pomContent: String): String {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.isValidating = false
            // Prevent XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(org.xml.sax.InputSource(StringReader(pomContent)))
            val licensesList = doc.getElementsByTagName("licenses")
            if (licensesList.length == 0) return "UNKNOWN"

            val licensesNode = licensesList.item(0)
            val licenseNodes = licensesNode.childNodes
            for (i in 0 until licenseNodes.length) {
                val licenseNode = licenseNodes.item(i)
                if (licenseNode.nodeName == "license") {
                    val nameNodes = licenseNode.childNodes
                    for (j in 0 until nameNodes.length) {
                        val child = nameNodes.item(j)
                        if (child.nodeName == "name") {
                            return child.textContent?.trim()?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
                        }
                    }
                }
            }
            "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * Query NPM registry for a dependency's license.
     */
    private fun fetchNpmLicense(dependency: Dependency): String {
        val packageName = dependency.name.trim()
        if (packageName.isBlank()) return "UNKNOWN"

        val registryUrl = "$NPM_REGISTRY_URL/$packageName"

        return try {
            val response = executeGet(registryUrl) ?: return "UNKNOWN"
            val json = JsonParser.parseString(response).asJsonObject

            // Try "dist-tags"."latest" first
            val distTags = json.getAsJsonObject("dist-tags")
            val latestVersion = distTags?.getAsJsonPrimitive("latest")?.asString

            val versionKey = latestVersion ?: dependency.version.takeIf { it.isNotBlank() }
            if (versionKey == null) return "UNKNOWN"

            val versions = json.getAsJsonObject("versions") ?: return "UNKNOWN"
            val versionObj = versions.getAsJsonObject(versionKey) ?: return "UNKNOWN"

            // "license" may be a string or an object with "type"
            val licenseElement = versionObj.get("license")
            when {
                licenseElement == null || licenseElement.isJsonNull -> {
                    "UNKNOWN"
                }

                licenseElement.isJsonPrimitive -> {
                    licenseElement.asString.trim()
                }

                licenseElement.isJsonObject -> {
                    licenseElement.asJsonObject
                        .getAsJsonPrimitive("type")
                        ?.asString
                        ?.trim() ?: "UNKNOWN"
                }

                else -> {
                    "UNKNOWN"
                }
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * Query PyPI for a dependency's license.
     */
    private fun fetchPypiLicense(dependency: Dependency): String {
        val packageName = dependency.name.trim().lowercase()
        if (packageName.isBlank()) return "UNKNOWN"

        val registryUrl = "$PYPI_REGISTRY_URL/$packageName/json"

        return try {
            val response = executeGet(registryUrl) ?: return "UNKNOWN"
            val json = JsonParser.parseString(response).asJsonObject
            val info = json.getAsJsonObject("info") ?: return "UNKNOWN"

            // 1. Try explicit license field
            info.getAsJsonPrimitive("license")?.asString?.trim()?.takeIf { it.isNotBlank() }?.let {
                return it
            }

            // 2. Try classifiers for OSI Approved licenses
            val classifiers = info.getAsJsonArray("classifiers") ?: return "UNKNOWN"
            for (element in classifiers) {
                val classifier = element.asString
                val osiPrefix = "License :: OSI Approved :: "
                if (classifier.startsWith(osiPrefix)) {
                    return classifier.substringAfter(osiPrefix).trim()
                }
            }

            "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * Execute a GET request and return the response body as a string.
     * Returns null on failure.
     */
    private fun executeGet(url: String): String? {
        return try {
            val request =
                Request
                    .Builder()
                    .url(url)
                    .get()
                    .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: IOException) {
            null
        }
    }
}
