// Java PSI compatibility guard for cross-IDE support
package io.dyuti.osvplugin.utils

/**
 * Detects whether the Java language PSI APIs are available at runtime.
 *
 * PyCharm, GoLand, WebStorm, and other non-Java IDEs do not bundle
 * `com.intellij.modules.java`, so classes like `PsiMethodCallExpression`
 * are missing.  This guard prevents `NoClassDefFoundError` when those
 * classes are referenced.
 */
object JavaPsiCompatibility {
    /** `true` iff the Java PSI module is present in the current IDE. */
    @JvmStatic
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("com.intellij.psi.PsiMethodCallExpression", false, this::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** Convenience: runs [action] only if Java PSI is available; otherwise returns [default]. */
    @JvmStatic
    inline fun <T> ifAvailable(
        default: T,
        action: () -> T,
    ): T = if (isAvailable) action() else default
}
