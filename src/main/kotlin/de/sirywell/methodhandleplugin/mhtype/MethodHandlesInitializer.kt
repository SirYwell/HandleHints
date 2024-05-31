package de.sirywell.methodhandleplugin.mhtype

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.*
import de.sirywell.methodhandleplugin.MethodHandleBundle.message
import de.sirywell.methodhandleplugin.asType
import de.sirywell.methodhandleplugin.dfa.SsaAnalyzer
import de.sirywell.methodhandleplugin.type.*
import de.sirywell.methodhandleplugin.type.TopType
import org.jetbrains.annotations.Nls

/**
 * Contains methods from [java.lang.invoke.MethodHandles] that create
 * new [java.lang.invoke.MethodHandle]s.
 */
class MethodHandlesInitializer(private val ssaAnalyzer: SsaAnalyzer) {

    private val intType = DirectType(PsiTypes.intType())
    private val voidType = DirectType(PsiTypes.voidType())
    private val topType = MethodHandleType(TopSignature)

    fun arrayConstructor(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()

        return MethodHandleType(CompleteSignature(arrayType, listOf(intType)))
    }

    fun arrayElementGetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = if (arrayType !is DirectType) {
            arrayType
        } else {
            DirectType((arrayType.psiType as PsiArrayType).componentType)
        }
        return MethodHandleType(CompleteSignature(componentType, listOf(arrayType, intType)))
    }

    fun arrayElementSetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = if (arrayType !is DirectType) {
            arrayType
        } else {
            DirectType((arrayType.psiType as PsiArrayType).componentType)
        }
        return MethodHandleType(CompleteSignature(voidType, listOf(arrayType, intType, componentType)))
    }

    // arrayElementVarHandle() no VarHandle support

    fun arrayLength(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        return MethodHandleType(CompleteSignature(intType, listOf(arrayType)))
    }

    // byteArray/BufferViewVarHandle() no VarHandle support

    fun constant(typeExpr: PsiExpression, valueExpr: PsiExpression): MethodHandleType {
        val type = typeExpr.asType()
        val valueType = valueExpr.type?.let { DirectType(it) } ?: BotType
        if (type == voidType) {
            return emitProblem(typeExpr, message("problem.creation.arguments.invalid.type", voidType))
        }
        if (!typesAreCompatible(type, valueType, valueExpr)) {
            return emitProblem(valueExpr, message("problem.general.parameters.expected.type", type, valueType))
        }
        return MethodHandleType(CompleteSignature(type, listOf()))
    }

    fun empty(mhType: MethodHandleType): MethodHandleType = mhType

    fun exactInvoker(mhType: MethodHandleType, methodHandleType: PsiType) = invoker(mhType, methodHandleType)

    fun identity(typeExpr: PsiExpression): MethodHandleType {
        val type = typeExpr.asType()
        if (type == voidType) {
            return emitProblem(typeExpr, message("problem.merging.general.typeMustNotBe", voidType))
        }
        return MethodHandleType(CompleteSignature(type, listOf(type)))
    }

    fun invoker(mhType: MethodHandleType, methodHandleType: PsiType): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val signature = mhType.signature
        val list = signature.parameterTypes.toMutableList()
        list.add(0, DirectType(methodHandleType))
        return MethodHandleType(CompleteSignature(signature.returnType(), list))
    }

    fun spreadInvoker(type: MethodHandleType, leadingArgCount: Int, objectType: PsiType): MethodHandleType {
        if (type.signature !is CompleteSignature) return type
        if (leadingArgCount < 0) return topType
        val signature = type.signature
        if (leadingArgCount >= signature.parameterTypes.size) return topType
        val keep = signature.parameterTypes.subList(0, leadingArgCount).toMutableList()
        keep.add(DirectType(objectType.createArrayType()))
        return MethodHandleType(CompleteSignature(signature.returnType(), keep))
    }

    fun throwException(returnTypeExpr: PsiExpression, exTypeExpr: PsiExpression): MethodHandleType {
        return MethodHandleType(CompleteSignature(returnTypeExpr.asType(), listOf(exTypeExpr.asType())))
    }

    fun zero(type: Type): MethodHandleType {
        return MethodHandleType(CompleteSignature(type, listOf()))
    }

    private fun PsiExpression.asArrayType(): Type {
        val referenceClass = this.asType()
        if (referenceClass is DirectType && referenceClass.psiType !is PsiArrayType) {
            emitMustBeArrayType(this, referenceClass)
            return TopType
        }
        return referenceClass
    }

    private fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        ssaAnalyzer.typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return MethodHandleType(TopSignature)
    }


    private fun emitMustBeArrayType(refc: PsiExpression, referenceClass: Type) {
        emitProblem(
            refc, message(
                "problem.merging.general.arrayTypeExpected",
                referenceClass,
            )
        )
    }

    private fun typesAreCompatible(
        left: Type,
        right: Type,
        context: PsiElement
    ): Boolean {
        val l = (left as? DirectType)?.psiType ?: return true // assume compatible if unknown
        var r = (right as? DirectType)?.psiType ?: return true // assume compatible if unknown
        if (r is PsiPrimitiveType) {
            r.getBoxedType(context)?.let { r = it }
        }
        return l.isConvertibleFrom(r)
    }

}
