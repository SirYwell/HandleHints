package de.sirywell.methodhandleplugin.type

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import de.sirywell.methodhandleplugin.TriState
import de.sirywell.methodhandleplugin.objectType
import de.sirywell.methodhandleplugin.toTriState

sealed interface Type {

    fun join(other: Type): Type

    fun erase(manager: PsiManager, scope: GlobalSearchScope): Type

    fun canBe(psiType: PsiType) = match(psiType) != TriState.NO

    fun match(psiType: PsiType): TriState

    fun isPrimitive() = false
}

data object BotType : Type {
    override fun join(other: Type) = other
    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun match(psiType: PsiType) = TriState.UNKNOWN

    override fun toString(): String {
        return "⊥"
    }
}

data object TopType : Type {
    override fun join(other: Type) = this
    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun match(psiType: PsiType) = TriState.UNKNOWN

    override fun toString(): String {
        return "⊤"
    }
}

@JvmRecord
data class ExactType(val psiType: PsiType) : Type {

    companion object {
        @JvmStatic
        val voidType = ExactType(PsiTypes.voidType())
        @JvmStatic
        val intType = ExactType(PsiTypes.intType())
    }

    override fun join(other: Type): Type {
        return when (other) {
            is BotType -> this
            is TopType -> other
            is ExactType -> if (other.psiType == psiType) this else TopType

        }
    }

    override fun erase(manager: PsiManager, scope: GlobalSearchScope): Type {
        if (psiType is PsiPrimitiveType) return this
        val objectType = objectType(manager, scope)
        return ExactType(objectType)
    }

    override fun match(psiType: PsiType) = (this.psiType == psiType).toTriState()

    override fun isPrimitive(): Boolean {
        return psiType is PsiPrimitiveType
    }

    override fun toString(): String {
        return psiType.presentableText
    }
}