package de.sirywell.methodhandleplugin.type

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import de.sirywell.methodhandleplugin.TriState
import de.sirywell.methodhandleplugin.objectType
import de.sirywell.methodhandleplugin.toTriState

sealed interface Type {

    fun join(other: Type): Type

    fun erase(manager: PsiManager, scope: GlobalSearchScope): Type

    // TODO this method is questionable, there might be a better approach
    fun canBe(psiType: PsiType): Boolean

    fun match(psiType: PsiType): TriState

    fun isPrimitive() = false
}

data object BotType : Type {
    override fun join(other: Type) = other
    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun canBe(psiType: PsiType) = true
    override fun match(psiType: PsiType) = TriState.UNKNOWN

    override fun toString(): String {
        return "⊥"
    }
}

data object TopType : Type {
    override fun join(other: Type) = this
    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun canBe(psiType: PsiType) = false
    override fun match(psiType: PsiType) = TriState.UNKNOWN

    override fun toString(): String {
        return "⊤"
    }
}

@JvmRecord
data class DirectType(val psiType: PsiType) : Type {
    override fun join(other: Type): Type {
        return when (other) {
            is BotType -> this
            is TopType -> other
            is DirectType -> if (other.psiType == psiType) this else TopType

        }
    }

    override fun erase(manager: PsiManager, scope: GlobalSearchScope): Type {
        if (psiType is PsiPrimitiveType) return this
        val objectType = objectType(manager, scope)
        return DirectType(objectType)
    }

    override fun canBe(psiType: PsiType) = this.psiType == psiType
    override fun match(psiType: PsiType) = (this.psiType == psiType).toTriState()

    override fun isPrimitive(): Boolean {
        return psiType is PsiPrimitiveType
    }

    override fun toString(): String {
        return psiType.presentableText
    }
}