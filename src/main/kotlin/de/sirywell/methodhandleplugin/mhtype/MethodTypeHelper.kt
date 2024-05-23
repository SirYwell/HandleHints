package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.asType
import de.sirywell.methodhandleplugin.getConstantOfType
import de.sirywell.methodhandleplugin.mapToTypes
import de.sirywell.methodhandleplugin.type.*
import java.util.Collections.nCopies

object MethodTypeHelper {

    fun appendParameterTypes(mhType: MethodHandleType, ptypesToInsert: List<PsiExpression>): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val additionalTypes = ptypesToInsert.mapToTypes()
        // TODO check types for void
        val parameters = mhType.signature.parameterTypes
        if (additionalTypes.isEmpty()) return mhType // no change
        return withParameters(mhType, parameters + additionalTypes)
    }

    fun changeParameterType(mhType: MethodHandleType, num: PsiExpression, nptype: PsiExpression): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val type = nptype.asType() // TODO inspection on void
        val pos = num.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        val parameters = mhType.signature.parameterTypes
        if (pos < 0 || pos > parameters.size) return MethodHandleType(TopSignature) // TODO inspection
        val mutable = parameters.toMutableList()
        mutable[pos] = type
        return withParameters(mhType, mutable)
    }

    fun changeReturnType(mhType: MethodHandleType, nrtype: PsiExpression): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val type = nrtype.asType() // TODO inspection on void
        return MethodHandleType(mhType.signature.withReturnType(type))
    }

    fun dropParameterTypes(mhType: MethodHandleType, start: PsiExpression, end: PsiExpression): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val startIndex = start.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        val endIndex = end.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        val parameters = mhType.signature.parameterTypes
        if (invalidRange(
                parameters.size,
                startIndex,
                endIndex
            )
        ) return MethodHandleType(TopSignature) // TODO inspection
        val mutable = parameters.toMutableList()
        mutable.subList(startIndex, endIndex).clear()
        return withParameters(mhType, mutable)
    }

    fun erase(mhType: MethodHandleType, objectType: PsiExpression): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val params = mhType.signature.parameterTypes.map { it.erase(objectType.manager, objectType.resolveScope) }
        val ret = mhType.signature.returnType.erase(objectType.manager, objectType.resolveScope)
        return MethodHandleType(CompleteSignature(ret, params))
    }

    fun generic(mhType: MethodHandleType, objectType: PsiType): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val size = mhType.signature.parameterTypes.size
        val objType = DirectType(objectType)
        return MethodHandleType(CompleteSignature(objType, nCopies(size, objType)))
    }

    fun genericMethodType(objectArgCount: PsiExpression, finalArray: Boolean, objectType: PsiType): MethodHandleType {
        val objType = DirectType(objectType)
        val count = objectArgCount.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        var parameters = nCopies(count, objType)
        if (finalArray) {
            parameters = parameters + DirectType(objectType.createArrayType())
        }
        return MethodHandleType(CompleteSignature(objType, parameters))
    }

    fun insertParameterTypes(
        mhType: MethodHandleType,
        num: PsiExpression,
        ptypesToInsert: List<PsiExpression>
    ): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val psiTypes = ptypesToInsert.mapToTypes()
        // TODO check types for void
        val parameters = mhType.signature.parameterTypes
        val pos = num.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        if (pos < 0 || pos > parameters.size) return MethodHandleType(TopSignature) // TODO inspection
        if (psiTypes.isEmpty()) return mhType // no change
        val mutable = parameters.toMutableList()
        mutable.addAll(pos, psiTypes)
        return withParameters(mhType, mutable)
    }

    private fun withParameters(
        mhType: MethodHandleType,
        newParameters: List<Type>
    ) = MethodHandleType(mhType.signature.withParameterTypes(newParameters))

    private fun invalidRange(parametersSize: Int, start: Int, end: Int): Boolean {
        return start < 0 || start > parametersSize || end < 0 || end > parametersSize || start > end
    }

    fun methodType(args: List<PsiExpression>): MethodHandleType {
        val rtype = args[0].asType()
        val params = args.drop(1).map { it.asType() }
        return MethodHandleType(CompleteSignature(rtype, params))
    }

    fun methodType(rtype: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val type = rtype.asType()
        return MethodHandleType(mhType.signature.withReturnType(type))
    }

    fun unwrap(mhType: MethodHandleType): MethodHandleType = mapTypes(mhType, this::unwrap)

    fun wrap(context: PsiElement, mhType: MethodHandleType): MethodHandleType = mapTypes(mhType) { wrap(context, it) }

    private fun mapTypes(mhType: MethodHandleType, map: (Type) -> Type): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val ret = map(mhType.signature.returnType)
        val params = mhType.signature.parameterTypes.map { map(it) }
        return MethodHandleType(CompleteSignature(ret, params))
    }

    private fun unwrap(type: Type): Type {
        if (type !is DirectType) {
            return type
        }
        return DirectType(PsiPrimitiveType.getOptionallyUnboxedType(type.psiType) ?: type.psiType)
    }

    private fun wrap(context: PsiElement, type: Type): Type {
        if (type !is DirectType || type.psiType !is PsiPrimitiveType) return type
        return DirectType(type.psiType.getBoxedType(context) ?: type.psiType)
    }
}
