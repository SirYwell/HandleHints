package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

sealed interface Signature : TypeLatticeElement<Signature> {


    fun withReturnType(returnType: Type): Signature

    fun withParameterTypes(parameterTypes: List<Type>) = withParameterTypes(CompleteParameterList(parameterTypes))
    fun withParameterTypes(parameterTypes: ParameterList): Signature

    fun withVarargs(varargs: TriState): Signature

    fun parameterTypeAt(index: Int): Type

    val returnType: Type

    val parameterList: ParameterList

    val varargs: TriState
}

data object BotSignature : Signature {
    override fun joinIdentical(other: Signature) = other to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = CompleteSignature(returnType, parameterList, TriState.NO)

    override fun withParameterTypes(parameterTypes: ParameterList) =
        CompleteSignature(returnType, parameterTypes, TriState.NO)

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = BotType

    override val returnType get() = BotType

    override val parameterList get() = BotParameterList

    override val varargs get() = TriState.UNKNOWN
}

data object TopSignature : Signature {
    override fun joinIdentical(other: Signature) = this to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: ParameterList) = this

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = TopType

    override val returnType get() = TopType

    override val parameterList get() = TopParameterList

    override val varargs get() = TriState.UNKNOWN
}

@JvmRecord
data class CompleteSignature(
    override val returnType: Type,
    override val parameterList: ParameterList,
    override val varargs: TriState
) : Signature {
    override fun joinIdentical(other: Signature): Pair<Signature, TriState> {
        val (ret, rIdentical) = returnType.joinIdentical(other.returnType)
        val (params, pIdentical) = parameterList.joinIdentical(other.parameterList)
        return CompleteSignature(ret, params, TriState.NO) to rIdentical.sharpenTowardsNo(pIdentical)
    }

    override fun withReturnType(returnType: Type): Signature {
        return CompleteSignature(returnType, parameterList, TriState.NO)
    }

    override fun withParameterTypes(parameterTypes: ParameterList): Signature {
        return CompleteSignature(returnType, parameterTypes, TriState.NO)
    }

    override fun withVarargs(varargs: TriState): Signature {
        return CompleteSignature(returnType, parameterList, varargs)
    }

    override fun parameterTypeAt(index: Int): Type {
        return parameterList[index]
    }

    override fun toString(): String {
        return parameterList.toString() + returnType
    }

}

fun complete(returnType: Type, parameterTypes: List<Type>): Signature {
    return complete(returnType, CompleteParameterList(parameterTypes))
}

fun complete(returnType: Type, parameterTypes: ParameterList): Signature {
    return CompleteSignature(returnType, parameterTypes, TriState.NO)
}
