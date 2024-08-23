package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.toTriState
import java.util.*
import java.util.stream.Collectors

sealed interface TypeLatticeElementList<T : TypeLatticeElement<T>> : TypeLatticeElement<TypeLatticeElementList<T>> {
    /**
     * Returns [TopType] if the index is out of known bounds.
     * Returns [BotType] if no better type is known for that index.
     */
    fun parameterType(index: Int): T
    operator fun get(index: Int) = parameterType(index)

    fun hasSize(size: Int): TriState {
        return when (compareSize(size)) {
            PartialOrder.LT,
            PartialOrder.GT -> TriState.NO
            PartialOrder.EQ -> TriState.YES
            PartialOrder.UNORDERED -> TriState.UNKNOWN
        }
    }

    fun dropFirst(n: Int): TypeLatticeElementList<T>

    fun removeAt(index: Int): TypeLatticeElementList<T> = removeAt(index, 1)

    fun removeAt(index: Int, n: Int): TypeLatticeElementList<T>

    fun addAllAt(index: Int, typeLatticeElementList: TypeLatticeElementList<T>): TypeLatticeElementList<T>

    fun setAt(index: Int, type: T): TypeLatticeElementList<T>

    operator fun set(index: Int, type: T) = setAt(index, type)

    fun sizeMatches(predicate: (Int) -> Boolean): TriState

    fun compareSize(value: Int): PartialOrder

    fun sizeOrNull(): Int?

    fun anyKnownMatches(predicate: (T) -> Boolean): Boolean

    /**
     * @return a list that contains all known elements, and unknown elements below the maximum known index
     * represented by [top]
     */
    fun partialList(): List<T>

    fun topList(): TopTypeLatticeElementList<T>
    fun botList(): BotTypeLatticeElementList<T>
    fun top(): T
    fun bot(): T
    fun complete(list: List<T>): CompleteTypeLatticeElementList<T>
    fun incomplete(list: SortedMap<Int, T>): IncompleteTypeLatticeElementList<T>
    fun lastOrNull(): T?

}

abstract class BotTypeLatticeElementList<T : TypeLatticeElement<T>> : TypeLatticeElementList<T> {
    override fun parameterType(index: Int) = bot()
    override fun joinIdentical(other: TypeLatticeElementList<T>) = other to TriState.UNKNOWN
    override fun dropFirst(n: Int) = this
    override fun removeAt(index: Int, n: Int) = this
    override fun addAllAt(index: Int, typeLatticeElementList: TypeLatticeElementList<T>): TypeLatticeElementList<T> {
        return incomplete(typeLatticeElementList.toMap().mapKeys { it.key + index }.toSortedMap())
    }

    override fun setAt(index: Int, type: T): TypeLatticeElementList<T> {
        if (index < 0) return topList()
        return incomplete(sortedMapOf(index to type))
    }

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun anyKnownMatches(predicate: (T) -> Boolean) = false

    override fun partialList(): List<T> = listOf()

    override fun lastOrNull() = null
}

abstract class TopTypeLatticeElementList<T : TypeLatticeElement<T>> : TypeLatticeElementList<T> {
    override fun parameterType(index: Int) = top()
    override fun joinIdentical(other: TypeLatticeElementList<T>) = this to TriState.UNKNOWN
    override fun dropFirst(n: Int) = this
    override fun removeAt(index: Int, n: Int) = this
    override fun addAllAt(index: Int, typeLatticeElementList: TypeLatticeElementList<T>) = this
    override fun setAt(index: Int, type: T) = topList()

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun anyKnownMatches(predicate: (T) -> Boolean) = false

    override fun partialList(): List<T> = listOf()

    override fun lastOrNull() = null
}

abstract class CompleteTypeLatticeElementList<T : TypeLatticeElement<T>>(val typeList: List<T>) : TypeLatticeElementList<T> {
    val size get() = typeList.size
    override fun parameterType(index: Int): T {
        return typeList.getOrElse(index) { top() }
    }

    override fun joinIdentical(other: TypeLatticeElementList<T>): Pair<TypeLatticeElementList<T>, TriState> {
        return when (other) {
            is BotTypeLatticeElementList -> this to TriState.UNKNOWN
            is TopTypeLatticeElementList -> topList() to TriState.UNKNOWN
            is CompleteTypeLatticeElementList -> {
                if (size != other.size) {
                    topList() to TriState.NO
                } else {
                    val params = typeList.zip(other.typeList).map { (a, b) -> a.joinIdentical(b) }
                    val identical = paramsAreIdentical(params)
                    complete(params.map { it.first }) to identical
                }
            }

            is IncompleteTypeLatticeElementList -> {
                if (size < other.knownTypes.lastKey()) {
                    // this list is definitely smaller than the other list, therefore incompatible
                    topList() to TriState.NO
                } else {
                    // join the known types, keep the rest (others would be BotType anyway)
                    val params = typeList.map { it to TriState.UNKNOWN }.toMutableList()
                    other.knownTypes.forEach { (index, type) ->
                        params[index] = typeList[index].joinIdentical(type)
                    }
                    val identical = paramsAreIdentical(params)
                    complete(params.map { it.first }) to identical
                }
            }
        }
    }

    override fun dropFirst(n: Int): TypeLatticeElementList<T> {
        if (typeList.size < n) return topList()
        return complete(typeList.drop(n))
    }

    override fun removeAt(index: Int, n: Int): TypeLatticeElementList<T> {
        if (index < 0 || index + n - 1 > size) return topList()
        val list = typeList.toMutableList()
        list.subList(index, index + n).clear()
        return complete(list)
    }

