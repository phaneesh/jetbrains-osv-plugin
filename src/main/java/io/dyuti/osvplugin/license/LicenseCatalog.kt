// SPDX License Scanner Integration - Enumerated License Catalog
package io.dyuti.osvplugin.license

/**
 * Exhaustive categorized catalog of open-source and proprietary software licenses.
 *
 * Each entry is a real SPDX identifier (where available) paired with a human-readable
 * display name.  Categories match how legal / compliance teams typically classify
 * licenses for approval workflows.
 *
 * Source references:
 *   • SPDX License List      – https://spdx.org/licenses/
 *   • Choose a License       – https://choosealicense.com/appendix/
 *   • OSI Approved Licenses  – https://opensource.org/licenses
 */
object LicenseCatalog {
    /**
     * One license entry with its SPDX id, friendly name, and risk description.
     */
    data class LicenseEntry(
        val spdxId: String,
        val displayName: String,
        val riskNote: String,
    )

    /**
     * License category used for grouping in the UI.
     */
    enum class Category(
        val displayName: String,
        val description: String,
        val defaultChecked: Boolean,
    ) {
        PERMISSIVE(
            "Permissive / Business-Friendly",
            "Minimal restrictions — can use, modify, and distribute in proprietary products. Usually only requires attribution.",
            true,
        ),
        WEAK_COPYLEFT(
            "Weak Copyleft",
            "Requires sharing modifications to the *same file* or *linked library*, but does not infect the entire application.  Common for libraries.",
            false,
        ),
        STRONG_COPYLEFT(
            "Strong Copyleft",
            "Requires sharing *all* source code of derived works under the same license (reciprocity).  Use with legal review.",
            false,
        ),
        PUBLIC_DOMAIN(
            "Public Domain / Creative Commons",
            "No copyright restrictions (or attribution-only).  Includes CC0 and permissive CC licenses.",
            true,
        ),
        INTERNATIONAL(
            "International / Regional",
            "Jurisdiction-specific licenses (e.g. EU, France, China).  Verify local legal requirements.",
            false,
        ),
        PROPRIETARY(
            "Proprietary / Restricted",
            "All-rights-reserved, commercial-only, or source-available licenses that are *not* open source.",
            false,
        ),
        OTHER(
            "Other / Specialized",
            "Special-purpose or niche licenses not fitting above categories (e.g. font, hardware, or academic licenses).",
            false,
        ),
    }

    /** ── Permissive / Business-Friendly ─────────────────────────────── */
    val PERMISSIVE =
        listOf(
            LicenseEntry("MIT", "MIT License", "Very permissive; requires attribution only."),
            LicenseEntry("MIT-0", "MIT No Attribution (MIT-0)", "MIT without attribution requirement."),
            LicenseEntry("Apache-2.0", "Apache License 2.0", "Permissive; explicit patent grant. Industry standard."),
            LicenseEntry("Apache-1.1", "Apache License 1.1", "Older Apache; still permissive."),
            LicenseEntry("Apache-1.0", "Apache License 1.0", "Original Apache license; rare today."),
            LicenseEntry("BSD-2-Clause", "BSD 2-Clause (Simplified)", "Minimal; prohibits use of project name for endorsement."),
            LicenseEntry("BSD-3-Clause", "BSD 3-Clause (New/Revised)", "Adds non-endorsement clause over 2-Clause."),
            LicenseEntry("BSD-4-Clause", "BSD 4-Clause (Original)", "Contains advertising clause — rarely used today."),
            LicenseEntry("ISC", "ISC License", "Functionally equivalent to MIT/BSD-2."),
            LicenseEntry("Zlib", "zlib License", "Permissive; used for compression libraries."),
            LicenseEntry("Unlicense", "The Unlicense", "Public domain dedication; zero restrictions."),
            LicenseEntry("WTFPL", "WTFPL", "Do What The F*ck You Want To Public License."),
            LicenseEntry("BSL-1.0", "Boost Software License 1.0", "Permissive; popular in C++ ecosystem."),
            LicenseEntry("UPL-1.0", "Universal Permissive License 1.0", "Oracle-originated; patent + copyright permissive."),
            LicenseEntry("Beerware", "Beerware", "Buy the author a beer if you meet them."),
        )

