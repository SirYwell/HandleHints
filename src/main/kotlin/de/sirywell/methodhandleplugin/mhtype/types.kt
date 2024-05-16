package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiElement
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.MethodHandleSignature
import de.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import de.sirywell.methodhandleplugin.MethodHandleSignature.Relation
import com.intellij.psi.PsiType
import org.jetbrains.annotations.Nls

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
    val parameters: List<PsiType>
        get() = signature.parameters
    val returnType: PsiType
        get() = signature.returnType

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
                }
            }
            Relation.SUBTYPE_OF -> { // other < this
                return when (other) {
                    is MhExactType -> MhSubType(other.signature) // e.g. more special type required for invoke
                    is MhSubType -> other
                }
            }
            Relation.SUPERTYPE_OF -> { // this < other
                return when (other) {
                    is MhExactType -> MhSubType(this.signature)
                    is MhSubType -> Bot
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
        if (other is Bot) return this
        if (other is Top) return Top
        if (other !is MhSingleType) throw IllegalArgumentException("Type ${other.javaClass} not supported")
        return when (this.signature.relation(other.signature)) {
            Relation.SAME -> this
            Relation.SUBTYPE_OF -> { // other < this
                return when (other) {
                    is MhExactType -> withSignature(other.signature)
                    is MhSubType -> other
                }
            }
            Relation.SUPERTYPE_OF -> { // this < other
                return when (other) {
                    is MhExactType -> this
                    is MhSubType -> Bot
                }
            }
            else -> Top
        }
    }

    override fun withSignature(signature: MethodHandleSignature): MhType {
        return MhSubType(signature)
    }

    override fun toString() = "~" + super.toString()
}

sealed interface TopType : MhType {
    override fun join(other: MhType) = this
    override fun withSignature(signature: MethodHandleSignature) = this
}
data class UnboundTop(val message: String, val target: PsiElement?): TopType {
    override fun toString() = "Top(${message.take(5)})"
}

object Top: TopType {

    override fun toString() = "Top"
    fun inspect(message: @Nls String, target: PsiElement? = null): MhType {
        return UnboundTop(message, target)
    }
}
object Bot : MhType {
    override fun join(other: MhType) = other
    override fun withSignature(signature: MethodHandleSignature) = this

    override fun toString() = "Bottom"
}
fun MHS.withParameters(parameters: List<PsiType>) = create(this.returnType, parameters.toList())

fun MHS.withReturnType(returnType: PsiType) = create(returnType, this.parameters)
