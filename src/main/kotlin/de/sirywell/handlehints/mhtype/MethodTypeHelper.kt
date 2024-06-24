package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.asType
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.mapToTypes
import de.sirywell.handlehints.type.*
import java.util.Collections.nCopies

class MethodTypeHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {
    private val topType = TopMethodHandleType

    fun appendParameterTypes(mhType: MethodHandleType, ptypesToInsert: List<PsiExpression>): MethodHandleType {
        // TODO check types for void
        val additionalTypes = ptypesToInsert.mapToTypes()
        if (additionalTypes.isEmpty()) return mhType // no change
        val parameters = mhType.parameterList as? CompleteParameterList ?: return topType
        return withParameters(mhType, parameters.addAllAt(parameters.size, CompleteParameterList(additionalTypes)))
    }

    fun changeParameterType(mhType: MethodHandleType, num: PsiExpression, nptype: PsiExpression): MethodHandleType {
        val type = nptype.asType() // TODO inspection on void
        val pos = num.getConstantOfType<Int>() ?: return BotMethodHandleType
        val parameters = mhType.parameterList
        if (pos < 0 || parameters.sizeMatches { pos > it } == TriState.YES) return topType // TODO inspection
        return withParameters(mhType, parameters.setAt(pos, type))
    }

    fun changeReturnType(mhType: MethodHandleType, nrtype: PsiExpression): MethodHandleType {
        val type = nrtype.asType() // TODO inspection on void
        return mhType.withReturnType(type)
    }

    fun dropParameterTypes(mhType: MethodHandleType, start: PsiExpression, end: PsiExpression): MethodHandleType {
        if (mhType !is CompleteMethodHandleType) return mhType
        val startIndex = start.getConstantOfType<Int>()
            ?: return complete(mhType.returnType, BotParameterList)
        val endIndex = end.getConstantOfType<Int>()
            ?: return complete(mhType.returnType, BotParameterList)
        val parameters = mhType.parameterList
        val size = parameters.sizeOrNull() ?: return topType
        if (invalidRange(size, startIndex, endIndex)) return topType // TODO inspection
        return withParameters(mhType, parameters.removeAt(startIndex, endIndex - startIndex))
    }

    fun erase(mhType: MethodHandleType, objectType: PsiExpression): MethodHandleType {
        val ret = mhType.returnType.erase(objectType.manager, objectType.resolveScope)
        val parameterList = mhType.parameterList as? CompleteParameterList
            ?: return complete(ret, TopParameterList)
        val params = parameterList.parameterTypes.map { it.erase(objectType.manager, objectType.resolveScope) }
        return complete(ret, params)
    }

    fun generic(mhType: MethodHandleType, objectType: PsiType): MethodHandleType {
        val size = (mhType.parameterList as? CompleteParameterList)?.size ?: return topType
        val objType = ExactType(objectType)
        return complete(objType, nCopies(size, objType))
    }

    fun genericMethodType(objectArgCount: PsiExpression, finalArray: Boolean, objectType: PsiType): MethodHandleType {
        val objType = ExactType(objectType)
        val count = objectArgCount.getConstantOfType<Int>() ?: return BotMethodHandleType
        var parameters = nCopies(count, objType)
        if (finalArray) {
            parameters = parameters + ExactType(objectType.createArrayType())
        }
        return complete(objType, parameters)
    }

    fun insertParameterTypes(
        mhType: MethodHandleType,
        num: PsiExpression,
        ptypesToInsert: List<PsiExpression>
    ): MethodHandleType {
        val types = ptypesToInsert.mapToTypes()
        // TODO check types for void
        val parameters = mhType.parameterList
        val pos = num.getConstantOfType<Int>() ?: return complete(mhType.returnType, BotParameterList)
        if (pos < 0 || parameters.sizeMatches { pos > it } == TriState.YES) return topType // TODO inspection
        if (types.isEmpty()) return mhType // no change
        val mutable = parameters.addAllAt(pos, CompleteParameterList(types))
        return withParameters(mhType, mutable)
    }

    private fun withParameters(
        mhType: MethodHandleType,
        newParameters: ParameterList
    ) = mhType.withParameterTypes(newParameters)

    private fun invalidRange(parametersSize: Int, start: Int, end: Int): Boolean {
        return start < 0 || start > parametersSize || end < 0 || end > parametersSize || start > end
    }

    fun methodType(args: List<PsiExpression>): MethodHandleType {
        val rtype = args[0].asType()
        val params = args.drop(1).map { it.asType() }
        return complete(rtype, params)
    }

    fun methodType(rtype: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        val type = rtype.asType()
        return mhType.withReturnType(type)
    }

    fun unwrap(mhType: MethodHandleType): MethodHandleType = mapTypes(mhType, this::unwrap)

    fun wrap(context: PsiElement, mhType: MethodHandleType): MethodHandleType = mapTypes(mhType) { wrap(context, it) }

    private fun mapTypes(mhType: MethodHandleType, map: (Type) -> Type): MethodHandleType {
        if (mhType !is CompleteMethodHandleType) return mhType
        val ret = map(mhType.returnType)
        val parameterList = mhType.parameterList as? CompleteParameterList
            ?: return complete(ret, TopParameterList)
        val params = parameterList.parameterTypes.map { map(it) }
        return complete(ret, params)
    }

    private fun unwrap(type: Type): Type {
        if (type !is ExactType) {
            return type
        }
        return ExactType(PsiPrimitiveType.getOptionallyUnboxedType(type.psiType) ?: type.psiType)
    }

    private fun wrap(context: PsiElement, type: Type): Type {
        if (type !is ExactType || type.psiType !is PsiPrimitiveType) return type
        return ExactType(type.psiType.getBoxedType(context) ?: type.psiType)
    }
}
