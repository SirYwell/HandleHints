package de.sirywell.handlehints.mhtype

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.type.*
import org.jetbrains.annotations.Nls

class MethodHandleTransformer(private val ssaAnalyzer: SsaAnalyzer) {

    // fun asCollector()

    fun asFixedArity(type: MethodHandleType) = MethodHandleType(type.signature.withVarargs(TriState.NO))

    // fun asSpreader()

    // TODO this is not exactly right...
    fun asType(type: MethodHandleType, newType: MethodHandleType): MethodHandleType = TODO()

    // fun asVarargsCollector()

    fun bindTo(typeExpr: PsiExpression, objectType: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val type = ssaAnalyzer.mhType(typeExpr, block) ?: MethodHandleType(BotSignature)
        if (type.signature !is CompleteSignature) return type
        val parameterTypes = type.signature.parameterList
        val firstParamType =
            // try to extract a first param if it exists
            // - CompleteParameterList has a first param if the size is > 0
            // - IncompleteParameterList has a first param as we assume it to be non-empty
            // - BotParameterList may have some first param
            // - TopParameterList may have some first param
            if (parameterTypes is CompleteParameterList && parameterTypes.size > 0
                || parameterTypes is IncompleteParameterList
                || parameterTypes is BotParameterList
                || parameterTypes is TopParameterList
            ) {
                type.signature.parameterTypeAt(0)
            } else {
                return emitProblem(typeExpr, message("problem.general.parameters.noParameter"))
            }
        if (firstParamType is ExactType) {
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
        return MethodHandleType(type.signature.withParameterTypes(parameterTypes.dropFirst(1)))
    }

    // fun withVarargs()

    private fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        ssaAnalyzer.typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return MethodHandleType(TopSignature)
    }
}
