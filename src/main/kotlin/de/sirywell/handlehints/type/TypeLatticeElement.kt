package de.sirywell.handlehints.type

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import de.sirywell.handlehints.*
import kotlin.reflect.KClass

sealed interface TypeLatticeElement<LE: TypeLatticeElement<LE>> {
    fun join(other: LE) = joinIdentical(other).first
    fun joinIdentical(other: LE): Pair<LE, TriState>
    fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R
}

sealed interface BotTypeLatticeElement<LE: TypeLatticeElement<LE>> : TypeLatticeElement<LE> {
    override fun joinIdentical(other: LE): Pair<LE, TriState> {
        return other to TriState.UNKNOWN
    }
}
sealed interface TopTypeLatticeElement<LE: TypeLatticeElement<LE>> : TypeLatticeElement<LE> {
    override fun joinIdentical(other: LE): Pair<LE, TriState> {
        return self() to TriState.UNKNOWN
    }

    fun self(): LE
}

inline fun <reified T : TypeLatticeElement<*>> topForType(): T {
    return topForType(T::class)
}

private val topMap = object : ClassValue<TopTypeLatticeElement<*>?>() {
    override fun computeValue(type: Class<*>): TopTypeLatticeElement<*>? {
        val typeInfo = type.getAnnotation(TypeInfo::class.java)
        return typeInfo.topType.objectInstance
    }

}

fun <T : TypeLatticeElement<*>> topForType(clazz: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return (topMap[clazz.java] ?: throw UnsupportedOperationException("$clazz is not supported")) as T
}

fun topForType(psiType: PsiType, context: PsiElement): TypeLatticeElement<*> {
    return when (psiType) {
        methodTypeType(context) -> topForType<MethodHandleType>()
        methodHandleType(context) -> topForType<MethodHandleType>()
        varHandleType(context) -> topForType<VarHandleType>()
        pathElementType(context) -> topForType<PathElementType>()
        in memoryLayoutTypes(context) -> topForType<MemoryLayoutType>()
        else -> throw UnsupportedOperationException("${psiType.presentableText} is not supported")
    }
}