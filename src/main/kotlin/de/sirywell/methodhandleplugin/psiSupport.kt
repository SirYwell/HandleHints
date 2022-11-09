package de.sirywell.methodhandleplugin

import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiVariable

val PsiMethodCallExpression.methodName
    get() = this.methodExpression.referenceName

fun isJavaLangInvoke(element: PsiMethodCallExpression) =
    (element.resolveMethod()?.containingClass?.containingFile as? PsiJavaFile)?.packageName == "java.lang.invoke"

fun receiverIsInvokeClass(element: PsiMethodCallExpression, className: String) =
    element.resolveMethod()?.containingClass?.qualifiedName == "java.lang.invoke.$className"

fun receiverIsMethodHandle(element: PsiMethodCallExpression) =
    receiverIsInvokeClass(element, "MethodHandle")

fun receiverIsMethodHandles(element: PsiMethodCallExpression) =
    receiverIsInvokeClass(element, "MethodHandles")

fun receiverIsMethodType(element: PsiMethodCallExpression) = receiverIsInvokeClass(element, "MethodType")

fun PsiDeclarationStatement.getVariable(): PsiVariable? {
    if (this.declaredElements.isEmpty()) return null
    val element = this.declaredElements[0]
    return element as? PsiVariable
}
