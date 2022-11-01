package com.github.sirywell.methodhandleplugin.mhtype

import com.github.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType

object MethodTypeHelper {

    fun appendParameterTypes(mhType: MhType, ptypesToInsert: List<PsiExpression>): MhType = TODO()

    fun changeParameterType(mhType: MhType, num: PsiExpression, nptype: PsiExpression): MhType = TODO()

    fun changeReturnType(mhType: MhType, num: PsiExpression, nptype: PsiExpression): MhType = TODO()

    fun dropParameterTypes(mhType: MhType, start: PsiExpression, end: PsiExpression): MhType = TODO()

    fun erase(mhType: MhType): MhType = TODO()

    fun generic(mhType: MhType, objectArgCount: PsiExpression, finalArray: PsiExpression? = null): MhType = TODO()

    fun insertParameterTypes(num: PsiExpression, ptypesToInsert: List<PsiExpression>): MhType = TODO()

    fun methodType(args: List<PsiType> = listOf()): MhType {
        if (args.isEmpty()) return Bot
        val rtype = args[0]
        val params = args.drop(1)
        return MhExactType(create(rtype, params.toList()))
    }

    fun unwrap(mhType: MhType): MhType = TODO()

    fun wrap(mhType: MhType): MhType = TODO()

    private fun PsiType.erase(): PsiType = TODO()
}
