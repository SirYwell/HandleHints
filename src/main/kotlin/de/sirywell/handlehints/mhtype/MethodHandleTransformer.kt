package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPrimitiveType
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
    fun asType(
        qualifierExpr: PsiExpression,
        newTypeExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val qualifier = ssaAnalyzer.methodHandleType(qualifierExpr, block) ?: TopMethodHandleType
        // TODO this is difficult to handle
        if (qualifier.varargs != TriState.NO) return TopMethodHandleType
        val newType = ssaAnalyzer.methodHandleType(newTypeExpr, block) ?: TopMethodHandleType
        val returnType = convert(qualifierExpr, qualifier.returnType, newType.returnType, false)
        val parameterTypes = convert(qualifierExpr, qualifier.parameterTypes, newType.parameterTypes)
        return complete(returnType, parameterTypes)
    }

    private fun convert(
        context: PsiExpression,
        t0: TypeList,
        t1: TypeList
    ): TypeList {
        val s0 = t0.sizeOrNull()
        val s1 = t1.sizeOrNull()
        if (s0 != null && s1 != null && s0 != s1) {
            return emitProblem(context, "params size mismatch")
        }
        val l0 = t0.partialList()
        val l1 = t1.partialList()
        val list = l0.zip(l1) { p0, p1 -> convert(context, p1, p0, true) }
        return if (s0 != null) {
            // both have the same length
            CompleteTypeList(list)
        } else {
            // we might lose some params, but that's good enough for an already incomplete list
            IncompleteTypeList(list.toIndexedMap())
        }
    }

    private fun convert(context: PsiElement, t0: Type, t1: Type, parameter: Boolean): Type {
        val t0NotPrimitive = t0.isPrimitive() == TriState.NO
        val t1NotPrimitive = t1.isPrimitive() == TriState.NO
        if (t0NotPrimitive && t1NotPrimitive) {
            // If T0 and T1 are references, then a cast to T1 is applied. [...]
            return if (parameter) t0 else t1
        }
        val (join, identical) = t0.joinIdentical(t1)
        if (identical == TriState.YES) {
            return join
        }
        if (t0 !is ExactType || t1 !is ExactType) {
            return join
        }
        val p0 = t0.psiType
        val p1 = t1.psiType
        if (!t0NotPrimitive && !t1NotPrimitive) {
            // If T0 and T1 are primitives, then [...]
            return if (p1.isAssignableFrom(p0)) {
                if (parameter) t0 else t1
            } else {
                emitProblem(context, "incompatible")
            }
        }
        if (t1NotPrimitive) {
            // If T0 is a primitive and T1 a reference [...]
            val b0 = (p0 as PsiPrimitiveType).getBoxedType(context)!!
            return if (b0.isAssignableFrom(p1)) {
                if (parameter) t0 else t1
            } else {
                emitProblem(context, "incompatible 2")
            }
        } else {
            // If T0 is a reference and T1 a primitive [...]
            return if (parameter) t0 else t1 // TODO this is a bit involved, but should be doable?
        }
    }

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
            if (firstParamType.isPrimitive() == TriState.YES) {
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
