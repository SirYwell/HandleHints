package de.sirywell.methodhandleplugin

enum class TriState {
    YES,
    UNKNOWN,
    NO
}

fun Boolean?.toTriState() = when (this) {
    null -> TriState.UNKNOWN
    true -> TriState.YES
    false -> TriState.NO
}