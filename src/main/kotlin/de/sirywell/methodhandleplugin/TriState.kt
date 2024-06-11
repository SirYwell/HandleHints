package de.sirywell.methodhandleplugin

enum class TriState {
    YES,
    UNKNOWN,
    NO;

    fun join(other: TriState): TriState {
        if (this != other) return UNKNOWN
        return this
    }

    fun sharpenTowardsNo(other: TriState): TriState {
        if (this == NO || other == NO) return NO
        return this.join(other)
    }
}

fun Boolean?.toTriState() = when (this) {
    null -> TriState.UNKNOWN
    true -> TriState.YES
    false -> TriState.NO
}