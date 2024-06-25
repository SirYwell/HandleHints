package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.toTriState

interface MemoryLayoutType : TypeLatticeElement<MemoryLayoutType> {
}

data object BotMemoryLayoutType : MemoryLayoutType, BotTypeLatticeElement<MemoryLayoutType> {
    override fun toString(): String {
        return "⊥"
    }
}

data object TopMemoryLayoutType : MemoryLayoutType, TopTypeLatticeElement<MemoryLayoutType> {
    override fun self() = this

    override fun toString(): String {
        return "⊤"
    }
}

data class ValueLayoutType(val type: Type, val byteAlignment: Int?) : MemoryLayoutType {
    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is ValueLayoutType) {
            val (new, identical) = this.type.joinIdentical(other.type)
            val identicalAlignment = (byteAlignment?.equals(other.byteAlignment)).toTriState()
            return ValueLayoutType(
                new,
                if (identicalAlignment == TriState.YES) byteAlignment else null
            ) to identical.sharpenTowardsNo(identicalAlignment)
        } else if (other is BotMemoryLayoutType) {
            return this to TriState.UNKNOWN
        }
        return TopMemoryLayoutType to TriState.UNKNOWN
    }
}