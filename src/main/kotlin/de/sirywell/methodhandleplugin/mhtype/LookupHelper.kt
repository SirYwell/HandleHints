package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.getConstantOfType

object LookupHelper {

    // MethodHandle factory methods

    fun findConstructor(refc: PsiExpression, type: MhType): MhType {
        if (type !is MhSingleType) return type
        // TODO inspection in type return type
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        // TODO inspection on non-reference type
        return type.withSignature(type.signature.withReturnType(referenceClass))
    }

    fun findGetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        // TODO inspection on non-reference type
        val returnType = type.getConstantOfType<PsiType>() ?: return Bot
        // TODO non-void inspection
        return MhExactType(MHS.create(returnType, listOf(referenceClass)))
    }

    fun findSetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        // TODO inspection on non-reference type
        val paramType = type.getConstantOfType<PsiType>() ?: return Bot
        // TODO non-void inspection
        return MhExactType(MHS.create(PsiPrimitiveType.VOID, listOf(referenceClass, paramType)))
    }

    fun findSpecial(refc: PsiExpression, type: MhType, specialCaller: PsiExpression): MhType {
        if (type !is MhSingleType) return type
        // TODO inspection:  caller class must be a subclass below the method
        val paramType = specialCaller.getConstantOfType<PsiType>() ?: return Bot
        return prependParameter(type, paramType)
    }

    fun findStatic(mhType: MhType) = mhType

    fun findStaticGetter(refc: PsiExpression, type: PsiExpression): MhType {
        // TODO inspection on non-reference type
        val returnType = type.getConstantOfType<PsiType>() ?: return Bot
        // TODO non-void inspection
        return MhExactType(MHS.create(returnType, listOf()))
    }

    fun findStaticSetter(refc: PsiExpression, type: PsiExpression): MhType {
        // TODO inspection on non-reference type
        val paramType = type.getConstantOfType<PsiType>() ?: return Bot
        // TODO non-void inspection
        return MhExactType(MHS.create(PsiPrimitiveType.VOID, listOf(paramType)))
    }

    fun findVirtual(refc: PsiExpression, mhType: MhType): MhType {
        if (mhType !is MhSingleType) return mhType
        val paramType = refc.getConstantOfType<PsiType>() ?: return Bot
        // TODO not exactly correct, receiver could be restricted to lookup class
        return prependParameter(mhType, paramType)
    }

    private fun prependParameter(
        mhType: MhSingleType,
        paramType: PsiType
    ) = mhType.withSignature(mhType.signature.withParameters(listOf(paramType) + mhType.signature.parameters))
}