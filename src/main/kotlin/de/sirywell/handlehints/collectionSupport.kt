package de.sirywell.handlehints

fun <T> List<T>.subList(start: Int) = this.subList(start, this.size)

/**
 * https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/lang/invoke/MethodHandles.html#effid
 */
fun <T> List<T>.effectivelyIdenticalTo(other: List<T>): Boolean {
    if (other.size < this.size) return false
    return this == other.subList(0, this.size)
}
