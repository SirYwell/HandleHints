package de.sirywell.methodhandleplugin.type

sealed interface Signature {
    fun join(signature: Signature): Signature
}

data object BotSignature : Signature {
    override fun join(signature: Signature) = signature
}

data object TopSignature : Signature {
    override fun join(signature: Signature) = this
}

@JvmRecord
data class CompleteSignature(
    val returnType: Type,
    val parameterTypes: List<Type>
) : Signature {
    override fun join(signature: Signature): Signature {
        if (signature is TopSignature) return signature
        if (signature is BotSignature) return this
        val other = signature as CompleteSignature
        if (other.parameterTypes.size != this.parameterTypes.size) return TopSignature
        return CompleteSignature(
            returnType.join(signature.returnType),
            signature.parameterTypes.zip(parameterTypes).map { (a, b) -> a.join(b) }
        )
    }
}
