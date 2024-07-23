package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

@TypeInfo(TopVarHandleType::class)
interface VarHandleType : TypeLatticeElement<VarHandleType> {
    val variableType: Type
    val coordinateTypes: TypeList
}

data object BotVarHandleType : VarHandleType, BotTypeLatticeElement<VarHandleType> {
    override val variableType = BotType
    override val coordinateTypes = BotTypeList
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object TopVarHandleType : VarHandleType, TopTypeLatticeElement<VarHandleType> {
    override fun self() = this
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override val variableType = TopType
    override val coordinateTypes = TopTypeList
}

data class CompleteVarHandleType(
    override val variableType: Type,
    override val coordinateTypes: TypeList
) : VarHandleType {
    override fun joinIdentical(other: VarHandleType): Pair<VarHandleType, TriState> {
        val (vt, identicalVt) = variableType.joinIdentical(other.variableType)
        val (ct, identicalCt) = coordinateTypes.joinIdentical(other.coordinateTypes)
        val identical = identicalVt.sharpenTowardsNo(identicalCt)
        if (vt == TopType && ct == TopTypeList) {
            return TopVarHandleType to identical
        }
        if (vt == BotType && ct == BotTypeList) {
            return BotVarHandleType to identical
        }
        return CompleteVarHandleType(vt, ct) to identical
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

