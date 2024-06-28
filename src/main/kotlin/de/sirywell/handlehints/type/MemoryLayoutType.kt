package de.sirywell.handlehints.type

import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.toTriState
import java.util.*

sealed interface MemoryLayoutType : TypeLatticeElement<MemoryLayoutType> {
    fun withByteAlignment(byteAlignment: Long): MemoryLayoutType

    val byteAlignment: Long?
    val byteSize: Long?
}

data object BotMemoryLayoutType : MemoryLayoutType, BotTypeLatticeElement<MemoryLayoutType> {
    override fun withByteAlignment(byteAlignment: Long) = this

    override val byteAlignment = null
    override val byteSize = null

    override fun toString(): String {
        return "⊥"
    }
}

data object TopMemoryLayoutType : MemoryLayoutType, TopTypeLatticeElement<MemoryLayoutType> {
    override fun self() = this
    override fun withByteAlignment(byteAlignment: Long) = this

    override val byteAlignment = null
    override val byteSize = null

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
            val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
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
        return (if (byteAlignment != null) "$byteAlignment%" else "?") +
                "$type" +
                if (byteSize != null) "$byteSize" else "?"
    }
}

data class StructLayoutType(
    val memberLayouts: MemoryLayoutList,
    override val byteAlignment: Long?,
    override val byteSize: Long?
) : MemoryLayoutType {
    override fun withByteAlignment(byteAlignment: Long): MemoryLayoutType {
        return StructLayoutType(this.memberLayouts, byteSize, byteAlignment)
    }

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is BotMemoryLayoutType) return this to TriState.UNKNOWN
        if (other !is StructLayoutType) return TopMemoryLayoutType to TriState.UNKNOWN
        val (members, identical) = this.memberLayouts.joinIdentical(other.memberLayouts)
        val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
        return StructLayoutType(
            members,
            if (identicalAlignment == TriState.YES) this.byteAlignment else null,
            if (identicalSize == TriState.YES) this.byteSize else null
        ) to identical.sharpenTowardsNo(identicalAlignment).sharpenTowardsNo(identicalSize)
    }

    override fun toString(): String {
        return (if (byteAlignment != null) "$byteAlignment%" else "?") +
                "$memberLayouts" +
                if (byteSize != null) "$byteSize" else "?"
    }

}

private fun joinSizeAndAlignment(first: MemoryLayoutType, second: MemoryLayoutType): Pair<TriState, TriState> {
    val identicalAlignment = (first.byteAlignment?.equals(second.byteAlignment)).toTriState()
    val identicalSize = (first.byteSize?.equals(second.byteSize)).toTriState()
    return identicalAlignment to identicalSize
}

typealias MemoryLayoutList = TypeLatticeElementList<MemoryLayoutType>

data object TopMemoryLayoutList : TopTypeLatticeElementList<MemoryLayoutType>() {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)

}

data object BotMemoryLayoutList : BotTypeLatticeElementList<MemoryLayoutType>() {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}

class CompleteMemoryLayoutList(list: List<MemoryLayoutType>) : CompleteTypeLatticeElementList<MemoryLayoutType>(list) {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}

class IncompleteMemoryLayoutList(knowParameterTypes: SortedMap<Int, MemoryLayoutType>) :
    IncompleteTypeLatticeElementList<MemoryLayoutType>(knowParameterTypes) {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}