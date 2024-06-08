package de.sirywell.methodhandleplugin.type

enum class PartialOrder {
    LT,
    EQ,
    GT,
    UNORDERED
}

fun Int.order() = if (this == 0) PartialOrder.EQ
else if (this < 0) PartialOrder.LT
else PartialOrder.GT