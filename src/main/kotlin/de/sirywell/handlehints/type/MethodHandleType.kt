package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

sealed interface MethodHandleType : TypeLatticeElement<MethodHandleType> {


    fun withReturnType(returnType: Type): MethodHandleType

    fun withParameterTypes(parameterTypes: List<Type>) = withParameterTypes(CompleteTypeList(parameterTypes))
    fun withParameterTypes(parameterTypes: TypeList): MethodHandleType

    fun withVarargs(varargs: TriState): MethodHandleType

    fun parameterTypeAt(index: Int): Type

    val returnType: Type

    val parameterTypes: TypeList

    val varargs: TriState
}

data object BotMethodHandleType : MethodHandleType, BotTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = other to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) =
        CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)

    override fun withParameterTypes(parameterTypes: TypeList) =
        CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = BotType

    override val returnType get() = BotType

    override val parameterTypes get() = BotTypeList

    override val varargs get() = TriState.UNKNOWN

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}


data object TopMethodHandleType : MethodHandleType, TopTypeLatticeElement<MethodHandleType> {
    override fun joinIdentical(other: MethodHandleType) = this to TriState.UNKNOWN

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: TypeList) = this

    override fun withVarargs(varargs: TriState) = this

    override fun parameterTypeAt(index: Int) = TopType

    override val returnType get() = TopType

    override val parameterTypes get() = TopTypeList

    override val varargs get() = TriState.UNKNOWN
    override fun self() = this
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

@JvmRecord
data class CompleteMethodHandleType(
    override val returnType: Type,
    override val parameterTypes: TypeList,
    override val varargs: TriState
) : MethodHandleType {
    override fun joinIdentical(other: MethodHandleType): Pair<MethodHandleType, TriState> {
        val (ret, rIdentical) = returnType.joinIdentical(other.returnType)
        val (params, pIdentical) = parameterTypes.joinIdentical(other.parameterTypes)
        return CompleteMethodHandleType(ret, params, TriState.NO) to rIdentical.sharpenTowardsNo(pIdentical)
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override fun withReturnType(returnType: Type): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
    }

    override fun withParameterTypes(parameterTypes: TypeList): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
    }

    override fun withVarargs(varargs: TriState): MethodHandleType {
        return CompleteMethodHandleType(returnType, parameterTypes, varargs)
    }

    override fun parameterTypeAt(index: Int): Type {
        return parameterTypes[index]
    }

    override fun toString(): String {
        return parameterTypes.toString() + returnType
    }

}

fun complete(returnType: Type, parameterTypes: List<Type>): MethodHandleType {
    return complete(returnType, CompleteTypeList(parameterTypes))
}

fun complete(returnType: Type, parameterTypes: TypeList): MethodHandleType {
    return CompleteMethodHandleType(returnType, parameterTypes, TriState.NO)
}