    override fun addAllAt(index: Int, typeLatticeElementList: TypeLatticeElementList<T>): TypeLatticeElementList<T> {
        if (index > size) return topList()
        return when (typeLatticeElementList) {
            is BotTypeLatticeElementList<T> -> incomplete(typeList.subList(0, index).toIndexedMap())
            is TopTypeLatticeElementList<T> -> topList()
            is CompleteTypeLatticeElementList<T> -> {
                val list = typeList.toMutableList()
                list.addAll(index, typeLatticeElementList.typeList)
                complete(list)
            }

            is IncompleteTypeLatticeElementList<T> -> {
                val map = typeList.toIndexedMap().subMap(0, index)
                val append = typeLatticeElementList.knownTypes.mapKeys { it.key + index }
                incomplete((map + append).toSortedMap())
            }
        }
    }

    override fun setAt(index: Int, type: T): TypeLatticeElementList<T> {
        if (index < 0 || index > typeList.size) return topList()
        val list = typeList.toMutableList()
        list[index] = type
        return complete(list)
    }

    override fun sizeMatches(predicate: (Int) -> Boolean): TriState {
        return predicate(size).toTriState()
    }

    override fun compareSize(value: Int) = size.compareTo(value).order()

    override fun sizeOrNull() = size

    override fun anyKnownMatches(predicate: (T) -> Boolean) = typeList.any(predicate)

    override fun partialList() = typeList

    override fun lastOrNull(): T? {
        return typeList.lastOrNull()
    }
}

abstract class IncompleteTypeLatticeElementList<T : TypeLatticeElement<T>>(val knownTypes: SortedMap<Int, T>) : TypeLatticeElementList<T> {
    override fun parameterType(index: Int): T {
        return knownTypes[index] ?: top()
    }

    override fun joinIdentical(other: TypeLatticeElementList<T>): Pair<TypeLatticeElementList<T>, TriState> {
        return when (other) {
            is BotTypeLatticeElementList -> this to TriState.UNKNOWN
            is TopTypeLatticeElementList -> topList() to TriState.UNKNOWN
            is CompleteTypeLatticeElementList -> other.joinIdentical(this) // do not reimplement code here for no reason
            is IncompleteTypeLatticeElementList -> {
                val new = (knownTypes.entries + (other.knownTypes.entries))
                    .stream()
                    .collect(Collectors.toMap({ it.key }, { it.value to TriState.UNKNOWN },
                        { (type, _), (o, _) -> type.joinIdentical(o) })
                    )
                val identical = if (paramsAreIdentical(new.values) == TriState.NO) {
                    TriState.NO
                } else {
                    TriState.YES
                }
                incomplete(new.mapValues { it.value.first }.toSortedMap()) to identical
            }
        }
    }

    override fun dropFirst(n: Int): TypeLatticeElementList<T> {
        val mutableMap = knownTypes.toMutableMap()
        (0..<n).forEach { mutableMap.remove(it) }
        return incomplete(mutableMap.mapKeys { it.key - n }.toSortedMap())
    }

    override fun removeAt(index: Int, n: Int): TypeLatticeElementList<T> {
        val mutableMap = knownTypes.toMutableMap()
        (0..<n).forEach { mutableMap.remove(index + it) }
        return incomplete(mutableMap.mapKeys { if (it.key >= index + n) it.key - n else it.key }.toSortedMap())
    }

    override fun addAllAt(index: Int, typeLatticeElementList: TypeLatticeElementList<T>): TypeLatticeElementList<T> {
        return when (typeLatticeElementList) {
            is BotTypeLatticeElementList -> incomplete(knownTypes.subMap(0, index))
            is TopTypeLatticeElementList -> topList()
            is CompleteTypeLatticeElementList -> {
                val parameterTypes = typeLatticeElementList.typeList
                val moved = parameterTypes.toIndexedMap().mapKeys { it.key + index }
                val shifted =
                    knownTypes.mapKeys { if (it.key >= index) it.key + parameterTypes.size else it.key }
                incomplete((moved + shifted).toSortedMap())
            }

            is IncompleteTypeLatticeElementList -> {
                val map = knownTypes.subMap(0, index)
                val append = typeLatticeElementList.knownTypes.mapKeys { it.key + index }
                incomplete((map + append).toSortedMap())
            }
        }

    }

    override fun setAt(index: Int, type: T): TypeLatticeElementList<T> {
        if (index < 0) return topList()
        val map = knownTypes.toMutableMap()
        map[index] = type
        return incomplete(map.toSortedMap())
    }

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else if (value < knownTypes.lastKey()) PartialOrder.GT // at least one parameter is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun anyKnownMatches(predicate: (T) -> Boolean) = knownTypes.values.any(predicate)

    override fun partialList(): List<T> {
        return (0..knownTypes.lastKey()).map { knownTypes.getOrDefault(it, top()) }
    }

    override fun lastOrNull() = null
}

private fun <T : TypeLatticeElement<T>> TypeLatticeElementList<T>.toMap(): SortedMap<Int, T> {
    return when (this) {
        is BotTypeLatticeElementList -> emptySortedMap()
        is TopTypeLatticeElementList -> emptySortedMap()
        is CompleteTypeLatticeElementList -> typeList.toIndexedMap()
        is IncompleteTypeLatticeElementList -> knownTypes
    }
}

private fun <T> paramsAreIdentical(params: Iterable<Pair<T, TriState>>) =
    if (params.any { it.second == TriState.NO }) {
        TriState.NO
    } else if (params.all { it.second == TriState.YES }) {
        TriState.YES
    } else {
        TriState.UNKNOWN
    }


fun <E> List<E>.toIndexedMap() = this.mapIndexed { index, e -> index to e }.toMap().toSortedMap()
private fun <K, V> emptySortedMap() = Collections.emptySortedMap<K, V>()