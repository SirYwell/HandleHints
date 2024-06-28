package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiExpression
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class MethodHandleTransformer(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    // fun asCollector()

    fun asFixedArity(type: MethodHandleType) = type.withVarargs(TriState.NO)

    // fun asSpreader()

    // TODO this is not exactly right...
    fun asType(type: MethodHandleType, newType: MethodHandleType): MethodHandleType = TODO()

    // fun asVarargsCollector()

    fun bindTo(typeExpr: PsiExpression, objectType: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val type = ssaAnalyzer.methodHandleType(typeExpr, block) ?: BotMethodHandleType
        if (type !is CompleteMethodHandleType) return type
        val parameterTypes = type.typeLatticeElementList
        val firstParamType =
            // try to extract a first param if it exists
            // - CompleteParameterList has a first param if the size is > 0
            // - IncompleteParameterList has a first param as we assume it to be non-empty
            // - BotParameterList may have some first param
            // - TopParameterList may have some first param
            if (parameterTypes is CompleteTypeLatticeElementList && parameterTypes.size > 0
                || parameterTypes is IncompleteTypeLatticeElementList
                || parameterTypes is BotTypeLatticeElementList
                || parameterTypes is TopTypeLatticeElementList
            ) {
                type.parameterTypeAt(0)
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
        return type.withParameterTypes(parameterTypes.dropFirst(1))
    }

    // fun withVarargs()

}
