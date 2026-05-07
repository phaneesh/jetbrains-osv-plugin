// Lightweight XSS (Cross-Site Scripting) Detector
package io.dyuti.osvplugin.sast

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiReferenceExpression

/**
 * Detects reflected XSS vulnerabilities by finding untrusted input
 * that flows into HTML/response output without escaping.
 *
 * ## What it catches
 *
 * ```java
 * // BAD — XSS risk
 * out.println("<script>" + request.getParameter("name") + "</script>");
 * response.getWriter().write(userInput);
 * model.addAttribute("message", request.getParameter("msg"));
 * ```
 *
 * ## What it misses
 *
 * - Stored XSS (requires database flow analysis)
 * - DOM-based XSS (requires JavaScript analysis)
 * - Properly-escaped output
 */
class XssDetector {
    /**
     * Dangerous output sinks that write to HTML/response.
     */
    private val dangerousSinks =
        listOf(
            // Servlet response writers
            "print" to listOf("PrintWriter", "JspWriter"),
            "println" to listOf("PrintWriter", "JspWriter"),
            "write" to listOf("PrintWriter", "Writer", "OutputStreamWriter"),
            "append" to listOf("PrintWriter", "StringWriter"),
            // Spring Model/ModelMap
            "addAttribute" to listOf("Model", "ModelMap", "ModelAndView"),
            "setAttribute" to listOf("Model", "ModelMap", "ServletRequest", "HttpServletRequest"),
            // JSP implicit objects
            "getOut" to listOf("PageContext"),
        )

    /**
     * Patterns indicating untrusted input flowing into output.
     */
    private val untrustedPatterns =
        listOf(
            "getParameter",
            "getQueryString",
            "getHeader",
            "getAttribute",
            "getPathInfo",
            "getInputStream",
            "getReader",
            "nextLine",
            "readLine",
        )

    fun detect(psiFile: PsiFile): List<SastFinding> {
        val findings = mutableListOf<SastFinding>()
        val filePath = psiFile.virtualFile?.path ?: "unknown"

        psiFile.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    if (element is PsiMethodCallExpression) {
                        val methodName = element.methodExpression.referenceName ?: return

                        if (isDangerousSink(methodName)) {
                            val args = element.argumentList.expressions
                            for (arg in args) {
                                if (arg.containsUserInput()) {
                                    val line = getLineNumber(element, psiFile)
                                    findings.add(
                                        SastFinding(
                                            ruleId = "XSS-REFLECTED",
                                            severity = SastSeverity.HIGH,
                                            message = "Potential XSS: untrusted input is output to response without escaping",
                                            filePath = filePath,
                                            lineNumber = line,
                                            remediation = "Use a templating engine with auto-escaping (Thymeleaf, JSP EL) or HTML-encode user input before output.",
                                        ),
                                    )
                                    return
                                }
                            }
                        }
                    }
                }
            },
        )

        return findings
    }

    private fun isDangerousSink(methodName: String): Boolean = dangerousSinks.any { it.first == methodName }

    private fun PsiElement.containsUserInput(): Boolean {
        var found = false
        this.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (found) return
                    super.visitElement(element)

                    if (element is PsiMethodCallExpression) {
                        val refName = element.methodExpression.referenceName ?: return
                        if (untrustedPatterns.any { refName.contains(it, ignoreCase = true) }) {
                            found = true
                        }
                    }

                    if (element is PsiReferenceExpression) {
                        val name = element.referenceName ?: return
                        if (name.contains("request", ignoreCase = true) ||
                            name.contains("input", ignoreCase = true) ||
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

    private fun getLineNumber(
        element: PsiElement,
        psiFile: PsiFile,
    ): Int {
        val document = psiFile.viewProvider.document ?: return 0
        return document.getLineNumber(element.textOffset) + 1
    }
}
