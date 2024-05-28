package de.sirywell.methodhandleplugin.mhtype

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import de.sirywell.methodhandleplugin.MethodHandleBundle.message
import de.sirywell.methodhandleplugin.dfa.SsaAnalyzer
import de.sirywell.methodhandleplugin.dfa.SsaConstruction
import de.sirywell.methodhandleplugin.type.*
import org.jetbrains.annotations.Nls

class MethodHandleTransformer(private val ssaAnalyzer: SsaAnalyzer) {

    // fun asCollector()

    fun asFixedArity(type: MethodHandleType) = type

    // fun asSpreader()

    // TODO this is not exactly right...
    fun asType(type: MethodHandleType, newType: MethodHandleType): MethodHandleType = TODO()

    // fun asVarargsCollector()

    fun bindTo(typeExpr: PsiExpression, objectType: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val type = ssaAnalyzer.mhType(typeExpr, block) ?: MethodHandleType(BotSignature)
        if (type.signature !is CompleteSignature) return type
        val firstParamType = type.signature.parameterTypes.getOrElse(0) { return MethodHandleType(TopSignature) }
        if (firstParamType is DirectType) {
            if (firstParamType.isPrimitive()) {
                return emitProblem(
                    typeExpr,
                    message("problem.merging.general.referenceTypeExpectedParameter", 0, firstParamType)
                )
            } else if (objectType.type != null && !firstParamType.psiType.isConvertibleFrom(objectType.type!!)) {
                return emitProblem(
                    objectType,
                    message(
                        "problem.general.parameters.expected.type",
                        firstParamType,
                        objectType.type!!.presentableText
                    )
                )
            }
        }
        return MethodHandleType(type.signature.withParameterTypes(type.signature.parameterTypes.drop(1)))
    }

    // fun withVarargs()

    private fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        ssaAnalyzer.typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return MethodHandleType(TopSignature)
    }
}
