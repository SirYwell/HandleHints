package de.sirywell.handlehints.type

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.objectType
import de.sirywell.handlehints.toTriState
import java.util.*

@TypeInfo(TopType::class)
sealed interface Type : TypeLatticeElement<Type> {

    fun erase(manager: PsiManager, scope: GlobalSearchScope): Type

    fun canBe(psiType: PsiType) = match(psiType) != TriState.NO

    fun match(psiType: PsiType): TriState

    fun isPrimitive() = false
}

data object BotType : Type, BotTypeLatticeElement<Type> {
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun match(psiType: PsiType) = TriState.UNKNOWN
}

data object TopType : Type, TopTypeLatticeElement<Type> {
    override fun self() = this

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

    override fun erase(manager: PsiManager, scope: GlobalSearchScope) = this
    override fun match(psiType: PsiType) = TriState.UNKNOWN
}

@JvmRecord
data class ExactType(val psiType: PsiType) : Type {

    companion object {
        @JvmStatic
        val voidType = ExactType(PsiTypes.voidType())
        @JvmStatic
        val intType = ExactType(PsiTypes.intType())
        @JvmStatic
        val booleanType = ExactType(PsiTypes.booleanType())
        @JvmStatic
        val byteType = ExactType(PsiTypes.byteType())
        @JvmStatic
        val charType = ExactType(PsiTypes.charType())
        @JvmStatic
        val doubleType = ExactType(PsiTypes.doubleType())
        @JvmStatic
        val floatType = ExactType(PsiTypes.floatType())
        @JvmStatic
        val longType = ExactType(PsiTypes.longType())
        @JvmStatic
        val shortType = ExactType(PsiTypes.shortType())
    }

    override fun joinIdentical(other: Type): Pair<Type, TriState> {
        return when (other) {
            is ExactType -> if (other.psiType == psiType) {
                this to TriState.YES
            } else {
                TopType to TriState.NO
            }
            is BotType -> this to TriState.UNKNOWN
            is TopType -> other to TriState.UNKNOWN
        }
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)

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

typealias TypeList = TypeLatticeElementList<Type>
data object TopTypeList : TopTypeLatticeElementList<Type>() {
    override fun topList() = TopTypeList
    override fun botList() = BotTypeList
    override fun top() = TopType
    override fun bot() = BotType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<Type>) = CompleteTypeList(list)
    override fun incomplete(list: SortedMap<Int, Type>) = IncompleteTypeList(list)

}
data object BotTypeList : BotTypeLatticeElementList<Type>() {
    override fun topList() = TopTypeList
    override fun botList() = BotTypeList
    override fun top() = TopType
    override fun bot() = BotType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<Type>) = CompleteTypeList(list)
    override fun incomplete(list: SortedMap<Int, Type>) = IncompleteTypeList(list)
}

class CompleteTypeList(list: List<Type>) : CompleteTypeLatticeElementList<Type>(list) {
    override fun topList() = TopTypeList
    override fun botList() = BotTypeList
    override fun top() = TopType
    override fun bot() = BotType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<Type>) = CompleteTypeList(list)
    override fun incomplete(list: SortedMap<Int, Type>) = IncompleteTypeList(list)
}

class IncompleteTypeList(knowParameterTypes: SortedMap<Int, Type>) : IncompleteTypeLatticeElementList<Type>(knowParameterTypes) {
    override fun topList() = TopTypeList
    override fun botList() = BotTypeList
    override fun top() = TopType
    override fun bot() = BotType
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
    override fun complete(list: List<Type>) = CompleteTypeList(list)
    override fun incomplete(list: SortedMap<Int, Type>) = IncompleteTypeList(list)
}