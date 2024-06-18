package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

interface VarHandleType : TypeLatticeElement<VarHandleType> {
    val variableType: Type
    val coordinateTypes: ParameterList
}

data object BotVarHandleType : VarHandleType, BotTypeLatticeElement<VarHandleType> {
    override val variableType = BotType
    override val coordinateTypes = BotParameterList
}

data object TopVarHandleType : VarHandleType, TopTypeLatticeElement<VarHandleType> {
    override fun self() = this
    override val variableType = TopType
    override val coordinateTypes = TopParameterList
}

data class CompleteVarHandleType(
    override val variableType: Type,
    override val coordinateTypes: ParameterList
) : VarHandleType {
    override fun joinIdentical(other: VarHandleType): Pair<VarHandleType, TriState> {
        val (vt, identicalVt) = variableType.joinIdentical(other.variableType)
        val (ct, identicalCt) = coordinateTypes.joinIdentical(other.coordinateTypes)
        val identical = identicalVt.sharpenTowardsNo(identicalCt)
        if (vt == TopType && ct == TopParameterList) {
            return TopVarHandleType to identical
        }
        if (vt == BotType && ct == BotParameterList) {
            return BotVarHandleType to identical
        }
        return CompleteVarHandleType(vt, ct) to identical
    }

    override fun toString(): String {
        return "$coordinateTypes($variableType)"
    }

}

