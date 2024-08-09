package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.toTriState
import org.jetbrains.uast.util.isInstanceOf
import java.util.*

@TypeInfo(TopMemoryLayoutType::class)
sealed interface MemoryLayoutType : TypeLatticeElement<MemoryLayoutType> {
    fun withByteAlignment(byteAlignment: Long): MemoryLayoutType
    fun withName(name: LayoutName): MemoryLayoutType

    val byteAlignment: Long?
    val byteSize: Long?
    val name: LayoutName
}

data object BotMemoryLayoutType : MemoryLayoutType, BotTypeLatticeElement<MemoryLayoutType> {
    override fun withByteAlignment(byteAlignment: Long) = this
    override fun withName(name: LayoutName) = this

    override val byteAlignment = null
    override val byteSize = null
    override val name = BotLayoutName

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object TopMemoryLayoutType : MemoryLayoutType, TopTypeLatticeElement<MemoryLayoutType> {
    override fun self() = this
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override fun withByteAlignment(byteAlignment: Long) = this
    override fun withName(name: LayoutName) = this

    override val byteAlignment = null
    override val byteSize = null
    override val name = TopLayoutName
}

sealed interface ValueLayoutType : MemoryLayoutType

data class AddressLayoutType(
    val targetLayout: MemoryLayoutType?, // null denotes EXPLICITLY no target layout
    override val byteAlignment: Long?,
    override val byteSize: Long?,
    override val name: LayoutName
) : ValueLayoutType {

    constructor(byteAlignment: Long?, byteSize: Long?) : this(null, byteAlignment, byteSize, WITHOUT_NAME)

    override fun withByteAlignment(byteAlignment: Long): MemoryLayoutType {
        return AddressLayoutType(targetLayout, byteAlignment, byteSize, name)
    }

    override fun withName(name: LayoutName): MemoryLayoutType {
        return AddressLayoutType(targetLayout, byteAlignment, byteSize, name)
    }

    fun withTargetLayout(targetLayout: MemoryLayoutType?): AddressLayoutType {
        return AddressLayoutType(targetLayout, byteAlignment, byteSize, name)
    }

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is AddressLayoutType) {
            val (targetLayout, identicalTargetLayout) = other.targetLayout?.let { this.targetLayout?.joinIdentical(it) }
            // if both are null, they are the same in this aspect
                ?: if (other.targetLayout == null && this.targetLayout == null) null to TriState.YES
                // if only one is null, they are definitely not the same
                else TopMemoryLayoutType to TriState.NO
            val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
            val (name, identicalName) = name.joinIdentical(other.name)
            return AddressLayoutType(
                targetLayout,
                if (identicalAlignment == TriState.YES) this.byteAlignment else null,
                if (identicalSize == TriState.YES) this.byteSize else null,
                name
            ) to identicalTargetLayout
                .sharpenTowardsNo(identicalAlignment)
                .sharpenTowardsNo(identicalSize)
                .sharpenTowardsNo(identicalName)
        } else if (other is BotMemoryLayoutType) {
            return this to TriState.UNKNOWN
        }
        return TopMemoryLayoutType to TriState.UNKNOWN

    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }
}

data class NormalValueLayoutType(
    val type: Type,
    override val byteAlignment: Long?,
    override val byteSize: Long?,
    override val name: LayoutName
) : ValueLayoutType {

    constructor(type: Type, byteAlignment: Long?, byteSize: Long?) : this(type, byteAlignment, byteSize, WITHOUT_NAME)

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is NormalValueLayoutType) {
            val (new, identical) = this.type.joinIdentical(other.type)
            val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
            val (name, identicalName) = name.joinIdentical(other.name)
            return NormalValueLayoutType(
                new,
                if (identicalAlignment == TriState.YES) this.byteAlignment else null,
                if (identicalSize == TriState.YES) this.byteSize else null,
                name
            ) to identical
                .sharpenTowardsNo(identicalAlignment)
                .sharpenTowardsNo(identicalSize)
                .sharpenTowardsNo(identicalName)
        } else if (other is BotMemoryLayoutType) {
            return this to TriState.UNKNOWN
        }
        return TopMemoryLayoutType to TriState.UNKNOWN
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override fun withByteAlignment(byteAlignment: Long) = NormalValueLayoutType(type, byteAlignment, byteSize, name)
    override fun withName(name: LayoutName) = NormalValueLayoutType(type, byteAlignment, byteSize, name)
}

