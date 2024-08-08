package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

/**
 * A generic type representing functions consisting of parameters and a return type of type [T].
 */
sealed interface FunctionType<S : FunctionType<S, T>, T : TypeLatticeElement<T>> : TypeLatticeElement<S> {
    fun withReturnType(returnType: T): S

    fun withParameterTypes(parameterTypes: List<T>): S
    fun withParameterTypes(parameterTypes: TypeLatticeElementList<T>): S

    fun parameterTypeAt(index: Int): T

    val returnType: T

    val parameterTypes: TypeLatticeElementList<T>
}

sealed interface CompleteFunctionType<S : FunctionType<S, T>, T : TypeLatticeElement<T>> : FunctionType<S, T> {
    override fun joinIdentical(other: S): Pair<S, TriState> {
        val (ret, rIdentical) = returnType.joinIdentical(other.returnType)
        val (params, pIdentical) = parameterTypes.joinIdentical(other.parameterTypes)
        return copy(ret, params) to rIdentical.sharpenTowardsNo(pIdentical)
    }

    override fun withReturnType(returnType: T): S {
        return copy(returnType, parameterTypes)
    }

    override fun withParameterTypes(parameterTypes: TypeLatticeElementList<T>): S {
        return copy(returnType, parameterTypes)
    }

    override fun parameterTypeAt(index: Int): T {
        return parameterTypes[index]
    }

    fun copy(returnType: T, parameterTypes: TypeLatticeElementList<T>): S
}