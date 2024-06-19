package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

sealed interface TypeLatticeElement<LE: TypeLatticeElement<LE>> {
    fun join(other: LE) = joinIdentical(other).first
    fun joinIdentical(other: LE): Pair<LE, TriState>
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