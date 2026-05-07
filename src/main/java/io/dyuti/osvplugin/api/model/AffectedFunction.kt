// Vulnerable method / function signature from OSV database
package io.dyuti.osvplugin.api.model

/**
 * Represents a vulnerable function signature from the OSV database.
 *
 * OSV stores function-level affected data in the `affected[].database_specific.functions`
 * field (a list of strings) for some vulnerabilities. When a vulnerability has this data,
 * we can perform "reachability analysis" — checking if the project's source code actually
 * calls the vulnerable method.
 *
 * @param signature The full method signature, e.g. "org.apache.logging.log4j.Logger.debug"
 * @param className Optional class name extracted from the signature
 * @param methodName Optional method name extracted from the signature
 */
data class AffectedFunction(
    val signature: String,
    val className: String? = null,
    val methodName: String? = null,
)

/**
 * Vulnerable method call site found in the project's source code.
 *
 * @param methodName The method name being called (e.g. "debug")
 * @param className The class on which the method is called (e.g. "org.apache.logging.log4j.Logger")
 * @param filePath Source file path
 * @param lineNumber Line number in the source file
 * @param qualifierExpression Optional qualifier (e.g. "logger" in "logger.debug")
 */
data class VulnerableCallSite(
    val methodName: String,
    val className: String,
    val filePath: String,
    val lineNumber: Int,
    val qualifierExpression: String? = null,
)

/**
 * Result of reachability analysis for a single vulnerability within a dependency.
 *
 * @param vulnerability The vulnerability being analyzed
 * @param dependency The dependency that contains the vulnerability
 * @param callSites List of actual call sites in the project's source code (empty if unreachable)
 * @param reachable true if at least one call site was found
 */
data class ReachabilityResult(
    val vulnerability: Vulnerability,
    val dependency: Dependency,
    val callSites: List<VulnerableCallSite>,
    val reachable: Boolean = callSites.isNotEmpty(),
)
