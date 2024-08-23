package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.parentOfType
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.inspection.RedundantInvocationFix
import de.sirywell.handlehints.toTriState
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
        val parameterTypes = type.parameterTypes
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

    fun withVarargs(
        qualifierExpr: PsiExpression,
        makeVarargsExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val qualifier = ssaAnalyzer.methodHandleType(qualifierExpr, block) ?: TopMethodHandleType
        val makeVarargs = makeVarargsExpr.getConstantOfType<Boolean>()
        val methodExpr = makeVarargsExpr.parentOfType<PsiMethodCallExpression>()!!
        if (makeVarargs == true) {
            if (qualifier.parameterTypes.compareSize(0) == PartialOrder.EQ) {
                return emitProblem(qualifierExpr, message("problem.merging.withVarargs.noParameters"))
            } else {
                val last = qualifier.parameterTypes.lastOrNull() ?: return TopMethodHandleType
                if (last is ExactType && last.psiType.arrayDimensions == 0) {
                    return emitProblem(qualifierExpr, message("problem.merging.withVarargs.arrayTypeExpected", last))
                }
            }
            if (qualifier.varargs == TriState.YES) {
                emitRedundant(methodExpr, message("problem.general.invocation.redundant"), RedundantInvocationFix())
            }
        } else if (makeVarargs == false && qualifier.varargs == TriState.NO) {
            emitRedundant(methodExpr, message("problem.general.invocation.redundant"), RedundantInvocationFix())
        }
        return qualifier.withVarargs(makeVarargs.toTriState())
    }

}
