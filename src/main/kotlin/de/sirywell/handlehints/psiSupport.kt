package de.sirywell.handlehints

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.CommonDataflow.DataflowResult
import com.intellij.codeInspection.dataFlow.types.DfType
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.JavaReflectionReferenceUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import de.sirywell.handlehints.type.*
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

fun receiverIsMemoryLayout(element: PsiMethodCallExpression): Boolean {
    val superType = PsiType.getTypeByName("java.lang.foreign.MemoryLayout", element.project, element.resolveScope)
    val actual = PsiTypesUtil.getClassType(element.resolveMethod()?.containingClass ?: return false)
    return superType.isAssignableFrom(actual)
}

fun receiverIsPathElement(element: PsiMethodCallExpression): Boolean {
    return element.resolveMethod()?.containingClass?.qualifiedName == "java.lang.foreign.MemoryLayout.PathElement"
}

fun receiverIsFunctionDescriptor(element: PsiMethodCallExpression): Boolean {
    return element.resolveMethod()?.containingClass?.qualifiedName == "java.lang.foreign.FunctionDescriptor"
}

fun methodHandleType(element: PsiElement): PsiClassType {
    return PsiType.getTypeByName("java.lang.invoke.MethodHandle", element.project, element.resolveScope)
}

fun varHandleType(element: PsiElement): PsiClassType {
    return PsiType.getTypeByName("java.lang.invoke.VarHandle", element.project, element.resolveScope)
}

fun methodTypeType(element: PsiElement): PsiClassType {
    return PsiType.getTypeByName("java.lang.invoke.MethodType", element.project, element.resolveScope)
}

fun pathElementType(element: PsiElement): PsiClassType {
    return PsiType.getTypeByName("java.lang.foreign.MemoryLayout.PathElement", element.project, element.resolveScope)
}

fun functionDescriptorType(element: PsiElement): PsiClassType {
    return PsiType.getTypeByName("java.lang.foreign.FunctionDescriptor", element.project, element.resolveScope)
}

fun objectType(element: PsiElement): PsiType {
    return PsiType.getJavaLangObject(element.manager, element.resolveScope)
}

fun memoryLayoutTypes(context: PsiElement): Set<PsiType> {
    // use a cache here
    return object {
        // https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/MemoryLayout-sealed-graph.svg
        val memoryLayoutTypes = listOf(
            "java.lang.foreign.MemoryLayout",
            "java.lang.foreign.SequenceLayout",
            "java.lang.foreign.GroupLayout",
            "java.lang.foreign.StructLayout",
            "java.lang.foreign.UnionLayout",
            "java.lang.foreign.PaddingLayout",
            "java.lang.foreign.ValueLayout",
            "java.lang.foreign.ValueLayout.OfBoolean",
            "java.lang.foreign.ValueLayout.OfByte",
            "java.lang.foreign.ValueLayout.OfChar",
            "java.lang.foreign.ValueLayout.OfShort",
            "java.lang.foreign.ValueLayout.OfInt",
            "java.lang.foreign.ValueLayout.OfFloat",
            "java.lang.foreign.ValueLayout.OfLong",
            "java.lang.foreign.ValueLayout.OfDouble",
            "java.lang.foreign.AddressLayout",
        )
            .map { PsiType.getTypeByName(it, context.project, context.resolveScope) }
            .toSet()
    }.memoryLayoutTypes
}


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

inline fun <reified T> PsiExpression.getConstantOfType(): T? {
    return getDfType()?.getConstantOfType(T::class.java)
}

@Suppress("UnstableApiUsage")
fun PsiExpression.getDfType(): DfType? {
    return (getDataflowResult(this) as DataflowResult?)
        ?.getDfType(this)
}

fun PsiExpression.getConstantLong(): Long? {
    return (getConstantOfType<Int>()
        ?: getConstantOfType<Long>())?.toLong()
}


fun Collection<PsiExpression>.mapToTypes(): List<Type> {
    return this.map { element -> element.asType() }
}

fun PsiExpression.asType(): Type {
    return (JavaReflectionReferenceUtil.getReflectiveType(this)?.type
        ?: getConstantOfType<PsiType>())
        ?.let { ExactType(it) }
        ?: BotType
}

fun objectType(manager: PsiManager, scope: GlobalSearchScope): PsiType {
    return PsiType.getJavaLangObject(manager, scope)
}

fun findMethodMatching(methodHandleType: MethodHandleType, methods: Array<PsiMethod>): PsiMethod? {
    if (methodHandleType !is CompleteMethodHandleType) return null
    if (methodHandleType.returnType !is ExactType) return null
    val parameterList = methodHandleType.parameterTypes as? CompleteTypeList ?: return null
    return methods.find { matches(it, methodHandleType.returnType as ExactType, parameterList) }
}

fun matches(method: PsiMethod, returnType: ExactType, parameterList: CompleteTypeList): Boolean {
    if (method.returnType == null) {
        if (returnType.psiType != PsiTypes.voidType()) {
            return false
        }
    } else if (method.returnType != returnType.psiType) return false
    if (parameterList.size != method.parameterList.parametersCount) return false
    val ps = parameterList.typeList.map { it as? ExactType ?: return false }.map { it.psiType }
    return method.parameterList.parameters
        .map { if (it.type is PsiEllipsisType) (it.type as PsiEllipsisType).toArrayType() else it.type }
        .foldIndexed(true) { index, acc, param -> acc && ps[index] == param }
}

/**
 * Returns true if the variable type has no [MethodHandleType]
 */
fun isUnrelated(variable: PsiVariable): Boolean {
    return isUnrelated(variable.type, variable)
}

fun isUnrelated(type: PsiType, context: PsiElement): Boolean {
    return type != methodTypeType(context)
            && type != methodHandleType(context)
            && type != varHandleType(context)
            && type != pathElementType(context)
            && type != functionDescriptorType(context)
            && type !in memoryLayoutTypes(context)
}
