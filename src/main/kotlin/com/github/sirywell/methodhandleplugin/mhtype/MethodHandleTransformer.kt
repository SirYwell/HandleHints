package com.github.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiType

object MethodHandleTransformer {

    // fun asCollector()

    fun asFixedArity(type: MhType) = type

    // fun asSpreader()

    // TODO this is not exactly right...
    fun asType(type: MhType, newType: MhType) = MethodHandlesMerger.explicitCastArguments(type, newType)

    // fun asVarargsCollector()

    fun bindTo(type: MhType, objectType: PsiType): MhType {
        if (type !is MhSingleType) return type
        val firstParamType = type.signature.parameters.getOrElse(0) { return Top }
        if (!firstParamType.isConvertibleFrom(objectType)) return Top
        return type.withSignature(type.signature.withParameters(type.signature.parameters.drop(1)))
    }

    // fun withVarargs()
}
