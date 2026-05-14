// SPDX License Scanner — LicensePolicyConfigurable unit tests
package io.dyuti.osvplugin.license

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LicensePolicyConfigurable].
 *
 * These run headless without a real Swing UI by directly exercising the
 * component via reflection on the check-box grid.  Primarily verifies
 * that `isModified` / `apply` / `reset` correctly sync with
 * [LicensePolicyConfig].
 */
class LicensePolicyConfigurableTest : LightJavaCodeInsightFixtureTestCase() {
    private lateinit var configurable: LicensePolicyConfigurable
    private lateinit var policy: LicensePolicyConfig

    @BeforeEach
    override fun setUp() {
        super.setUp()
        policy = ApplicationManager.getApplication().getService(LicensePolicyConfig::class.java)
        policy.allowedLicenses = emptyList()
        policy.blockedLicenses = emptyList()
        policy.copyleftAllowList = emptyList()
        policy.strictMode = true

        configurable = LicensePolicyConfigurable()
        configurable.createComponent() // initialise checkbox map
        configurable.reset()
    }

    @AfterEach
    override fun tearDown() {
        configurable.disposeUIResources()
        super.tearDown()
    }

    @Test
    fun `initial reset reflects empty policy`() {
        assert(!configurable.isModified) { "Fresh reset should not be modified" }
    }

    @Test
    fun `isModified after checking a license`() {
        setLicenseChecked("MIT", true)
        assert(configurable.isModified) { "Checking MIT should mark config as modified" }
    }

    @Test
    fun `apply persists checked licenses`() {
        setLicenseChecked("MIT", true)
        setLicenseChecked("Apache-2.0", true)
        configurable.apply()

        assert(policy.allowedLicenses.contains("MIT"))
        assert(policy.allowedLicenses.contains("Apache-2.0"))
        assert(!policy.allowedLicenses.contains("GPL-3.0-only"))
    }

    @Test
    fun `apply with strict mode off`() {
        setStrictMode(false)
        configurable.apply()
        assert(!policy.strictMode)
    }

    @Test
    fun `reset restores persisted state after apply`() {
        setLicenseChecked("MIT", true)
        configurable.apply()

        // simulate user un-checking
        setLicenseChecked("MIT", false)
        assert(configurable.isModified)

        configurable.reset()
        assert(!configurable.isModified)
        assert(isLicenseChecked("MIT")) { "Reset should restore MIT" }
    }

    @Test
    fun `no modification when checking then unchecking same license`() {
        setLicenseChecked("BSD-3-Clause", true)
        setLicenseChecked("BSD-3-Clause", false)
        assert(!configurable.isModified) { "Check then uncheck should net zero diff" }
    }

    @Test
    fun `catalog coverage includes all categories`() {
        LicenseCatalog.Category.values().forEach { cat ->
            val list = LicenseCatalog.byCategory[cat]
            assert(list != null) { "Missing category $cat" }
            assert(list!!.isNotEmpty()) { "Category $cat has no licenses" }
        }
    }

    // ── Reflection helpers into the private checkbox map ──

    private fun setLicenseChecked(
        spdxId: String,
        checked: Boolean,
    ) {
        val checkBoxes = getCheckboxMap()
        val cb =
            checkBoxes[spdxId]
                ?: throw AssertionError("Checkbox for $spdxId not found in catalog")
        cb.isSelected = checked
    }

    private fun isLicenseChecked(spdxId: String): Boolean {
        val checkBoxes = getCheckboxMap()
        return checkBoxes[spdxId]?.isSelected ?: false
    }

    private fun setStrictMode(enabled: Boolean) {
        val strictCheck =
            LicensePolicyConfigurable::class.java
                .getDeclaredField("strictCheck")
                .apply { isAccessible = true }
                .get(configurable) as javax.swing.JCheckBox
        strictCheck.isSelected = enabled
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCheckboxMap(): Map<String, javax.swing.JCheckBox> =
        LicensePolicyConfigurable::class.java
            .getDeclaredField("checkBoxes")
            .apply { isAccessible = true }
            .get(configurable) as Map<String, javax.swing.JCheckBox>
}
