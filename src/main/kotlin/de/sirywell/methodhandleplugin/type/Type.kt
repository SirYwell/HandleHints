package de.sirywell.methodhandleplugin.type

import com.intellij.psi.PsiType

sealed interface Type {

    fun join(other: Type): Type
}

data object BotType : Type {
    override fun join(other: Type) = other
}

data object TopType : Type {
    override fun join(other: Type) = this
}

@JvmRecord
data class DirectType(val psiType: PsiType) : Type {
    override fun join(other: Type): Type {
        return when (other) {
            is BotType -> this
            is TopType -> other
            is DirectType -> {
                if (other.psiType == psiType) {
                    return this
                } else {
                    return TopType
                }
            }
        }
    }
}