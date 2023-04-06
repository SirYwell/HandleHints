package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import de.sirywell.methodhandleplugin.getConstantOfType
import java.util.Collections.nCopies

object MethodTypeHelper {

    fun appendParameterTypes(mhType: MhType, ptypesToInsert: List<PsiExpression>): MhType {
        if (mhType !is MhSingleType) return mhType
        val psiTypes = ptypesToInsert.map { it.getConstantOfType<PsiType>() ?: return Top }
        // TODO check types for void
        val parameters = mhType.signature.parameters
        if (psiTypes.isEmpty()) return mhType // no change
        return withParameters(mhType, parameters + psiTypes)
    }

    fun changeParameterType(mhType: MhType, num: PsiExpression, nptype: PsiExpression): MhType {
        if (mhType !is MhSingleType) return mhType
        val type = nptype.getConstantOfType<PsiType>() ?: return Top // TODO inspection on void
        val pos = num.getConstantOfType<Int>() ?: return Top
        val parameters = mhType.signature.parameters
        if (pos < 0 || pos > parameters.size) return Top // TODO inspection
        val mutable = parameters.toMutableList()
        mutable[pos] = type
        return withParameters(mhType, mutable)
    }

    fun changeReturnType(mhType: MhType, nrtype: PsiExpression): MhType {
        if (mhType !is MhSingleType) return mhType
        val type = nrtype.getConstantOfType<PsiType>() ?: return Top // TODO inspection on void
        return mhType.withSignature(mhType.signature.withReturnType(type))
    }

    fun dropParameterTypes(mhType: MhType, start: PsiExpression, end: PsiExpression): MhType {
        if (mhType !is MhSingleType) return mhType
        val startIndex = start.getConstantOfType<Int>() ?: return Bot
        val endIndex = end.getConstantOfType<Int>() ?: return Bot
        val parameters = mhType.signature.parameters
        if (invalidRange(parameters.size, startIndex, endIndex)) return Top // TODO inspection
        val mutable = parameters.toMutableList()
        mutable.subList(startIndex, endIndex).clear()
        return withParameters(mhType, mutable)
    }

    fun erase(mhType: MhType, objectType: PsiType): MhType {
        if (mhType !is MhSingleType) return Bot
        val params = mhType.signature.parameters.map { it.erase(objectType) }
        val ret = mhType.signature.returnType.erase(objectType)
        return mhType.withSignature(MHS.create(ret, params))
    }

    fun generic(mhType: MhType, objectType: PsiType): MhType {
        if (mhType !is MhSingleType) return mhType
        val size = mhType.signature.parameters.size
        return mhType.withSignature(MHS.create(objectType, nCopies(size, objectType)))
    }

    fun genericMethodType(objectArgCount: PsiExpression, finalArray: Boolean, objectType: PsiType): MhType {
        val count = objectArgCount.getConstantOfType<Int>() ?: return Bot
        var parameters = nCopies(count, objectType)
        if (finalArray) {
            parameters = parameters + objectType.createArrayType()
        }
        return MhExactType(MHS.create(objectType, parameters))
    }

    fun insertParameterTypes(mhType: MhType, num: PsiExpression, ptypesToInsert: List<PsiExpression>): MhType {
        if (mhType !is MhSingleType) return mhType
        val psiTypes = ptypesToInsert.map { it.getConstantOfType<PsiType>() ?: return Top }
        // TODO check types for void
        val parameters = mhType.signature.parameters
        val pos = num.getConstantOfType<Int>() ?: return Top
        if (pos < 0 || pos > parameters.size) return Top // TODO inspection
        if (psiTypes.isEmpty()) return mhType // no change
        val mutable = parameters.toMutableList()
        mutable.addAll(pos, psiTypes)
        return withParameters(mhType, mutable)
    }

    private fun withParameters(
        mhType: MhSingleType,
        newParameters: List<PsiType>
    ) = mhType.withSignature(mhType.signature.withParameters(newParameters))

    private fun invalidRange(parametersSize: Int, start: Int, end: Int): Boolean {
        return start < 0 || start > parametersSize || end < 0 || end > parametersSize || start > end
    }

    fun methodType(args: List<PsiExpression>): MhType {
        val rtype = args[0].getConstantOfType<PsiType>() ?: return Bot
        val params = args.drop(1).map { it.getConstantOfType<PsiType>() ?: return Bot }
        return MhExactType(create(rtype, params.toList()))
    }

    fun methodType(rtype: PsiExpression, mhType: MhType): MhType {
        if (mhType !is MhSingleType) return mhType
        val type = rtype.getConstantOfType<PsiType>() ?: return Bot
        return mhType.withSignature(mhType.signature.withReturnType(type))
    }

    fun unwrap(mhType: MhType): MhType = mapTypes(mhType, this::unwrap)

    fun wrap(context: PsiElement, mhType: MhType): MhType = mapTypes(mhType) { wrap(context, it) }

    private fun mapTypes(mhType: MhType, map: (PsiType) -> PsiType): MhType {
        if (mhType !is MhSingleType) return mhType
        val ret = map(mhType.signature.returnType)
        val params = mhType.signature.parameters.map { map(it) }
        return mhType.withSignature(MHS.create(ret, params))
    }

    private fun PsiType.erase(objectType: PsiType): PsiType {
        if (this is PsiPrimitiveType) return this
        return objectType
    }

    private fun unwrap(type: PsiType): PsiType = PsiPrimitiveType.getOptionallyUnboxedType(type) ?: type

    private fun wrap(context: PsiElement, type: PsiType): PsiType {
        if (type !is PsiPrimitiveType) return type
        return type.getBoxedType(context) ?: type
    }
}
