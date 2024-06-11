package de.sirywell.methodhandleplugin.type

import de.sirywell.methodhandleplugin.TriState
import de.sirywell.methodhandleplugin.toTriState
import java.util.*
import java.util.stream.Collectors

sealed interface ParameterList {
    /**
     * Returns [TopType] if the index is out of known bounds.
     * Returns [BotType] if no better type is known for that index.
     */
    fun parameterType(index: Int): Type
    operator fun get(index: Int) = parameterType(index)

    fun join(parameterList: ParameterList): ParameterList

    fun hasSize(size: Int): TriState {
        return when (compareSize(size)) {
            PartialOrder.LT,
            PartialOrder.GT -> TriState.NO
            PartialOrder.EQ -> TriState.YES
            PartialOrder.UNORDERED -> TriState.UNKNOWN
        }
    }

    fun dropFirst(n: Int): ParameterList

    fun removeAt(index: Int): ParameterList = removeAt(index, 1)

    fun removeAt(index: Int, n: Int): ParameterList

    fun addAllAt(index: Int, parameterList: ParameterList): ParameterList

    fun setAt(index: Int, type: Type): ParameterList

    operator fun set(index: Int, type: Type) = setAt(index, type)

    fun sizeMatches(predicate: (Int) -> Boolean): TriState

    fun compareSize(value: Int): PartialOrder

    fun sizeOrNull(): Int?

}

data object BotParameterList : ParameterList {
    override fun parameterType(index: Int) = BotType
    override fun join(parameterList: ParameterList) = parameterList
    override fun dropFirst(n: Int) = this
    override fun removeAt(index: Int, n: Int) = this
    override fun addAllAt(index: Int, parameterList: ParameterList): ParameterList {
        return IncompleteParameterList(parameterList.toMap().mapKeys { it.key + index }.toSortedMap())
    }

    override fun setAt(index: Int, type: Type): ParameterList {
        if (index < 0) return TopParameterList
        return IncompleteParameterList(sortedMapOf(index to type))
    }

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun toString() = "[⊥]"
}

data object TopParameterList : ParameterList {
    override fun parameterType(index: Int) = TopType
    override fun join(parameterList: ParameterList) = this
    override fun dropFirst(n: Int) = this
    override fun removeAt(index: Int, n: Int) = this
    override fun addAllAt(index: Int, parameterList: ParameterList) = this
    override fun setAt(index: Int, type: Type) = TopParameterList

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun toString() = "[⊤]"
}

data class CompleteParameterList(val parameterTypes: List<Type>) : ParameterList {
    val size get() = parameterTypes.size
    override fun parameterType(index: Int): Type {
        return parameterTypes.getOrElse(index) { TopType }
    }

    override fun join(parameterList: ParameterList): ParameterList {
        return when (parameterList) {
            BotParameterList -> this
            TopParameterList -> TopParameterList
            is CompleteParameterList -> {
                if (size != parameterList.size) {
                    TopParameterList
                } else {
                    CompleteParameterList(parameterTypes.zip(parameterList.parameterTypes).map { (a, b) -> a.join(b) })
                }
            }

            is IncompleteParameterList -> {
                if (size < parameterList.knownParameterTypes.lastKey()) {
                    // this list is definitely smaller than the other list, therefore incompatible
                    TopParameterList
                } else {
                    // join the known types, keep the rest (others would be BotType anyway)
                    val params = parameterTypes.toMutableList()
                    parameterList.knownParameterTypes.forEach { (index, type) ->
                        params[index] = params[index].join(type)
                    }
                    CompleteParameterList(params)
                }
            }
        }
    }

    override fun dropFirst(n: Int): ParameterList {
        if (parameterTypes.size < n) return TopParameterList
        return CompleteParameterList(parameterTypes.drop(n))
    }

    override fun removeAt(index: Int, n: Int): ParameterList {
        if (index < 0 || index + n - 1 > size) return TopParameterList
        val list = parameterTypes.toMutableList()
        list.subList(index, index + n).clear()
        return CompleteParameterList(list)
    }

    override fun addAllAt(index: Int, parameterList: ParameterList): ParameterList {
        if (index > size) return TopParameterList
        return when (parameterList) {
            BotParameterList -> IncompleteParameterList(parameterTypes.subList(0, index).toIndexedMap())
            TopParameterList -> TopParameterList
            is CompleteParameterList -> {
                val list = parameterTypes.toMutableList()
                list.addAll(index, parameterList.parameterTypes)
                CompleteParameterList(list)
            }

            is IncompleteParameterList -> {
                val map = parameterTypes.toIndexedMap().subMap(0, index)
                val append = parameterList.knownParameterTypes.mapKeys { it.key + index }
                IncompleteParameterList((map + append).toSortedMap())
            }
        }
    }

