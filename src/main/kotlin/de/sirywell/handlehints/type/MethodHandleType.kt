package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

sealed interface MethodHandleType : TypeLatticeElement<MethodHandleType> {


    fun withReturnType(returnType: Type): MethodHandleType

    fun withParameterTypes(parameterTypes: List<Type>) = withParameterTypes(CompleteTypeList(parameterTypes))
    fun withParameterTypes(parameterTypes: TypeList): MethodHandleType

    fun withVarargs(varargs: TriState): MethodHandleType

    fun parameterTypeAt(index: Int): Type

    val returnType: Type

    val typeLatticeElementList: TypeList

    val varargs: TriState
}

data object BotMethodHandleType : MethodHandleType, BotTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = other to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = CompleteMethodHandleType(returnType, typeLatticeElementList, TriState.NO)

    override fun withParameterTypes(parameterTypes: TypeList) =
        CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = BotType

    override val returnType get() = BotType

    override val typeLatticeElementList get() = BotTypeList

    override val varargs get() = TriState.UNKNOWN
}

data object TopMethodHandleType : MethodHandleType, TopTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = this to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: TypeList) = this

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = TopType

    override val returnType get() = TopType

    override val typeLatticeElementList get() = TopTypeList

    override val varargs get() = TriState.UNKNOWN
    override fun self() = this
}

@JvmRecord
data class CompleteMethodHandleType(
    override val returnType: Type,
    override val typeLatticeElementList: TypeList,
    override val varargs: TriState
) : MethodHandleType {
    override fun joinIdentical(other: MethodHandleType): Pair<MethodHandleType, TriState> {
        val (ret, rIdentical) = returnType.joinIdentical(other.returnType)
        val (params, pIdentical) = typeLatticeElementList.joinIdentical(other.typeLatticeElementList)
        return CompleteMethodHandleType(ret, params, TriState.NO) to rIdentical.sharpenTowardsNo(pIdentical)
    }

    override fun withReturnType(returnType: Type): MethodHandleType {
        return CompleteMethodHandleType(returnType, typeLatticeElementList, TriState.NO)
    }

    override fun withParameterTypes(parameterTypes: TypeList): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
    }

    override fun withVarargs(varargs: TriState): MethodHandleType {
        return CompleteMethodHandleType(returnType, typeLatticeElementList, varargs)
    }

    override fun parameterTypeAt(index: Int): Type {
        return typeLatticeElementList[index]
    }

    override fun toString(): String {
        return typeLatticeElementList.toString() + returnType
    }

}

fun complete(returnType: Type, parameterTypes: List<Type>): MethodHandleType {
    return complete(returnType, CompleteTypeList(parameterTypes))
}

fun complete(returnType: Type, parameterTypes: TypeList): MethodHandleType {
    return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
}
