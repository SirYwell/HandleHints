package de.sirywell.handlehints.foreign

import com.intellij.util.containers.headTail
import de.sirywell.handlehints.type.*
import kotlin.reflect.KClass

interface PathTraverser<T> {

    fun traverse(path: List<PathElementType>, layoutType: MemoryLayoutType, coords: MutableList<Type>): T {
        return resolvePath(path.withIndex().toList(), layoutType, coords)
    }

    private tailrec fun resolvePath(
        path: List<IndexedValue<PathElementType>>,
        layoutType: MemoryLayoutType,
        coords: MutableList<Type>
    ): T {
        if (layoutType == BotMemoryLayoutType) return onBottomLayout(path, coords)
        else if (layoutType == TopMemoryLayoutType) return onTopLayout(path, coords)
        if (path.isEmpty()) {
            return onPathEmpty(layoutType, coords)
        }
        val (head, tail) = path.headTail()
        onPathElement(head, layoutType)
        val resolvedLayout = when (head.value) {
            BotPathElementType -> return onBottomPathElement(path, coords, layoutType)
            TopPathElementType -> return onTopPathElement(path, coords, layoutType)
            is SequenceElementType -> sequenceElement(
                layoutType,
                // what is wrong with the Kotlin type system?
                IndexedValue(head.index, head.value as SequenceElementType),
                coords
            )

            is GroupElementType -> groupElement(
                layoutType,
                IndexedValue(head.index, head.value as GroupElementType)
            )

            DereferenceElementType -> dereferenceElement(layoutType, IndexedValue(head.index, DereferenceElementType))
        }
        return resolvePath(tail, resolvedLayout, coords)
    }

    fun dereferenceElement(
        layoutType: MemoryLayoutType,
        head: IndexedValue<DereferenceElementType>
    ): MemoryLayoutType {
        return when (layoutType) {
            BotMemoryLayoutType -> return BotMemoryLayoutType
            TopMemoryLayoutType -> return TopMemoryLayoutType
            is AddressLayoutType -> {
                layoutType.targetLayout ?: // knowingly no target layout present
                return invalidAddressDereference(head)
            }
            is StructLayoutType,
            is UnionLayoutType,
            is PaddingLayoutType,
            is SequenceLayoutType,
            is NormalValueLayoutType -> {
                return pathElementAndLayoutTypeMismatch(head, layoutType::class, DereferenceElementType::class)
            }
        }
    }

    fun invalidAddressDereference(head: IndexedValue<DereferenceElementType>): MemoryLayoutType

    private fun sequenceElement(
        layoutType: MemoryLayoutType,
        head: IndexedValue<SequenceElementType>,
        coords: MutableList<Type>
    ): MemoryLayoutType {
        val inner = when (layoutType) {
            BotMemoryLayoutType -> return BotMemoryLayoutType
            TopMemoryLayoutType -> return TopMemoryLayoutType
            is SequenceLayoutType -> layoutType.elementLayout
            is PaddingLayoutType,
            is StructLayoutType,
            is UnionLayoutType,
            is ValueLayoutType -> {
                return pathElementAndLayoutTypeMismatch(head, layoutType::class, SequenceElementType::class)
            }
        }
        val headVal = head.value
        when (headVal.variant) {
            OpenSequenceElementVariant -> coords.add(ExactType.longType)
            is SelectingOpenSequenceElementVariant -> coords.add(ExactType.longType)
            is SelectingSequenceElementVariant -> {
                headVal.variant.index?.let {
                    val c = layoutType.elementCount ?: Long.MAX_VALUE
                    if (it >= c) {
                        return onSequenceElementIndexOutOfBounds(layoutType, it, head)
                    }
                }
            }
        }
        return inner
    }

    private fun groupElement(
        layoutType: MemoryLayoutType,
        head: IndexedValue<GroupElementType>
    ): MemoryLayoutType {
        return when (layoutType) {
            BotMemoryLayoutType -> return BotMemoryLayoutType
            TopMemoryLayoutType -> return TopMemoryLayoutType
            is StructLayoutType -> findByElementType(head, layoutType.memberLayouts)
            is UnionLayoutType -> findByElementType(head, layoutType.memberLayouts)
            is SequenceLayoutType,
            is PaddingLayoutType,
            is ValueLayoutType -> {
                return pathElementAndLayoutTypeMismatch(head, layoutType::class, GroupElementType::class)
            }
        }
    }

    private fun findByElementType(
        elementType: IndexedValue<GroupElementType>,
        memberLayouts: MemoryLayoutList
    ): MemoryLayoutType {
        return when (elementType.value.variant) {
            is IndexGroupElementVariant -> {
                val index = (elementType.value.variant as IndexGroupElementVariant).index
                if (index == null || index >= Int.MAX_VALUE) TopMemoryLayoutType
                else if (memberLayouts.compareSize((index + 1).toInt()) == PartialOrder.LT) {
                    // known index out of bounds
                    onGroupElementIndexOutOfBounds(elementType, index, memberLayouts)
                } else memberLayouts[index.toInt()]
            }

            is NameGroupElementVariant -> {
                val name = (elementType.value.variant as NameGroupElementVariant).name
                if (name == null) TopMemoryLayoutType
                // we need to be conservative here: multiple memberLayouts can have the same name
                // so we must abort as soon as we find a layout with an unknown name
                else {
                    for (type in memberLayouts.partialList()) {
                        if (type.name !is ExactLayoutName) return TopMemoryLayoutType
                        else if ((type.name as ExactLayoutName).name == name) return type

                    }
                    return onGroupElementNameNotFound(elementType, name)
                }
            }
        }
    }

    fun pathElementAndLayoutTypeMismatch(
        head: IndexedValue<PathElementType>,
        memoryLayoutType: KClass<out MemoryLayoutType>,
        pathElementType: KClass<out PathElementType>
    ): MemoryLayoutType

    fun onGroupElementNameNotFound(elementType: IndexedValue<GroupElementType>, name: String): MemoryLayoutType

    fun onGroupElementIndexOutOfBounds(
        elementType: IndexedValue<GroupElementType>,
        index: Long,
        memberLayouts: MemoryLayoutList
    ): MemoryLayoutType

    fun onSequenceElementIndexOutOfBounds(
        layoutType: SequenceLayoutType,
        index: Long,
        head: IndexedValue<SequenceElementType>
    ): MemoryLayoutType

    fun onComplete(layoutType: ValueLayoutType, coords: MutableList<Type>): T

    fun onPathEmpty(layoutType: MemoryLayoutType, coords: MutableList<Type>): T

    fun onTopPathElement(
        path: List<IndexedValue<PathElementType>>,
        coords: MutableList<Type>,
        layoutType: MemoryLayoutType
    ): T

    fun onBottomPathElement(
        path: List<IndexedValue<PathElementType>>,
        coords: MutableList<Type>,
        layoutType: MemoryLayoutType
    ): T

    fun onTopLayout(path: List<IndexedValue<PathElementType>>, coords: MutableList<Type>): T

    fun onBottomLayout(path: List<IndexedValue<PathElementType>>, coords: MutableList<Type>): T

    fun onPathElement(head: IndexedValue<PathElementType>, layoutType: MemoryLayoutType) {

    }
}
