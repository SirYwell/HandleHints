package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.TriState
import de.sirywell.methodhandleplugin.asType
import de.sirywell.methodhandleplugin.getConstantOfType
import de.sirywell.methodhandleplugin.mapToTypes
import de.sirywell.methodhandleplugin.type.*
import java.util.Collections.nCopies

object MethodTypeHelper {
    private val topType = MethodHandleType(TopSignature)

    fun appendParameterTypes(mhType: MethodHandleType, ptypesToInsert: List<PsiExpression>): MethodHandleType {
        // TODO check types for void
        val additionalTypes = ptypesToInsert.mapToTypes()
        if (additionalTypes.isEmpty()) return mhType // no change
        val parameters = mhType.signature.parameterList as? CompleteParameterList ?: return topType
        return withParameters(mhType, parameters.addAllAt(parameters.size, CompleteParameterList(additionalTypes)))
    }

    fun changeParameterType(mhType: MethodHandleType, num: PsiExpression, nptype: PsiExpression): MethodHandleType {
        val type = nptype.asType() // TODO inspection on void
        val pos = num.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        val parameters = mhType.signature.parameterList
        if (pos < 0 || parameters.sizeMatches { pos > it } == TriState.YES) return topType // TODO inspection
        return withParameters(mhType, parameters.setAt(pos, type))
    }

    fun changeReturnType(mhType: MethodHandleType, nrtype: PsiExpression): MethodHandleType {
        val type = nrtype.asType() // TODO inspection on void
        return MethodHandleType(mhType.signature.withReturnType(type))
    }

    fun dropParameterTypes(mhType: MethodHandleType, start: PsiExpression, end: PsiExpression): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val startIndex = start.getConstantOfType<Int>()
            ?: return MethodHandleType(complete(mhType.signature.returnType, BotParameterList))
        val endIndex = end.getConstantOfType<Int>()
            ?: return MethodHandleType(complete(mhType.signature.returnType, BotParameterList))
        val parameters = mhType.signature.parameterList
        val size = parameters.sizeOrNull() ?: return topType
        if (invalidRange(size, startIndex, endIndex)) return topType // TODO inspection
        return withParameters(mhType, parameters.removeAt(startIndex, endIndex - startIndex))
    }

    fun erase(mhType: MethodHandleType, objectType: PsiExpression): MethodHandleType {
        val ret = mhType.signature.returnType.erase(objectType.manager, objectType.resolveScope)
        val parameterList = mhType.signature.parameterList as? CompleteParameterList
            ?: return MethodHandleType(complete(ret, TopParameterList))
        val params = parameterList.parameterTypes.map { it.erase(objectType.manager, objectType.resolveScope) }
        return MethodHandleType(complete(ret, params))
    }

    fun generic(mhType: MethodHandleType, objectType: PsiType): MethodHandleType {
        val size = (mhType.signature.parameterList as? CompleteParameterList)?.size ?: return topType
        val objType = DirectType(objectType)
        return MethodHandleType(complete(objType, nCopies(size, objType)))
    }

    fun genericMethodType(objectArgCount: PsiExpression, finalArray: Boolean, objectType: PsiType): MethodHandleType {
        val objType = DirectType(objectType)
        val count = objectArgCount.getConstantOfType<Int>() ?: return MethodHandleType(BotSignature)
        var parameters = nCopies(count, objType)
        if (finalArray) {
            parameters = parameters + DirectType(objectType.createArrayType())
        }
        return MethodHandleType(complete(objType, parameters))
    }

    fun insertParameterTypes(
        mhType: MethodHandleType,
        num: PsiExpression,
        ptypesToInsert: List<PsiExpression>
    ): MethodHandleType {
        val types = ptypesToInsert.mapToTypes()
        // TODO check types for void
        val parameters = mhType.signature.parameterList
        val pos = num.getConstantOfType<Int>() ?: return MethodHandleType(complete(mhType.signature.returnType, BotParameterList))
        if (pos < 0 || parameters.sizeMatches { pos > it } == TriState.YES) return topType // TODO inspection
        if (types.isEmpty()) return mhType // no change
        val mutable = parameters.addAllAt(pos, CompleteParameterList(types))
        return withParameters(mhType, mutable)
    }

    private fun withParameters(
        mhType: MethodHandleType,
        newParameters: ParameterList
    ) = MethodHandleType(mhType.signature.withParameterTypes(newParameters))

    private fun invalidRange(parametersSize: Int, start: Int, end: Int): Boolean {
        return start < 0 || start > parametersSize || end < 0 || end > parametersSize || start > end
    }

    fun methodType(args: List<PsiExpression>): MethodHandleType {
        val rtype = args[0].asType()
        val params = args.drop(1).map { it.asType() }
        return MethodHandleType(complete(rtype, params))
    }

    fun methodType(rtype: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        val type = rtype.asType()
        return MethodHandleType(mhType.signature.withReturnType(type))
    }

    fun unwrap(mhType: MethodHandleType): MethodHandleType = mapTypes(mhType, this::unwrap)

    fun wrap(context: PsiElement, mhType: MethodHandleType): MethodHandleType = mapTypes(mhType) { wrap(context, it) }

    private fun mapTypes(mhType: MethodHandleType, map: (Type) -> Type): MethodHandleType {
        if (mhType.signature !is CompleteSignature) return mhType
        val ret = map(mhType.signature.returnType)
        val parameterList = mhType.signature.parameterList as? CompleteParameterList
            ?: return MethodHandleType(complete(ret, TopParameterList))
        val params = parameterList.parameterTypes.map { map(it) }
        return MethodHandleType(complete(ret, params))
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
