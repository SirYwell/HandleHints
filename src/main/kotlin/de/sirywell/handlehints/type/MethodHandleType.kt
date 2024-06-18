package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

@JvmRecord
data class MethodHandleType(val signature: Signature) : TypeLatticeElement<MethodHandleType> {

    override fun joinIdentical(other: MethodHandleType): Pair<MethodHandleType, TriState> {
        val (sig, identical) = signature.joinIdentical(other.signature)
        return MethodHandleType(sig) to identical
    }

    override fun toString(): String {
        return signature.toString()
    }
}