sealed interface GroupLayoutType : MemoryLayoutType {
    val memberLayouts: MemoryLayoutList
}

data class StructLayoutType(
    override val memberLayouts: MemoryLayoutList,
    override val byteAlignment: Long?,
    override val byteSize: Long?,
    override val name: LayoutName
) : GroupLayoutType {
    override fun withByteAlignment(byteAlignment: Long) =
        StructLayoutType(this.memberLayouts, byteSize, byteAlignment, name)

    override fun withName(name: LayoutName) = StructLayoutType(memberLayouts, byteAlignment, byteSize, name)

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is BotMemoryLayoutType) return this to TriState.UNKNOWN
        if (other !is StructLayoutType) return TopMemoryLayoutType to TriState.UNKNOWN
        val (members, identical) = this.memberLayouts.joinIdentical(other.memberLayouts)
        val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
        val (name, identicalName) = name.joinIdentical(other.name)
        return StructLayoutType(
            members,
            if (identicalAlignment == TriState.YES) this.byteAlignment else null,
            if (identicalSize == TriState.YES) this.byteSize else null,
            name
        ) to identical
            .sharpenTowardsNo(identicalAlignment)
            .sharpenTowardsNo(identicalSize)
            .sharpenTowardsNo(identicalName)
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data class UnionLayoutType(
    override val memberLayouts: MemoryLayoutList,
    override val byteAlignment: Long?,
    override val byteSize: Long?,
    override val name: LayoutName
) : GroupLayoutType {
    override fun withByteAlignment(byteAlignment: Long): MemoryLayoutType {
        return UnionLayoutType(this.memberLayouts, byteSize, byteAlignment, name)
    }

    override fun withName(name: LayoutName) = UnionLayoutType(memberLayouts, byteAlignment, byteSize, name)

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is BotMemoryLayoutType) return this to TriState.UNKNOWN
        if (other !is UnionLayoutType) return TopMemoryLayoutType to TriState.UNKNOWN
        // TODO it might make sense to ignore order here?
        val (members, identical) = this.memberLayouts.joinIdentical(other.memberLayouts)
        val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
        val (name, identicalName) = name.joinIdentical(other.name)
        return UnionLayoutType(
            members,
            if (identicalAlignment == TriState.YES) this.byteAlignment else null,
            if (identicalSize == TriState.YES) this.byteSize else null,
            name
        ) to identical
            .sharpenTowardsNo(identicalAlignment)
            .sharpenTowardsNo(identicalSize)
            .sharpenTowardsNo(identicalName)
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data class SequenceLayoutType(
    val elementLayout: MemoryLayoutType,
    val elementCount: Long?,
    override val byteAlignment: Long?,
    override val name: LayoutName
) : MemoryLayoutType {
    override fun withByteAlignment(byteAlignment: Long): MemoryLayoutType {
        return SequenceLayoutType(this.elementLayout, this.elementCount, byteAlignment, name)
    }

    override fun withName(name: LayoutName) = SequenceLayoutType(elementLayout, elementCount, byteAlignment, name)

    override val byteSize = elementCount?.let { elementLayout.byteSize?.times(it) }

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is BotMemoryLayoutType) return this to TriState.UNKNOWN
        if (other !is SequenceLayoutType) return TopMemoryLayoutType to TriState.UNKNOWN
        val (element, identical) = this.elementLayout.joinIdentical(other.elementLayout)
        val (identicalAlignment, identicalSize) = joinElementCountAndAlignment(this, other)
        val (name, identicalName) = name.joinIdentical(other.name)
        return SequenceLayoutType(
            element,
            if (identicalSize == TriState.YES) this.byteSize else null,
            if (identicalAlignment == TriState.YES) this.byteAlignment else null,
            name
        ) to identical
            .sharpenTowardsNo(identicalAlignment)
            .sharpenTowardsNo(identicalSize)
            .sharpenTowardsNo(identicalName)
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }

}

data class PaddingLayoutType(
    override val byteAlignment: Long?,
    override val byteSize: Long?,
    override val name: LayoutName
) : MemoryLayoutType {
    override fun withByteAlignment(byteAlignment: Long): MemoryLayoutType {
        return PaddingLayoutType(byteAlignment, byteSize, name)
    }

    override fun withName(name: LayoutName) = PaddingLayoutType(byteAlignment, byteSize, name)

    override fun joinIdentical(other: MemoryLayoutType): Pair<MemoryLayoutType, TriState> {
        if (other is BotMemoryLayoutType) return this to TriState.UNKNOWN
        if (other !is PaddingLayoutType) return TopMemoryLayoutType to TriState.UNKNOWN
        val (identicalAlignment, identicalSize) = joinSizeAndAlignment(this, other)
        val (name, identicalName) = name.joinIdentical(other.name)
        return PaddingLayoutType(
            if (identicalAlignment == TriState.YES) this.byteAlignment else null,
            if (identicalSize == TriState.YES) this.byteSize else null,
            name

        ) to identicalAlignment.sharpenTowardsNo(identicalSize).sharpenTowardsNo(identicalName)
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }

}

