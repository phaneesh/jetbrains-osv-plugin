// Lightweight SQL Injection Detector
package io.dyuti.osvplugin.sast

import com.intellij.psi.*

/**
 * Detects SQL injection vulnerabilities via pattern matching.
 *
 * ## Detection Logic
 *
 * 1. Find `java.sql.Statement.executeQuery()` or `executeUpdate()` calls
 * 2. Check if the argument is a string concatenation or contains user input
 * 3. Flag if the argument is not a compile-time constant literal
 *
 * ## What it catches
 *
 * ```java
 * // BAD — SQL injection risk
 * stmt.executeQuery("SELECT * FROM users WHERE id = " + request.getParameter("id"));
 * stmt.executeUpdate("DELETE FROM accounts WHERE name = '" + name + "'");
 *
 * // GOOD — Parameterized query
 * PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
 * ps.setInt(1, id);
 * ```
 *
 * ## Limitations
 *
 * - Does not track data flow across method boundaries
 * - May have false positives for properly-sanitized input
 * - String builder patterns may not be detected
 */
class SqlInjectionDetector {
    /**
     * Known SQL execution method patterns (class.simpleName, methodName).
     */
    private val sqlMethodPatterns =
        listOf(
            "executeQuery" to listOf("Statement", "PreparedStatement"),
            "executeUpdate" to listOf("Statement", "PreparedStatement"),
            "execute" to listOf("Statement"),
            "prepareStatement" to listOf("Connection"),
        )

    /**
     * Dangerous patterns that suggest untrusted input in SQL.
     */
    private val dangerousPatterns =
        listOf(
            "getParameter",
            "getQueryString",
            "getHeader",
            "getAttribute",
            "getInputStream",
            "getReader",
            "nextLine",
            "readLine",
            "next",
            "getText",
        )

    /**
     * Detect SQL injection vulnerabilities in a PSI file.
     *
     * @param psiFile Java source file to analyze
     * @return List of SQL injection findings
     */
    fun detect(psiFile: PsiFile): List<SastFinding> {
        val findings = mutableListOf<SastFinding>()
        val filePath = psiFile.virtualFile?.path ?: "unknown"

        psiFile.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    // Look for method call expressions
                    if (element is PsiMethodCallExpression) {
                        val methodName = element.methodExpression.referenceName ?: return

                        // Check if this is a SQL execution method
                        if (isSqlMethod(methodName)) {
                            val argument = element.argumentList.expressions.firstOrNull()
                            if (argument != null && !argument.isConstantExpression()) {
                                // Check if argument contains dangerous patterns
                                if (argument.containsUserInput()) {
                                    val lineNumber = getLineNumber(element, psiFile)
                                    findings.add(
                                        SastFinding(
                                            ruleId = "SQL-INJECTION",
                                            severity = SastSeverity.CRITICAL,
                                            message = "Potential SQL injection: untrusted input flows into SQL query",
                                            filePath = filePath,
                                            lineNumber = lineNumber,
                                            remediation = "Use PreparedStatement with parameterized queries instead of string concatenation.",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )

        return findings
    }

    /**
     * Check if a method name matches a known SQL execution pattern.
     */
    private fun isSqlMethod(methodName: String): Boolean = sqlMethodPatterns.any { it.first == methodName }

    /**
     * Check if a PSI element is a compile-time constant (safe from injection).
     */
    private fun PsiElement.isConstantExpression(): Boolean =
        when (this) {
            is PsiLiteralExpression -> true
            is PsiTypeCastExpression -> operand?.isConstantExpression() ?: false
            else -> false
        }

    /**
     * Check if a PSI element or its children contain user input patterns.
     */
    private fun PsiElement.containsUserInput(): Boolean {
        var found = false

        this.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (found) return
                    super.visitElement(element)

                    // Check for method calls suggesting user input
                    if (element is PsiMethodCallExpression) {
                        val refName = element.methodExpression.referenceName ?: return
                        if (dangerousPatterns.any { refName.contains(it, ignoreCase = true) }) {
                            found = true
                        }
                    }

                    // Check for variable references that might hold user input
                    if (element is PsiReferenceExpression) {
                        val name = element.referenceName ?: return
                        if (name.contains("request", ignoreCase = true) ||
                            name.contains("input", ignoreCase = true) ||
                            name.contains("param", ignoreCase = true) ||
                            name.contains("user", ignoreCase = true)
                        ) {
                            found = true
                        }
                    }
                }
            },
        )

        return found
    }

    /**
     * Get the 1-based line number of a PSI element within a file.
     */
    private fun getLineNumber(
        element: PsiElement,
        psiFile: PsiFile,
    ): Int {
        val document = psiFile.viewProvider.document ?: return 0
        return document.getLineNumber(element.textOffset) + 1
    }
}
