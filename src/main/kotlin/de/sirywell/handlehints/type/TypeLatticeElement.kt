package de.sirywell.handlehints.type

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import de.sirywell.handlehints.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

inline fun <reified T : TypeLatticeElement<*>> bottomForType(): T {
    return bottomForType(T::class)
}

fun <T : TypeLatticeElement<*>> bottomForType(clazz: KClass<T>): T {
    if (clazz == MethodHandleType::class) {
        return clazz.java.cast(BotMethodHandleType)
    }
    if (clazz == VarHandleType::class) {
        return clazz.java.cast(BotVarHandleType)
    }
    if (clazz == Type::class) {
        return clazz.java.cast(BotType)
    }
    if (clazz.isSubclassOf(MemoryLayoutType::class)) {
        return clazz.java.cast(BotMemoryLayoutType)
    }
    throw UnsupportedOperationException("$clazz is not supported")
}

fun bottomForType(psiType: PsiType, context: PsiElement): TypeLatticeElement<*> {
    return when (psiType) {
        methodTypeType(context) -> bottomForType<MethodHandleType>()
        methodHandleType(context) -> bottomForType<MethodHandleType>()
        varHandleType(context) -> bottomForType<VarHandleType>()
        in memoryLayoutTypes(context) -> bottomForType<MemoryLayoutType>()
        else -> throw UnsupportedOperationException("${psiType.presentableText} is not supported")
    }
}

inline fun <reified T : TypeLatticeElement<*>> topForType(): T {
    return topForType(T::class)
}

fun <T : TypeLatticeElement<*>> topForType(clazz: KClass<T>): T {
    if (clazz == MethodHandleType::class) {
        return clazz.java.cast(TopMethodHandleType)
    }
    if (clazz == VarHandleType::class) {
        return clazz.java.cast(TopVarHandleType)
    }
    if (clazz == Type::class) {
        return clazz.java.cast(TopType)
    }
    if (clazz.isSubclassOf(MemoryLayoutType::class)) {
        return clazz.java.cast(TopMemoryLayoutType)
    }
    throw UnsupportedOperationException("$clazz is not supported")
}

fun topForType(psiType: PsiType, context: PsiElement): TypeLatticeElement<*> {
    return when (psiType) {
        methodTypeType(context) -> topForType<MethodHandleType>()
        methodHandleType(context) -> topForType<MethodHandleType>()
        varHandleType(context) -> topForType<VarHandleType>()
        in memoryLayoutTypes(context) -> topForType<MemoryLayoutType>()
        else -> throw UnsupportedOperationException("${psiType.presentableText} is not supported")
    }
}