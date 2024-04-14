package de.sirywell.methodhandleplugin

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.CommonDataflow.DataflowResult
import com.intellij.psi.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType.methodType

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

// if only >=241 is supported, this can be simplified again
@Suppress("UnstableApiUsage")
val getDataflowResult: MethodHandle = run {
    val lookup = MethodHandles.lookup()
    val commonDataflowClass = CommonDataflow::class.java
    try {
        lookup.findStatic(
            commonDataflowClass,
            "getDataflowResult",
            methodType(DataflowResult::class.java, PsiExpression::class.java)
        )
    } catch (_: Exception) {
        lookup.findStatic(
            commonDataflowClass,
            "getDataflowResult",
            methodType(DataflowResult::class.java, PsiElement::class.java)
        )
    }
}

@Suppress("UnstableApiUsage")
inline fun <reified T> PsiExpression.getConstantOfType(): T? {
    return (getDataflowResult(this) as DataflowResult?)
        ?.getDfType(this)
        ?.getConstantOfType(T::class.java)
}
