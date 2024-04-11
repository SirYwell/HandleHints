package de.sirywell.methodhandleplugin.type

@JvmRecord
data class MethodHandleType(val signature: Signature) {
    fun join(methodHandleType: MethodHandleType): MethodHandleType {
        return MethodHandleType(signature.join(methodHandleType.signature))
    }
}
