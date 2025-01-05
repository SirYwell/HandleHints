package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

@TypeInfo(TopVarHandleType::class)
interface VarHandleType : TypeLatticeElement<VarHandleType> {
    val variableType: Type
    val coordinateTypes: TypeList
    val invocationBehavior: InvocationBehavior
    fun withInvokeBehavior(behavior: InvocationBehavior): VarHandleType
}

sealed interface InvocationBehavior : TypeLatticeElement<InvocationBehavior>
data object BotInvocationBehavior : InvocationBehavior, BotTypeLatticeElement<InvocationBehavior> {
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }
}

data object TopInvocationBehavior : InvocationBehavior, TopTypeLatticeElement<InvocationBehavior> {
    override fun self() = this

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }
}

enum class KnownInvocationBehavior(val readableName: String) : InvocationBehavior {
    INVOKE("invoke"),
    INVOKE_EXACT("invokeExact");

    override fun joinIdentical(other: InvocationBehavior): Pair<InvocationBehavior, TriState> {
        return when (other) {
            is BotInvocationBehavior -> this to TriState.UNKNOWN
            is TopInvocationBehavior -> TopInvocationBehavior to TriState.UNKNOWN
            else -> if (this == other) this to TriState.YES else TopInvocationBehavior to TriState.NO
        }
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }
}

data object BotVarHandleType : VarHandleType, BotTypeLatticeElement<VarHandleType> {
    override fun withInvokeBehavior(behavior: InvocationBehavior) =
        CompleteVarHandleType(this.variableType, this.coordinateTypes, behavior)
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override val variableType = BotType
    override val coordinateTypes = BotTypeList
    override val invocationBehavior = BotInvocationBehavior
}

data object TopVarHandleType : VarHandleType, TopTypeLatticeElement<VarHandleType> {
    override fun self() = this
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun withInvokeBehavior(behavior: InvocationBehavior) = this
    override val variableType = TopType
    override val coordinateTypes = TopTypeList
    override val invocationBehavior = TopInvocationBehavior
}

data class CompleteVarHandleType(
    override val variableType: Type,
    override val coordinateTypes: TypeList,
    override val invocationBehavior: InvocationBehavior
) : VarHandleType {
    override fun withInvokeBehavior(behavior: InvocationBehavior) =
        CompleteVarHandleType(this.variableType, this.coordinateTypes, behavior)

    override fun joinIdentical(other: VarHandleType): Pair<VarHandleType, TriState> {
        val (vt, identicalVt) = variableType.joinIdentical(other.variableType)
        val (ct, identicalCt) = coordinateTypes.joinIdentical(other.coordinateTypes)
        val (ib, identicalIb) = invocationBehavior.joinIdentical(other.invocationBehavior)
        val identical = identicalVt.sharpenTowardsNo(identicalCt).sharpenTowardsNo(identicalIb)
        if (vt == TopType && ct == TopTypeList) {
            return TopVarHandleType to identical
        }
        if (vt == BotType && ct == BotTypeList) {
            return BotVarHandleType to identical
        }
        return CompleteVarHandleType(vt, ct, ib) to identical
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

