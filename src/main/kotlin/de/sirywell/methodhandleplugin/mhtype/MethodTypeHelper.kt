package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import de.sirywell.methodhandleplugin.getConstantOfType

object MethodTypeHelper {

    fun appendParameterTypes(mhType: MhType, ptypesToInsert: List<PsiExpression>): MhType = TODO()

    fun changeParameterType(mhType: MhType, num: PsiExpression, nptype: PsiExpression): MhType = TODO()

    fun changeReturnType(mhType: MhType, num: PsiExpression, nptype: PsiExpression): MhType = TODO()

    fun dropParameterTypes(mhType: MhType, start: PsiExpression, end: PsiExpression): MhType {
        if (mhType !is MhSingleType) return mhType
        val startIndex = start.getConstantOfType<Int>() ?: return Bot
        val endIndex = end.getConstantOfType<Int>() ?: return Bot
        val parameters = mhType.signature.parameters
        if (invalidRange(parameters.size, startIndex, endIndex)) return Top // TODO inspection
        val mutable = parameters.toMutableList()
        mutable.subList(startIndex, endIndex).clear()
        return mhType.withSignature(mhType.signature.withParameters(mutable.toList()))
    }
    fun erase(mhType: MhType): MhType = TODO()

    fun generic(mhType: MhType, objectArgCount: PsiExpression, finalArray: PsiExpression? = null): MhType = TODO()

    fun insertParameterTypes(num: PsiExpression, ptypesToInsert: List<PsiExpression>): MhType = TODO()

    private fun invalidRange(parametersSize: Int, start: Int, end: Int): Boolean {
        return start < 0 || start > parametersSize || end < 0 || end > parametersSize || start > end
    }

    fun methodType(args: List<PsiType> = listOf()): MhType {
        if (args.isEmpty()) return Bot
        val rtype = args[0]
        val params = args.drop(1)
        return MhExactType(create(rtype, params.toList()))
    }

    fun unwrap(mhType: MhType): MhType = mapTypes(mhType, this::unwrap)

    fun wrap(context: PsiElement, mhType: MhType): MhType = mapTypes(mhType) { wrap(context, it)}

    private fun mapTypes(mhType: MhType, map: (PsiType) -> PsiType): MhType {
        if (mhType !is MhSingleType) return mhType
        val ret = map(mhType.signature.returnType)
        val params = mhType.signature.parameters.map { map(it) }
        return mhType.withSignature(MHS.create(ret, params))
    }

    private fun PsiType.erase(): PsiType = TODO()

    private fun unwrap(type: PsiType): PsiType = PsiPrimitiveType.getOptionallyUnboxedType(type) ?: type

    private fun wrap(context: PsiElement, type: PsiType): PsiType {
        if (type !is PsiPrimitiveType) return type
        return type.getBoxedType(context) ?: type
    }
}
