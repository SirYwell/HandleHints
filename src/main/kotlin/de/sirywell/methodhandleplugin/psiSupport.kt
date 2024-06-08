package de.sirywell.methodhandleplugin

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.CommonDataflow.DataflowResult
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.JavaReflectionReferenceUtil
import com.intellij.psi.search.GlobalSearchScope
import de.sirywell.methodhandleplugin.type.*
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

fun Collection<PsiExpression>.mapToTypes(): List<Type> {
    return this.map { element -> element.asType() }
}

fun PsiExpression.asType(): Type {
    return (JavaReflectionReferenceUtil.getReflectiveType(this)?.type
        ?: getConstantOfType<PsiType>())
        ?.let { DirectType(it) }
        ?: BotType
}

fun objectType(manager: PsiManager, scope: GlobalSearchScope): PsiType {
    return PsiType.getJavaLangObject(manager, scope)
}

fun findMethodMatching(signature: Signature, methods: Array<PsiMethod>): PsiMethod? {
    if (signature !is CompleteSignature) return null
    if (signature.returnType !is DirectType) return null
    val parameterList = signature.parameterList as? CompleteParameterList ?: return null
    return methods.find { matches(it, signature.returnType as DirectType, parameterList) }
}

fun matches(method: PsiMethod, returnType: DirectType, parameterList: CompleteParameterList): Boolean {
    if (method.returnType == null) {
        if (returnType.psiType != PsiTypes.voidType()) {
            return false
        }
    } else if (method.returnType != returnType.psiType) return false
    if (parameterList.size != method.parameterList.parametersCount) return false
    val ps = parameterList.parameterTypes.map { it as? DirectType ?: return false }.map { it.psiType }
    return method.parameterList.parameters
        .map { if (it.type is PsiEllipsisType) (it.type as PsiEllipsisType).toArrayType() else it.type }
        .foldIndexed(true) { index, acc, param -> acc && ps[index] == param }
}
