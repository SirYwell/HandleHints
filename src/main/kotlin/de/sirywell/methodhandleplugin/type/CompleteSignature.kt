package de.sirywell.methodhandleplugin.type

sealed interface Signature {

    fun join(signature: Signature): Signature

    fun withReturnType(returnType: Type): Signature

    fun withParameterTypes(parameterTypes: List<Type>): Signature

    fun returnType(): Type
}

data object BotSignature : Signature {
    override fun join(signature: Signature) = signature

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: List<Type>) = this

    override fun returnType() = BotType
}

data object TopSignature : Signature {
    override fun join(signature: Signature) = this

    override fun withReturnType(returnType: Type) = this

    override fun withParameterTypes(parameterTypes: List<Type>) = this

    override fun returnType() = TopType
}

@JvmRecord
data class CompleteSignature(
    @get:JvmName("rType") val returnType: Type,
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

    override fun withReturnType(returnType: Type): Signature {
        return CompleteSignature(returnType, parameterTypes)
    }

    override fun withParameterTypes(parameterTypes: List<Type>): Signature {
        return CompleteSignature(returnType, parameterTypes)
    }

    override fun returnType(): Type {
        return this.returnType
    }

    override fun toString(): String {
        return parameterTypes.joinToString(separator = ",", prefix = "(", postfix = ")") + returnType
    }

}
