// Lightweight Path Traversal Detector
package io.dyuti.osvplugin.sast

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiRecursiveElementVisitor

/**
 * Detects path traversal vulnerabilities by finding file system operations
 * that accept untrusted input for paths.
 *
 * ## What it catches
 *
 * ```java
 * // BAD — Path traversal risk
 * new FileInputStream("/uploads/" + request.getParameter("file"));
 * Paths.get(userInput);
 * new File(request.getHeader("X-File-Path"));
 * ```
 */
class PathTraversalDetector {
    /**
     * Dangerous file operation constructors/methods.
     */
    private val dangerousCalls =
        listOf(
            // Java IO
            "FileInputStream" to "<init>",
            "FileOutputStream" to "<init>",
            "FileReader" to "<init>",
            "FileWriter" to "<init>",
            "File" to "<init>",
            "RandomAccessFile" to "<init>",
            // NIO
            "Paths" to "get",
            "Files" to "readAllBytes",
            "Files" to "readAllLines",
            "Files" to "readString",
            "Files" to "write",
            "Files" to "copy",
            "Files" to "move",
        )

    /**
     * Patterns indicating untrusted input.
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
                        val qualifier = element.methodExpression.qualifierExpression?.text ?: ""

                        // Check NIO static methods like Paths.get(...)
                        if (isDangerousNioCall(qualifier, methodName)) {
                            checkArguments(element, filePath, findings, psiFile)
                        }
                    }

                    // Detect constructor calls: new File(...), new FileInputStream(...)
                    val elementType = element::class.java.simpleName
                    if (elementType == "PsiNewExpression") {
                        try {
                            val classRef =
                                element::class.java
                                    .getMethod("getClassReference")
                                    ?.invoke(element)
                            val className = classRef?.toString()?.split(".")?.lastOrNull() ?: ""

                            if (isDangerousConstructor(className)) {
                                val argsMethod = element::class.java.getMethod("getArgumentList")
                                val args = argsMethod?.invoke(element)

                                @Suppress("UNCHECKED_CAST")
                                val expressions =
                                    (
                                        args
                                            ?.javaClass
                                            ?.getMethod("getExpressions")
                                            ?.invoke(args) as? Array<PsiElement>
                                    )?.toList() ?: emptyList()

                                if (expressions.any { it.containsUntrustedInput() }) {
                                    val line = getLineNumber(element, psiFile)
                                    findings.add(
                                        SastFinding(
                                            ruleId = "PATH-TRAVERSAL",
                                            severity = SastSeverity.HIGH,
                                            message = "Potential path traversal: untrusted input used in file path",
                                            filePath = filePath,
                                            lineNumber = line,
                                            remediation = "Validate and sanitize user-provided paths. Use canonical paths and restrict to allowed directories.",
                                        ),
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            // Reflection errors — ignore
                        }
                    }
                }
            },
        )

        return findings
    }

    private fun isDangerousNioCall(
        qualifier: String,
        methodName: String,
    ): Boolean =
        dangerousCalls.any {
            it.first == qualifier && it.second == methodName
        }

    private fun isDangerousConstructor(className: String): Boolean = dangerousCalls.any { it.first == className && it.second == "<init>" }

    private fun checkArguments(
        callExpr: PsiMethodCallExpression,
        filePath: String,
        findings: MutableList<SastFinding>,
        psiFile: PsiFile,
    ) {
        val args = callExpr.argumentList.expressions
        for (arg in args) {
            if (arg.containsUntrustedInput()) {
                val line = getLineNumber(callExpr, psiFile)
                findings.add(
                    SastFinding(
                        ruleId = "PATH-TRAVERSAL",
                        severity = SastSeverity.HIGH,
                        message = "Potential path traversal: untrusted input used in file path",
                        filePath = filePath,
                        lineNumber = line,
                        remediation = "Validate and sanitize user-provided paths. Use canonical paths and restrict to allowed directories.",
                    ),
                )
                return
            }
        }
    }

    private fun PsiElement.containsUntrustedInput(): Boolean {
        var found = false
        this.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (found) return
                    super.visitElement(element)

                    if (element is PsiMethodCallExpression) {
                        val name = element.methodExpression.referenceName ?: return
                        if (untrustedPatterns.any { name.contains(it, ignoreCase = true) }) {
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
