package de.sirywell.handlehints.type

@JvmRecord
data class MethodHandleType(val signature: Signature) {
    fun join(methodHandleType: MethodHandleType): MethodHandleType {
        return MethodHandleType(signature.join(methodHandleType.signature))
    }

    override fun toString(): String {
        return signature.toString()
    }
}
