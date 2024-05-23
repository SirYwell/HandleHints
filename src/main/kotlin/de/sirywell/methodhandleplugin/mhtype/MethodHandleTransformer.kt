package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.type.CompleteSignature
import de.sirywell.methodhandleplugin.type.DirectType
import de.sirywell.methodhandleplugin.type.MethodHandleType
import de.sirywell.methodhandleplugin.type.TopSignature

object MethodHandleTransformer {

    // fun asCollector()

    fun asFixedArity(type: MethodHandleType) = type

    // fun asSpreader()

    // TODO this is not exactly right...
    fun asType(type: MethodHandleType, newType: MethodHandleType): MethodHandleType = TODO()

    // fun asVarargsCollector()

    fun bindTo(type: MethodHandleType, objectType: PsiType): MethodHandleType {
        if (type.signature !is CompleteSignature) return type
        val firstParamType = type.signature.parameterTypes.getOrElse(0) { return MethodHandleType(TopSignature) }
        if (firstParamType is DirectType && !firstParamType.psiType.isConvertibleFrom(objectType)) {
            return MethodHandleType(TopSignature)
        }
        return MethodHandleType(type.signature.withParameterTypes(type.signature.parameterTypes.drop(1)))
    }

    // fun withVarargs()
}
