package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

sealed interface MethodHandleType : TypeLatticeElement<MethodHandleType> {


    fun withReturnType(returnType: Type): MethodHandleType

    fun withParameterTypes(parameterTypes: List<Type>) = withParameterTypes(CompleteParameterList(parameterTypes))
    fun withParameterTypes(parameterTypes: ParameterList): MethodHandleType

    fun withVarargs(varargs: TriState): MethodHandleType

    fun parameterTypeAt(index: Int): Type

    val returnType: Type

    val parameterList: ParameterList

    val varargs: TriState
}

data object BotMethodHandleType : MethodHandleType, BotTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = other to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = CompleteMethodHandleType(returnType, parameterList, TriState.NO)

    override fun withParameterTypes(parameterTypes: ParameterList) =
        CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = BotType

    override val returnType get() = BotType

    override val parameterList get() = BotParameterList

    override val varargs get() = TriState.UNKNOWN
}

data object TopMethodHandleType : MethodHandleType, TopTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = this to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: ParameterList) = this

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = TopType

    override val returnType get() = TopType

    override val parameterList get() = TopParameterList

    override val varargs get() = TriState.UNKNOWN
    override fun self() = this
}

@JvmRecord
data class CompleteMethodHandleType(
    override val returnType: Type,
    override val parameterList: ParameterList,
    override val varargs: TriState
) : MethodHandleType {
    override fun joinIdentical(other: MethodHandleType): Pair<MethodHandleType, TriState> {
        val (ret, rIdentical) = returnType.joinIdentical(other.returnType)
        val (params, pIdentical) = parameterList.joinIdentical(other.parameterList)
        return CompleteMethodHandleType(ret, params, TriState.NO) to rIdentical.sharpenTowardsNo(pIdentical)
    }

    override fun withReturnType(returnType: Type): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterList, TriState.NO)
    }

    override fun withParameterTypes(parameterTypes: ParameterList): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
    }

    override fun withVarargs(varargs: TriState): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterList, varargs)
    }

    override fun parameterTypeAt(index: Int): Type {
        return parameterList[index]
    }

    override fun toString(): String {
        return parameterList.toString() + returnType
    }

}

fun complete(returnType: Type, parameterTypes: List<Type>): MethodHandleType {
    return complete(returnType, CompleteParameterList(parameterTypes))
}

fun complete(returnType: Type, parameterTypes: ParameterList): MethodHandleType {
    return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
}