private fun joinSizeAndAlignment(first: MemoryLayoutType, second: MemoryLayoutType): Pair<TriState, TriState> {
    val identicalAlignment = (first.byteAlignment?.equals(second.byteAlignment)).toTriState()
    val identicalSize = (first.byteSize?.equals(second.byteSize)).toTriState()
    return identicalAlignment to identicalSize
}

private fun joinElementCountAndAlignment(
    first: SequenceLayoutType,
    second: SequenceLayoutType
): Pair<TriState, TriState> {
    val identicalAlignment = (first.byteAlignment?.equals(second.byteAlignment)).toTriState()
    val identicalElementCount = (first.elementCount?.equals(second.elementCount)).toTriState()
    return identicalAlignment to identicalElementCount
}

typealias MemoryLayoutList = TypeLatticeElementList<MemoryLayoutType>

data object TopMemoryLayoutList : TopTypeLatticeElementList<MemoryLayoutType>() {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)

}

data object BotMemoryLayoutList : BotTypeLatticeElementList<MemoryLayoutType>() {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}

class CompleteMemoryLayoutList(list: List<MemoryLayoutType>) : CompleteTypeLatticeElementList<MemoryLayoutType>(list) {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}

class IncompleteMemoryLayoutList(knowParameterTypes: SortedMap<Int, MemoryLayoutType>) :
    IncompleteTypeLatticeElementList<MemoryLayoutType>(knowParameterTypes) {
    override fun topList() = TopMemoryLayoutList
    override fun botList() = BotMemoryLayoutList
    override fun top() = TopMemoryLayoutType
    override fun bot() = BotMemoryLayoutType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<MemoryLayoutType>) = CompleteMemoryLayoutList(list)
    override fun incomplete(list: SortedMap<Int, MemoryLayoutType>) = IncompleteMemoryLayoutList(list)
}

@TypeInfo(TopLayoutName::class)
sealed interface LayoutName : TypeLatticeElement<LayoutName>

val WITHOUT_NAME = ExactLayoutName(null)

