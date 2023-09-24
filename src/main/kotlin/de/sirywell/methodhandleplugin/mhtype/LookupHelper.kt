package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.getConstantOfType

object LookupHelper {

    // MethodHandle factory methods

    fun findConstructor(refc: PsiExpression, type: MhType): MhType {
        if (type !is MhSingleType) return type
        if (type.returnType != PsiTypes.voidType()) return unexpectedReturnType(type.returnType, PsiTypes.voidType())
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass == PsiTypes.voidType()) return typeMustNotBe(refc, PsiTypes.voidType())
        return type.withSignature(type.signature.withReturnType(referenceClass))
    }

    fun findGetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        val returnType = type.getConstantOfType<PsiType>() ?: return Bot
        if (returnType == PsiTypes.voidType()) return typeMustNotBe(type, PsiTypes.voidType())
        return MhExactType(MHS.create(returnType, listOf(referenceClass)))
    }

    fun findSetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        val paramType = type.getConstantOfType<PsiType>() ?: return Bot
        if (paramType == PsiTypes.voidType()) return typeMustNotBe(type, PsiTypes.voidType())
        return MhExactType(MHS.create(PsiTypes.voidType(), listOf(referenceClass, paramType)))
    }

    fun findSpecial(refc: PsiExpression, type: MhType, specialCaller: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        if (type !is MhSingleType) return type
        // TODO inspection:  caller class must be a subclass below the method
        val paramType = specialCaller.getConstantOfType<PsiType>() ?: return Bot
        return prependParameter(type, paramType)
    }

    fun findStatic(refc: PsiExpression, mhType: MhType): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        return mhType
    }

    fun findStaticGetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        val returnType = type.getConstantOfType<PsiType>() ?: return Bot
        if (returnType == PsiTypes.voidType()) return typeMustNotBe(type, PsiTypes.voidType())
        return MhExactType(MHS.create(returnType, listOf()))
    }

    fun findStaticSetter(refc: PsiExpression, type: PsiExpression): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
        val paramType = type.getConstantOfType<PsiType>() ?: return Bot
        if (paramType == PsiTypes.voidType()) return typeMustNotBe(type, PsiTypes.voidType())
        return MhExactType(MHS.create(PsiTypes.voidType(), listOf(paramType)))
    }

    fun findVirtual(refc: PsiExpression, mhType: MhType): MhType {
        val referenceClass = refc.getConstantOfType<PsiType>() ?: return Bot
        if (referenceClass is PsiPrimitiveType) return referenceTypeExpected(refc, referenceClass)
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