    /** ── Weak Copyleft ──────────────────────────────────────────────── */
    val WEAK_COPYLEFT =
        listOf(
            LicenseEntry("MPL-2.0", "Mozilla Public License 2.0", "File-level copyleft; MPL files must stay open."),
            LicenseEntry("MPL-1.1", "Mozilla Public License 1.1", "Older MPL; MPL-2.0 preferred today."),
            LicenseEntry("CDDL-1.0", "Common Development & Distribution 1.0", "File-level copyleft; used by OpenSolaris."),
            LicenseEntry("CDDL-1.1", "Common Development & Distribution 1.1", "Updated CDDL."),
            LicenseEntry("EPL-1.0", "Eclipse Public License 1.0", "Weak copyleft; used by Eclipse Foundation."),
            LicenseEntry("EPL-2.0", "Eclipse Public License 2.0", "Updated EPL with GPL compatibility option."),
            LicenseEntry("LGPL-2.1", "GNU LGPL 2.1 only", "Library-level copyleft; linking allowed."),
            LicenseEntry("LGPL-2.1+", "GNU LGPL 2.1 or later", "LGPL 2.1 with upgrade path."),
            LicenseEntry("LGPL-3.0", "GNU LGPL 3.0 only", "Updated LGPL; addresses tivoization."),
            LicenseEntry("LGPL-3.0+", "GNU LGPL 3.0 or later", "LGPL 3.0 with upgrade path."),
            LicenseEntry("CPL-1.0", "Common Public License 1.0", "Weak copyleft; predecessor of EPL."),
        )

    /** ── Strong Copyleft ────────────────────────────────────────────── */
    val STRONG_COPYLEFT =
        listOf(
            LicenseEntry("GPL-2.0-only", "GNU GPL 2.0 only", "Strong copyleft; no upgrade clause."),
            LicenseEntry("GPL-2.0-or-later", "GNU GPL 2.0 or later", "Strong copyleft; can upgrade to GPL-3.0+."),
            LicenseEntry("GPL-3.0-only", "GNU GPL 3.0 only", "Current FSF recommended strong copyleft."),
            LicenseEntry("GPL-3.0-or-later", "GNU GPL 3.0 or later", "GPL-3.0 with upgrade path (e.g. to AGPL)."),
            LicenseEntry("AGPL-3.0-only", "GNU AGPL 3.0 only", "Network copyleft; SaaS usage triggers sharing."),
            LicenseEntry("AGPL-3.0-or-later", "GNU AGPL 3.0 or later", "AGPL with upgrade path."),
            LicenseEntry("SSPL-1.0", "Server Side Public License 1.0", "Source-available; MongoDB-created. NOT OSI-approved."),
        )

    /** ── Public Domain / Creative Commons ───────────────────────────── */
    val PUBLIC_DOMAIN =
        listOf(
            LicenseEntry("CC0-1.0", "CC0 1.0 Universal", "Public domain dedication; zero restrictions."),
            LicenseEntry("CC-BY-4.0", "Creative Commons BY 4.0", "Attribution only; not software-specific."),
            LicenseEntry("CC-BY-SA-4.0", "Creative Commons BY-SA 4.0", "Attribution + ShareAlike (copyleft)."),
            LicenseEntry("CC-BY-NC-4.0", "Creative Commons BY-NC 4.0", "Non-commercial restriction."),
            LicenseEntry("CC-BY-ND-4.0", "Creative Commons BY-ND 4.0", "No derivatives allowed."),
            LicenseEntry("PDDL-1.0", "Open Data Commons PDDL 1.0", "Public domain for databases / data."),
        )

    /** ── International / Regional ───────────────────────────────────── */
    val INTERNATIONAL =
        listOf(
            LicenseEntry("EUPL-1.2", "European Union Public License 1.2", "EU-approved copyleft; available in 22 languages."),
            LicenseEntry("EUPL-1.1", "European Union Public License 1.1", "Older EUPL version."),
            LicenseEntry("CeCILL-2.1", "CeCILL 2.1", "France; designed to be GPL-compatible."),
            LicenseEntry("MulanPSL-2.0", "Mulan Permissive Software License 2.0", "China-originated; bilingual (CN/EN)."),
            LicenseEntry("Motosoto", "Motosoto License", "Spain; evaluated by OSI."),
        )

    /** ── Proprietary / Restricted ───────────────────────────────────── */
    val PROPRIETARY =
        listOf(
            LicenseEntry("Proprietary", "Proprietary / All Rights Reserved", "Closed-source; no redistribution permitted."),
            LicenseEntry("Commercial", "Commercial License", "Requires purchase or subscription."),
            LicenseEntry("No-License", "No License (Unlicensed)", "Copyright applies by default; no permissions granted."),
            LicenseEntry("MS-PL", "Microsoft Public License", "Microsoft open-source; patent grant included."),
            LicenseEntry("MS-RL", "Microsoft Reciprocal License", "Microsoft copyleft; file-level sharing required."),
        )