data class ExactLayoutName(val name: String?) : LayoutName {
    override fun joinIdentical(other: LayoutName): Pair<LayoutName, TriState> {
        if (other is ExactLayoutName) {
            if (this.name == other.name) {
                return this to TriState.YES
            }
            return TopLayoutName to TriState.NO
        }
        if (other is TopLayoutName) return TopLayoutName to TriState.UNKNOWN
        // BotLayoutName
        return this to TriState.UNKNOWN
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object TopLayoutName : LayoutName, TopTypeLatticeElement<LayoutName> {
    override fun self() = this

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object BotLayoutName : LayoutName, BotTypeLatticeElement<LayoutName> {

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

@TypeInfo(TopPathElementType::class)
sealed interface PathElementType : TypeLatticeElement<PathElementType>

sealed interface SequenceElementVariant {
    fun isEqualTo(other: SequenceElementVariant): TriState
}

// sequenceElement()
data object OpenSequenceElementVariant : SequenceElementVariant {
    override fun isEqualTo(other: SequenceElementVariant) = (this == other).toTriState()
}

// sequenceElement(index)
data class SelectingSequenceElementVariant(val index: Long?) : SequenceElementVariant {
    override fun isEqualTo(other: SequenceElementVariant): TriState {
        if (other !is SelectingSequenceElementVariant) return TriState.NO
        if (index == null || other.index == null) return TriState.UNKNOWN
        return (index == other.index).toTriState()
    }
}

// sequenceElement(start, step)
data class SelectingOpenSequenceElementVariant(val start: Long?, val step: Long?) : SequenceElementVariant {
    override fun isEqualTo(other: SequenceElementVariant): TriState {
        if (other !is SelectingOpenSequenceElementVariant) return TriState.NO
        if (start == null || step == null || other.start == null || other.step == null) return TriState.UNKNOWN
        return (start == other.start && step == other.step).toTriState()
    }
}

data class SequenceElementType(val variant: SequenceElementVariant) : PathElementType {
    override fun joinIdentical(other: PathElementType): Pair<PathElementType, TriState> {
        return when (other) {
            TopPathElementType -> other to TriState.UNKNOWN
            BotPathElementType -> this to TriState.UNKNOWN
            is SequenceElementType -> {
                if (variant.isEqualTo(other.variant) == TriState.NO) return this to TriState.YES
                return TopPathElementType to TriState.NO
            }

            is GroupElementType,
            DereferenceElementType -> TopPathElementType to TriState.NO
        }
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

sealed interface GroupElementVariant {
    fun isEqualTo(other: GroupElementVariant): TriState
}

// groupElement(index)
data class IndexGroupElementVariant(val index: Long?) : GroupElementVariant {
    override fun isEqualTo(other: GroupElementVariant): TriState {
        if (other !is IndexGroupElementVariant) return TriState.NO
        if (index == null || other.index == null) return TriState.UNKNOWN
        return (index == other.index).toTriState()
    }
}

// groupElement(name)
data class NameGroupElementVariant(val name: String?) : GroupElementVariant {
    override fun isEqualTo(other: GroupElementVariant): TriState {
        if (other !is NameGroupElementVariant) return TriState.NO
        if (name == null || other.name == null) return TriState.UNKNOWN
        return (name == other.name).toTriState()
    }
}

data class GroupElementType(val variant: GroupElementVariant) : PathElementType {
    override fun joinIdentical(other: PathElementType): Pair<PathElementType, TriState> {
        return when (other) {
            TopPathElementType -> other to TriState.UNKNOWN
            BotPathElementType -> this to TriState.UNKNOWN
            is GroupElementType -> {
                if (variant.isEqualTo(other.variant) == TriState.NO) return this to TriState.YES
                return TopPathElementType to TriState.NO
            }

            is SequenceElementType,
            DereferenceElementType -> TopPathElementType to TriState.NO
        }
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object DereferenceElementType : PathElementType {
    override fun joinIdentical(other: PathElementType): Pair<PathElementType, TriState> {
        return when (other) {
            TopPathElementType -> other to TriState.UNKNOWN
            BotPathElementType -> this to TriState.UNKNOWN
            DereferenceElementType -> {
                return this to TriState.YES
            }

            is SequenceElementType,
            is GroupElementType -> TopPathElementType to TriState.NO
        }
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object TopPathElementType : PathElementType, TopTypeLatticeElement<PathElementType> {
    override fun self() = this

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data object BotPathElementType : PathElementType, BotTypeLatticeElement<PathElementType> {
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

typealias PathElementList = TypeLatticeElementList<PathElementType>

data object TopPathElementList : TopTypeLatticeElementList<PathElementType>() {
    override fun topList() = TopPathElementList
    override fun botList() = BotPathElementList
    override fun top() = TopPathElementType
    override fun bot() = BotPathElementType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<PathElementType>) = CompletePathElementList(list)
    override fun incomplete(list: SortedMap<Int, PathElementType>) = IncompletePathElementList(list)

}

data object BotPathElementList : BotTypeLatticeElementList<PathElementType>() {
    override fun topList() = TopPathElementList
    override fun botList() = BotPathElementList
    override fun top() = TopPathElementType
    override fun bot() = BotPathElementType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<PathElementType>) = CompletePathElementList(list)
    override fun incomplete(list: SortedMap<Int, PathElementType>) = IncompletePathElementList(list)
}

class CompletePathElementList(list: List<PathElementType>) : CompleteTypeLatticeElementList<PathElementType>(list) {
    override fun topList() = TopPathElementList
    override fun botList() = BotPathElementList
    override fun top() = TopPathElementType
    override fun bot() = BotPathElementType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<PathElementType>) = CompletePathElementList(list)
    override fun incomplete(list: SortedMap<Int, PathElementType>) = IncompletePathElementList(list)
}

class IncompletePathElementList(knowParameterTypes: SortedMap<Int, PathElementType>) :
    IncompleteTypeLatticeElementList<PathElementType>(knowParameterTypes) {
    override fun topList() = TopPathElementList
    override fun botList() = BotPathElementList
    override fun top() = TopPathElementType
    override fun bot() = BotPathElementType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<PathElementType>) = CompletePathElementList(list)
    override fun incomplete(list: SortedMap<Int, PathElementType>) = IncompletePathElementList(list)
}
