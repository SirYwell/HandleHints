package de.sirywell.handlehints.type

import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.toTriState

sealed interface MemoryLayoutType : TypeLatticeElement<MemoryLayoutType> {
    fun withByteAlignment(byteAlignment: Long): MemoryLayoutType

    val byteSize: Long?
    val byteAlignment: Long?
}

data object BotMemoryLayoutType : MemoryLayoutType, BotTypeLatticeElement<MemoryLayoutType> {
    override fun withByteAlignment(byteAlignment: Long) = this

    override val byteSize = null
    override val byteAlignment = null

    override fun toString(): String {
        return "⊥"
    }
}

data object TopMemoryLayoutType : MemoryLayoutType, TopTypeLatticeElement<MemoryLayoutType> {
    override fun self() = this
    override fun withByteAlignment(byteAlignment: Long) = this

    override val byteSize = null
    override val byteAlignment = null

    override fun toString(): String {
        return "⊤"
    }
}

val ADDRESS_TYPE = ExactType(PsiTypes.nullType())

data class ValueLayoutType(
    val type: Type,
    override val byteAlignment: Long?,
    override val byteSize: Long?
) : MemoryLayoutType {
    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is ValueLayoutType) {
            val (new, identical) = this.type.joinIdentical(other.type)
            val identicalAlignment = (this.byteAlignment?.equals(other.byteAlignment)).toTriState()
            val identicalSize = (this.byteSize?.equals(other.byteSize)).toTriState()
            return ValueLayoutType(
                new,
                if (identicalAlignment == TriState.YES) this.byteAlignment else null,
                if (identicalSize == TriState.YES) this.byteSize else null
            ) to identical.sharpenTowardsNo(identicalAlignment).sharpenTowardsNo(identicalSize)
        } else if (other is BotMemoryLayoutType) {
            return this to TriState.UNKNOWN
        }
        return TopMemoryLayoutType to TriState.UNKNOWN
    }

    override fun withByteAlignment(byteAlignment: Long) = ValueLayoutType(type, byteAlignment, byteSize)

    override fun toString(): String {
        return (if (byteAlignment != null) "$byteAlignment%" else "") +
                "$type" +
                if (byteSize != null) "$byteSize" else ""
    }
}