    override fun setAt(index: Int, type: Type): ParameterList {
        if (index < 0 || index > parameterTypes.size) return TopParameterList
        val list = parameterTypes.toMutableList()
        list[index] = type
        return CompleteParameterList(list)
    }

    override fun sizeMatches(predicate: (Int) -> Boolean): TriState {
        return predicate(size).toTriState()
    }

    override fun compareSize(value: Int) = size.compareTo(value).order()

    override fun sizeOrNull() = size

    override fun toString(): String {
        return parameterTypes.joinToString(separator = ",", prefix = "(", postfix = ")")
    }
}

data class IncompleteParameterList(val knownParameterTypes: SortedMap<Int, Type>) : ParameterList {
    override fun parameterType(index: Int): Type {
        return knownParameterTypes[index] ?: if (index < 0) TopType else BotType
    }

    override fun join(parameterList: ParameterList): ParameterList {
        return when (parameterList) {
            BotParameterList -> this
            TopParameterList -> TopParameterList
            is CompleteParameterList -> parameterList.join(this) // do not reimplement code here for no reason
            is IncompleteParameterList -> {
                val new = (knownParameterTypes.entries + (parameterList.knownParameterTypes.entries))
                    .stream()
                    .collect(Collectors.toMap({ it.key }, { it.value }, Type::join))
                IncompleteParameterList(new.toSortedMap())
            }
        }
    }

    override fun dropFirst(n: Int): ParameterList {
        val mutableMap = knownParameterTypes.toMutableMap()
        (0..<n).forEach { mutableMap.remove(it) }
        return IncompleteParameterList(mutableMap.mapKeys { it.key - n }.toSortedMap())
    }

    override fun removeAt(index: Int, n: Int): ParameterList {
        val mutableMap = knownParameterTypes.toMutableMap()
        (0..<n).forEach { mutableMap.remove(index + it) }
        return IncompleteParameterList(mutableMap.mapKeys { if (it.key >= index + n) it.key - n else it.key }
            .toSortedMap())
    }

    override fun addAllAt(index: Int, parameterList: ParameterList): ParameterList {
        return when (parameterList) {
            BotParameterList -> IncompleteParameterList(knownParameterTypes.subMap(0, index))
            TopParameterList -> TopParameterList
            is CompleteParameterList -> {
                val parameterTypes = parameterList.parameterTypes
                val moved = parameterTypes.toIndexedMap().mapKeys { it.key + index }
                val shifted =
                    knownParameterTypes.mapKeys { if (it.key >= index) it.key + parameterTypes.size else it.key }
                IncompleteParameterList((moved + shifted).toSortedMap())
            }

            is IncompleteParameterList -> {
                val map = knownParameterTypes.subMap(0, index)
                val append = parameterList.knownParameterTypes.mapKeys { it.key + index }
                IncompleteParameterList((map + append).toSortedMap())
            }
        }

    }

    override fun setAt(index: Int, type: Type): ParameterList {
        if (index < 0) return TopParameterList
        val map = knownParameterTypes.toMutableMap()
        map[index] = type
        return IncompleteParameterList(map.toSortedMap())
    }

    override fun sizeMatches(predicate: (Int) -> Boolean) = TriState.UNKNOWN
    override fun compareSize(value: Int): PartialOrder {
        return if (value < 0) PartialOrder.GT // if value is negative, this is definitely greater
        else if (value < knownParameterTypes.lastKey()) PartialOrder.GT // at least one parameter is definitely greater
        else PartialOrder.UNORDERED
    }

    override fun sizeOrNull() = null

    override fun toString(): String {
        return "(" +
                (0..knownParameterTypes.lastKey()).map { parameterType(it) }.joinToString(separator = ",") +
                ",???)"
    }
}

private fun ParameterList.toMap(): SortedMap<Int, Type> {
    return when (this) {
        BotParameterList -> emptySortedMap()
        TopParameterList -> emptySortedMap()
        is CompleteParameterList -> parameterTypes.toIndexedMap()
        is IncompleteParameterList -> knownParameterTypes
    }
}

private fun <E> List<E>.toIndexedMap() = this.mapIndexed { index, e -> index to e }.toMap().toSortedMap()
private fun <K, V> emptySortedMap() = Collections.emptySortedMap<K, V>()