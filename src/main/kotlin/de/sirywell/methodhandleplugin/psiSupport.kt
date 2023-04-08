package de.sirywell.methodhandleplugin

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.psi.*

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

fun receiverIsLookup(element: PsiMethodCallExpression) = receiverIsInvokeClass(element, "MethodHandles.Lookup")

fun PsiDeclarationStatement.getVariable(): PsiVariable? {
    if (this.declaredElements.isEmpty()) return null
    val element = this.declaredElements[0]
    return element as? PsiVariable
}

@Suppress("UnstableApiUsage")
inline fun <reified T> PsiExpression.getConstantOfType(): T? {
    return CommonDataflow.getDataflowResult(this)
        ?.getDfType(this)
        ?.getConstantOfType(T::class.java)
}
