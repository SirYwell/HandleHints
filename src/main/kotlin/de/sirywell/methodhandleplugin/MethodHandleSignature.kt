package de.sirywell.methodhandleplugin

import com.intellij.psi.PsiType

typealias MHS = MethodHandleSignature


@Suppress("DataClassPrivateConstructor") // why on earth... maybe just don't use a data class here?
data class MethodHandleSignature private constructor(
    val returnType: PsiType,
    val parameters: List<PsiType>
) {
    companion object {
        fun create(returnType: PsiType, parameters: List<PsiType>) = MHS(returnType, parameters.toList())
    }

    fun relation(other: MethodHandleSignature): Relation {
        val returnRelation = this.returnType.relation(other.returnType)
        // already "inverted" for proper subtyping
        val parametersRelation =
            if (this.parameters.size == other.parameters.size)
                this.parameters.relation(other.parameters)
            else Relation.UNRELATED
        if (returnRelation == parametersRelation) {
            return returnRelation
        }
        if (returnRelation == Relation.SAME) {
            return parametersRelation
        }
        if (parametersRelation == Relation.SAME) {
            return if (this.parameters.isEmpty()) returnRelation else parametersRelation
        }
        if (returnRelation == Relation.UNRELATED || parametersRelation == Relation.UNRELATED) {
            return Relation.UNRELATED
        }
        return Relation.UNRELATED
    }

    override fun toString(): String {
        return "(${parameters.joinToString(",") { it.presentableText }})${returnType.presentableText}"
    }

    private fun PsiType.isSubtypeOf(other: PsiType) = other.isAssignableFrom(this)

    private fun PsiType.relation(other: PsiType): Relation {
        if (this == other) {
            return Relation.SAME
        } else if (this.isSubtypeOf(other)) {
            return Relation.SUBTYPE_OF
        } else if (other.isSubtypeOf(this)) {
            return Relation.SUPERTYPE_OF
        }
        return Relation.UNRELATED
    }

    private fun List<PsiType>.relation(other: List<PsiType>): Relation {
        assert(this.size == other.size) { "Parameter lists must have equal length" }
        // SAME         if for this = (a0, ..., aN), other = (b0, ..., bN): ai == bi
        // SUBTYPE_OF   if for this = (a0, ..., aN), other = (b0, ..., bN): ai >= bi
        // SUPERTYPE_OF if for this = (a0, ..., aN), other = (b0, ..., bN): ai <= bi
        // UNRELATED    otherwise
        var current = Relation.SAME
        for ((b, a) in other.zip(this)) {
            if (b == a) continue
            if (b.isSubtypeOf(a)) {
                if (current != Relation.SUPERTYPE_OF) {
                    current = Relation.SUBTYPE_OF
                    continue
                }
            } else if (a.isSubtypeOf(b)) {
                if (current != Relation.SUBTYPE_OF) {
                    current = Relation.SUPERTYPE_OF
                    continue
                }
            }
            // either 'a' is not related to 'b' or we have both supertypes and subtypes
            return Relation.UNRELATED
        }
        return current
    }

    enum class Relation {
        UNRELATED,
        SAME,
        SUBTYPE_OF,
        SUPERTYPE_OF
    }
}
