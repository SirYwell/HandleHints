package com.github.sirywell.methodhandleplugin.mhtype

import com.github.sirywell.methodhandleplugin.MHS
import com.github.sirywell.methodhandleplugin.MethodHandleSignature
import com.github.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import com.github.sirywell.methodhandleplugin.MethodHandleSignature.Relation
import com.intellij.psi.PsiType

/*
         Top
 Super   Exact   Sub
         Bot
 */

sealed interface MhType {
    fun join(other: MhType): MhType
    fun withSignature(signature: MethodHandleSignature): MhType
}

sealed class MhSingleType(val signature: MethodHandleSignature) : MhType {
    override fun toString(): String {
        return signature.toString()
    }
}

class MhExactType(signature: MethodHandleSignature) : MhSingleType(signature) {

    override fun join(other: MhType): MhType {
        if (other is Bot) return this
        if (other is Top) return Top
        if (other !is MhSingleType) throw IllegalArgumentException("Type ${other.javaClass} not supported")
        return when (this.signature.relation(other.signature)) {
            Relation.SAME -> {
                return when (other) {
                    is MhExactType -> this
                    is MhSubType -> other
                    is MhSuperType -> other
                }
            }
            Relation.SUBTYPE_OF -> { // other < this
                return when (other) {
                    is MhExactType -> MhSubType(other.signature) // e.g. more special type required for invoke
                    is MhSubType -> TODO()
                    is MhSuperType -> Bot
                }
            }
            Relation.SUPERTYPE_OF -> { // this < other
                return when (other) {
                    is MhExactType -> MhSubType(this.signature)
                    is MhSubType -> Bot
                    is MhSuperType -> TODO()
                }
            }
            else -> Top
        }
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        return MhExactType(signature)
    }
}

// TODO figure out if we actually want to go that route
class MhSubType(signature: MethodHandleSignature) : MhSingleType(signature) {
    override fun join(other: MhType): MhType {
        TODO("Not yet implemented")
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        return MhSubType(signature)
    }

    override fun toString() = "~" + super.toString()
}

class MhSuperType(signature: MethodHandleSignature) : MhSingleType(signature) {
    override fun join(other: MhType): MhType {
        TODO("Not yet implemented")
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        return MhSuperType(signature)
    }
}

object Top : MhType {
    override fun join(other: MhType) = this
    override fun withSignature(signature: MethodHandleSignature) = this
    override fun toString() = "Top"
}
object Bot : MhType {
    override fun join(other: MhType) = other
    override fun withSignature(signature: MethodHandleSignature) = this
    override fun toString() = "Bottom"
}
fun MHS.withParameters(parameters: List<PsiType>) = create(this.returnType, parameters.toList())

fun MHS.withReturnType(returnType: PsiType) = create(returnType, this.parameters)
