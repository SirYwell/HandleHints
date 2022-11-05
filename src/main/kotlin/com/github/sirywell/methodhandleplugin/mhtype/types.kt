package com.github.sirywell.methodhandleplugin.mhtype

import com.github.sirywell.methodhandleplugin.MHS
import com.github.sirywell.methodhandleplugin.MethodHandleSignature
import com.github.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
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
        return when (other) {
            is MhExactType -> {
                return when (this.signature.relation(other.signature)) {
                    MethodHandleSignature.Relation.SAME -> this
                    else -> Top
                }
            }
            is MhSubType -> TODO()
            is MhSuperType -> TODO()
            is MhIncompatibleType -> MhIncompatibleType(other.signatures + this.signature)
            Top -> Top
            Bot -> this
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
}

class MhSuperType(signature: MethodHandleSignature) : MhSingleType(signature) {
    override fun join(other: MhType): MhType {
        TODO("Not yet implemented")
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        return MhSuperType(signature)
    }
}

class MhIncompatibleType(val signatures: Set<MethodHandleSignature>) : MhType {
    override fun join(other: MhType): MhType {
        return when (other) {
            is MhIncompatibleType -> MhIncompatibleType(this.signatures + other.signatures)
            is MhSingleType -> MhIncompatibleType(this.signatures + other.signature)
            Top -> Top // does that make sense?
            Bot -> this
        }
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        TODO("Not yet implemented")
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
