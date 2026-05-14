// SPDX License Scanner — LicenseCatalog unit tests
package io.dyuti.osvplugin.license

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LicenseCatalogTest {
    @Test
    fun `catalog contains every major SPDX license`() {
        Assertions.assertTrue(LicenseCatalog.allLicenses.isNotEmpty(), "Catalog must not be empty")
        Assertions.assertTrue(LicenseCatalog.allLicenses.size >= 60, "Expected at least 60 licenses")

        // Permissive
        assertContains("MIT")
        assertContains("Apache-2.0")
        assertContains("BSD-2-Clause")
        assertContains("BSD-3-Clause")
        assertContains("ISC")

        // Weak copyleft
        assertContains("MPL-2.0")
        assertContains("LGPL-2.1")
        assertContains("EPL-2.0")

        // Strong copyleft
        assertContains("GPL-3.0-only")
        assertContains("GPL-2.0-or-later")
        assertContains("AGPL-3.0-only")

        // Public domain
        assertContains("CC0-1.0")
        assertContains("Unlicense")

        // International
        assertContains("EUPL-1.2")

        // Proprietary
        assertContains("Proprietary")

        // Other
        assertContains("Artistic-2.0")
    }

    @Test
    fun `findBySpdx is case insensitive`() {
        val a = LicenseCatalog.findBySpdx("MIT")
        val b = LicenseCatalog.findBySpdx("mit")
        val c = LicenseCatalog.findBySpdx("Mit")
        Assertions.assertNotNull(a)
        Assertions.assertEquals(a, b, "Case-insensitive lookup failed for lower case")
        Assertions.assertEquals(a, c, "Case-insensitive lookup failed for mixed case")
    }

    @Test
    fun `findBySpdx returns null for unknown id`() {
        Assertions.assertNull(LicenseCatalog.findBySpdx("XYZ-UNKNOWN-NOT-REAL"))
    }

    @Test
    fun `every license has non-empty fields`() {
        LicenseCatalog.allLicenses.forEach { entry ->
            Assertions.assertTrue(entry.spdxId.isNotBlank(), "spdxId must not be blank")
            Assertions.assertTrue(entry.displayName.isNotBlank(), "displayName must not be blank for ${entry.spdxId}")
            Assertions.assertTrue(entry.riskNote.isNotBlank(), "riskNote must not be blank for ${entry.spdxId}")
        }
    }

    @Test
    fun `all spdx ids are unique`() {
        val ids = LicenseCatalog.allLicenses.map { it.spdxId }
        Assertions.assertEquals(ids.toSet().size, ids.size, "Duplicate SPDX ids found")
    }

    @Test
    fun `categorize returns correct category for known SPDX ids`() {
        Assertions.assertEquals(LicenseCatalog.Category.PERMISSIVE, LicenseCatalog.categorize("MIT"))
        Assertions.assertEquals(LicenseCatalog.Category.PERMISSIVE, LicenseCatalog.categorize("Apache-2.0"))
        Assertions.assertEquals(LicenseCatalog.Category.STRONG_COPYLEFT, LicenseCatalog.categorize("GPL-3.0-only"))
        Assertions.assertEquals(LicenseCatalog.Category.WEAK_COPYLEFT, LicenseCatalog.categorize("MPL-2.0"))
        Assertions.assertEquals(LicenseCatalog.Category.PUBLIC_DOMAIN, LicenseCatalog.categorize("CC0-1.0"))
        Assertions.assertEquals(LicenseCatalog.Category.PROPRIETARY, LicenseCatalog.categorize("Proprietary"))
    }

    @Test
    fun `categorize falls back to heuristic for free text`() {
        Assertions.assertEquals(LicenseCatalog.Category.PERMISSIVE, LicenseCatalog.categorize("MIT License"))
        Assertions.assertEquals(LicenseCatalog.Category.STRONG_COPYLEFT, LicenseCatalog.categorize("GNU General Public License v3"))
        Assertions.assertEquals(LicenseCatalog.Category.PROPRIETARY, LicenseCatalog.categorize("All Rights Reserved"))
        Assertions.assertEquals(LicenseCatalog.Category.PUBLIC_DOMAIN, LicenseCatalog.categorize("Creative Commons Zero"))
        Assertions.assertEquals(LicenseCatalog.Category.WEAK_COPYLEFT, LicenseCatalog.categorize("Mozilla Public License"))
    }

    @Test
    fun `categorize returns null for truly unknown strings`() {
        Assertions.assertNull(LicenseCatalog.categorize(""), "Empty string should categorize as null")
        Assertions.assertNull(LicenseCatalog.categorize("something completely random"), "Random string should categorize as null")
        Assertions.assertNull(LicenseCatalog.categorize("__12783921abc"), "Gibberish should categorize as null")
    }

    @Test
    fun `byCategory covers all categories`() {
        LicenseCatalog.Category.values().forEach { cat ->
            val list = LicenseCatalog.byCategory[cat]
            Assertions.assertNotNull(list, "Category $cat missing from byCategory map")
            Assertions.assertTrue(list!!.isNotEmpty(), "Category $cat must have at least one license")
        }
    }

    private fun assertContains(spdxId: String) {
        Assertions.assertNotNull(LicenseCatalog.findBySpdx(spdxId), "Catalog must contain $spdxId")
    }
}