    /** ── Other / Specialized ────────────────────────────────────────── */
    val OTHER =
        listOf(
            LicenseEntry("Artistic-2.0", "Artistic License 2.0", "Perl Foundation; allows relicensing."),
            LicenseEntry("PSF-2.0", "Python Software Foundation 2.0", "Python-specific; used by CPython."),
            LicenseEntry("NCSA", "University of Illinois / NCSA", "Academic; used by LLVM."),
            LicenseEntry("OSL-3.0", "Open Software License 3.0", "Strong copyleft by Lawrence Rosen."),
            LicenseEntry("APL-1.0", "Adaptive Public License 1.0", "Strong copyleft with modular clauses."),
            LicenseEntry("IPL-1.0", "IBM Public License 1.0", "Weak copyleft; IBM-specific variant."),
            LicenseEntry("PostgreSQL", "PostgreSQL License", "MIT-like; used by PostgreSQL project."),
            LicenseEntry("AFL-3.0", "Academic Free License 3.0", "OSI-approved; legally reviewed."),
            LicenseEntry("0BSD", "Zero-Clause BSD", "BSD with ALL clauses removed."),
            LicenseEntry("HPND", "Historical Permission Notice", "Academic; used by X Consortium code."),
            LicenseEntry("NASA-1.3", "NASA Open Source Agreement 1.3", "US Government; specific to NASA projects."),
        )

    /** Convenience map from category → licenses */
    val byCategory: Map<Category, List<LicenseEntry>> =
        mapOf(
            Category.PERMISSIVE to PERMISSIVE,
            Category.WEAK_COPYLEFT to WEAK_COPYLEFT,
            Category.STRONG_COPYLEFT to STRONG_COPYLEFT,
            Category.PUBLIC_DOMAIN to PUBLIC_DOMAIN,
            Category.INTERNATIONAL to INTERNATIONAL,
            Category.PROPRIETARY to PROPRIETARY,
            Category.OTHER to OTHER,
        )

    /** Flat list of every license in the catalog. */
    val allLicenses: List<LicenseEntry> by lazy {
        Category.values().flatMap { byCategory[it] ?: emptyList() }
    }

    /** Fast lookup by SPDX identifier (case-insensitive). */
    fun findBySpdx(id: String): LicenseEntry? = allLicenses.firstOrNull { it.spdxId.equals(id, ignoreCase = true) }

    /** Categorize an external license string (may be SPDX id or free text). */
    fun categorize(license: String): Category? {
        val entry = findBySpdx(license)
        if (entry != null) {
            return byCategory.entries
                .firstOrNull { (_, list) ->
                    list.any { it.spdxId == entry.spdxId }
                }?.key
        }
        // Fallback heuristic for free-text licenses
        return when {
            isProprietaryLike(license) -> Category.PROPRIETARY
            isStrongCopyleftLike(license) -> Category.STRONG_COPYLEFT
            isWeakCopyleftLike(license) -> Category.WEAK_COPYLEFT
            isPublicDomainLike(license) -> Category.PUBLIC_DOMAIN
            isPermissiveLike(license) -> Category.PERMISSIVE
            else -> null
        }
    }

    private fun isProprietaryLike(l: String) =
        l.contains("proprietary", ignoreCase = true) ||
            l.contains("commercial", ignoreCase = true) ||
            l.contains("all rights reserved", ignoreCase = true)

    private fun isStrongCopyleftLike(l: String) =
        l.contains(Regex("""\b(GPL|AGPL|SSPL)\b""", RegexOption.IGNORE_CASE)) ||
            l.contains("general public license", ignoreCase = true)

    private fun isWeakCopyleftLike(l: String) =
        l.contains(Regex("""\b(LGPL|MPL|CDDL|EPL|CPL)\b""", RegexOption.IGNORE_CASE)) ||
            l.contains("mozilla public", ignoreCase = true) ||
            l.contains("eclipse public", ignoreCase = true) ||
            l.contains("common development", ignoreCase = true) ||
            l.contains("lesser general public", ignoreCase = true)

    private fun isPublicDomainLike(l: String) =
        l.contains("CC0", ignoreCase = true) ||
            l.contains("public domain", ignoreCase = true) ||
            l.contains("Unlicense", ignoreCase = true) ||
            l.contains("creative commons zero", ignoreCase = true) ||
            l.contains("cc-zero", ignoreCase = true)

    private fun isPermissiveLike(l: String) =
        l.contains(Regex("""\bMIT\b""", RegexOption.IGNORE_CASE)) ||
            l.contains("Apache", ignoreCase = true) ||
            l.contains(Regex("""\bBSD\b""", RegexOption.IGNORE_CASE)) ||
            l.contains(Regex("""\bISC\b""", RegexOption.IGNORE_CASE)) ||
            l.contains("zlib", ignoreCase = true) ||
            l.contains("Boost", ignoreCase = true) ||
            l.contains("WTFPL", ignoreCase = true)
}
