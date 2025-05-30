package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.parentOfType
import de.sirywell.handlehints.*
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.inspection.RedundantInvocationFix
import de.sirywell.handlehints.type.*

/**
 * Contains methods to merge multiple [MethodHandleType]s into a new one,
 * provided by [java.lang.invoke.MethodHandles]
 */
class MethodHandlesMerger(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    private val bottomType = BotMethodHandleType
    private val topType = TopMethodHandleType

    // TODO effectively identical sequences for loops???

    fun catchException(
        targetExpr: PsiExpression,
        exTypeExpr: PsiExpression,
        handlerExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val exType = exTypeExpr.asType()
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        val handler = ssaAnalyzer.methodHandleType(handlerExpr, block) ?: bottomType
        val handlerParameterTypes = handler.parameterTypes
        if (handlerParameterTypes is CompleteTypeLatticeElementList && (handlerParameterTypes.size == 0
                    || (exType is ExactType && !handlerParameterTypes[0].canBe(exType.psiType)))
        ) {
            emitProblem<MethodHandleType>(
                handlerExpr,
                message("problem.merging.catchException.missingException", exType)
            )
        }
        val returnType = if (target.returnType != handler.returnType) {
            emitIncompatibleReturnTypes(targetExpr, target.returnType, handler.returnType)
            TopType
        } else {
            target.returnType
        }
        val comparableTypes = handlerParameterTypes.dropFirst(1)
        // TODO calculate common type of parameters
        if (!comparableTypes.effectivelyIdenticalTo(target.parameterTypes)) {
            emitProblem<MethodHandleType>(
                targetExpr,
                message(
                    "problem.merging.general.effectivelyIdenticalParametersExpected",
                    target,
                    comparableTypes
                )
            )
        }
        return target.withReturnType(returnType)
    }

    fun collectArguments(
        targetExpr: PsiExpression,
        posExpr: PsiExpression,
        filterExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: return bottomType
        val filter = ssaAnalyzer.methodHandleType(filterExpr, block) ?: return bottomType
        var parameters = target.parameterTypes
        val pos = posExpr.nonNegativeInt() ?: return bottomType
        if (parameters is CompleteTypeLatticeElementList && pos >= parameters.size) {
            return emitProblem(
                posExpr,
                message("problem.general.position.invalidIndexKnownBoundsExcl", pos, parameters.size)
            )
        }
        val returnType = filter.returnType
        if (returnType is ExactType && returnType.psiType != PsiTypes.voidType()) {
            // the return type of the filter must match the replaced type
            if (parameters[pos] != returnType) {
                return topType
            }
            // the return value of filter will be passed to target at pos
            parameters = parameters.removeAt(pos)
        }
        val filterParameters = filter.parameterTypes
        parameters = parameters.addAllAt(pos, filterParameters)
        return target.withParameterTypes(parameters)
    }

    // fun collectCoordinates() no VarHandle support, preview

    fun countedLoop(
        iterationsExpr: PsiExpression,
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        var iterations = ssaAnalyzer.methodHandleType(iterationsExpr, block) ?: bottomType
        var init = ssaAnalyzer.methodHandleType(initExpr, block)
        val body = ssaAnalyzer.methodHandleType(bodyExpr, block) ?: bottomType
        if (iterations is BotMethodHandleType) {
            // iterations and init have an effectively identical parameter list
            // so if one is missing, just use the other, but assert return type
            iterations = init?.withReturnType(ExactType.intType)
                ?: return bottomType
        }
        if (init == null || init is BotMethodHandleType) {
            // just assume they are both the same (besides return type) for the rest of the logic
            val returnType = body.returnType
            init = iterations.withReturnType(returnType)
        }
        if (iterations is TopMethodHandleType || body is TopMethodHandleType || init is TopMethodHandleType) {
            return topType
        }
        if (!iterations.returnType.canBe(PsiTypes.intType())) {
            return topType
        }
        if (body is CompleteMethodHandleType) {
            val externalParameters: TypeList
            val internalParameters = body.parameterTypes
            val returnType = body.returnType
            if (init is CompleteMethodHandleType) {
                if (returnType != init.returnType) {
                    return topType
                }
            }
            if (returnType.canBe(PsiTypes.voidType())) {
                // (I A...)
                val size = internalParameters.sizeOrNull()
                if (size == 0 || !internalParameters[0].canBe(PsiTypes.intType())) {
                    return topType
                }
                externalParameters = if (size != 1) {
                    internalParameters.dropFirst(1)
                } else {
                    (iterations as CompleteMethodHandleType).parameterTypes
                }
            } else {
                // (V I A...)
                val size = internalParameters.sizeOrNull() ?: return topType
                if (size < 2 || (internalParameters[0] != returnType || !internalParameters[1].canBe(PsiTypes.intType()))) {
                    return topType
                }
                externalParameters = if (size != 2) {
                    internalParameters.dropFirst(2)
                } else {
                    (iterations as CompleteMethodHandleType).parameterTypes
                }
            }
            if (!(init as CompleteMethodHandleType).parameterTypes.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
            if (!(iterations as CompleteMethodHandleType).parameterTypes.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
        }
        if (!iterations.returnType.canBe(PsiTypes.intType())) {
            return topType
        }
        // this might not be very precise
        return iterations.withReturnType(init.returnType)
    }

    fun countedLoop(
        startExpr: PsiExpression,
        endExpr: PsiExpression,
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val start = ssaAnalyzer.methodHandleType(startExpr, block) ?: bottomType
        val end = ssaAnalyzer.methodHandleType(endExpr, block) ?: bottomType
        if (start.returnType.join(end.returnType) == TopType) {
            return emitIncompatibleReturnTypes(startExpr, start.returnType, end.returnType)
        }
        if (start !is CompleteMethodHandleType || end !is CompleteMethodHandleType) {
            return bottomType // TODO not correct
        }
        val startParameterList = start.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        val endParameterList = end.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        if (startParameterList.typeList.effectivelyIdenticalTo(endParameterList.typeList)) {
            return topType
        }
        // types otherwise must be equal to countedLoop(iterations, init, body)
        return countedLoop(startExpr, initExpr, bodyExpr, block)
    }

    fun doWhileLoop(
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        predExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val init = ssaAnalyzer.methodHandleType(initExpr, block) ?: bottomType
        val body = ssaAnalyzer.methodHandleType(bodyExpr, block) ?: bottomType
        val pred = ssaAnalyzer.methodHandleType(predExpr, block) ?: bottomType
        if (!pred.returnType.canBe(PsiTypes.booleanType())) {
            return topType
        }
        val internalParams = (body as? CompleteMethodHandleType)?.parameterTypes ?: return body
        if (internalParams != ((pred as? CompleteMethodHandleType)?.parameterTypes ?: return pred)) {
            return topType
        }
        val returnType = init.returnType.join(body.returnType)
        if (returnType == TopType) {
            return topType
        }
        if (body.returnType.canBe(PsiTypes.voidType())) {
            if (init != body) return topType
        } else {
            if (internalParams.hasSize(0) == TriState.YES) return topType
            if (internalParams[0].join(body.returnType) == TopType) return topType
            if (init !is CompleteMethodHandleType) return init
            if (init.parameterTypes != internalParams.dropFirst(1)) return topType
        }
        // TODO this is not always correct due to effectively identical parameter lists
        return init
    }

    fun dropArguments(
        targetExpr: PsiExpression,
        posExpr: PsiExpression,
        valueTypes: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val types = valueTypes.mapToTypes(::warnOnVoid)
        val pos = posExpr.nonNegativeInt() ?: return TopMethodHandleType
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        if (target.parameterTypes.compareSize(pos) == PartialOrder.LT) {
            return emitOutOfBounds(target.parameterTypes.sizeOrNull()?.toLong(), posExpr, pos.toLong(), false)
        }
        val list = target.parameterTypes.addAllAt(pos, CompleteTypeList(types))
        return target.withParameterTypes(list)
    }

    // fun dropArgumentsToMatch()

    // fun dropCoordinates() no VarHandle support, preview

    fun dropReturn(targetExpr: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: topType
        if (target.returnType == ExactType.voidType) {
            emitRedundant(
                targetExpr.parentOfType<PsiMethodCallExpression>()!!,
                message("problem.transforming.dropReturn.redundant"),
                RedundantInvocationFix()
            )
        }
        return target.withReturnType(ExactType.voidType)
    }

    fun explicitCastArguments(
        targetExpr: PsiExpression,
        newTypeExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        val newType = ssaAnalyzer.methodHandleType(newTypeExpr, block) ?: bottomType
        // TODO proper handling of bottom/top
        val newTypeParameterList = newType.parameterTypes
        if (newTypeParameterList !is CompleteTypeLatticeElementList) return newType // result param types unknown
        val targetParameterList = target.parameterTypes
        if (targetParameterList !is CompleteTypeLatticeElementList) return target // source param types unknown
        if (targetParameterList.size != newTypeParameterList.size) return topType
        return newType
    }

    fun filterArguments(target: MethodHandleType, pos: Int, filters: List<MethodHandleType>): MethodHandleType {
        // TODO proper handling of bottom/top
        if (target !is CompleteMethodHandleType) return target
        if (filters.any { it !is CompleteMethodHandleType }) return topType
        val targetSignature = target
        val targetParameterList = targetSignature.parameterTypes
        if (targetParameterList.sizeMatches { pos + filters.size > it } == TriState.YES) return topType
        val filtersCast = filters.map { it as CompleteMethodHandleType }
        // filters must be unary operators T1 -> T2
        if (filtersCast.any { it.parameterTypes.hasSize(1) == TriState.NO }) return topType
        // return type of filters[i] must be type of targetParameters[i + pos]
        val targetParams = (targetParameterList as? CompleteTypeLatticeElementList ?: return topType).typeList
        if (targetParams.subList(pos)
                .zip(filtersCast)
                .any { (psiType, mhType) -> psiType.join(mhType.returnType) == TopType }
        )
            return topType
        // replace parameter types in range of [pos, pos + filters.size)
        val result = targetParams.mapIndexed { index, psiType ->
            if (index >= pos && index - pos < filters.size) filtersCast[index - pos].parameterTypes[0]
            else psiType
        }
        return targetSignature.withParameterTypes(result)
    }

    // fun filterCoordinates() no VarHandle support, preview

    fun filterReturnValue(
        targetExpr: PsiExpression,
        filterExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: TopMethodHandleType
        val filter = ssaAnalyzer.methodHandleType(filterExpr, block) ?: TopMethodHandleType
        val compareTo0 = filter.parameterTypes.compareSize(0)
        val compareTo1 = filter.parameterTypes.compareSize(1)
        when (target.returnType.match(PsiTypes.voidType())) {
            TriState.YES -> if (compareTo0 == PartialOrder.GT) {
                // filter expects parameters when target returns void...
                return emitProblem(filterExpr, message("problem.merging.filterReturnValue.voidButParameters"))
            }

            TriState.UNKNOWN -> if (compareTo1 == PartialOrder.GT) {
                return emitProblem(
                    filterExpr,
                    message("problem.merging.filterReturnValue.filterMustHaveAtMostOneParameter")
                )
            }

            TriState.NO -> {
                if (compareTo0 == PartialOrder.EQ) {
                    return emitProblem(
                        filterExpr,
                        message("problem.merging.filterReturnValue.filterMustHaveParameterOfType", target.returnType)
                    )
                } else if (compareTo1 == PartialOrder.GT) {
                    return emitProblem(
                        filterExpr,
                        message("problem.merging.filterReturnValue.filterMustHaveAtMostOneParameter")
                    )
                }
                val filterParameter = filter.parameterTypeAt(0)
                if (filterParameter.joinIdentical(target.returnType).second == TriState.NO) {
                    return emitProblem(
                        filterExpr,
                        message(
                            "problem.merging.filterReturnValue.incompatibleFilterParameter",
                            filterParameter,
                            target.returnType
                        )
                    )
                }
            }
        }
        return target.withReturnType(filter.returnType)
    }

    // filterValue() no VarHandle support, preview

    fun foldArguments(target: MethodHandleType, pos: Int, combiner: MethodHandleType): MethodHandleType {
        // TODO proper handling of bottom/top
        val targetSignature = target // (Z..., V, A[N]..., B...)T, where N = |combiner.parameters|
        val combinerSignature = combiner // (A...)V
        if (targetSignature.parameterTypes.sizeMatches { pos >= it } == TriState.YES) return topType
        val combinerIsVoid = combinerSignature.returnType.match(PsiTypes.voidType())
        val combinerParameterList =
            combinerSignature.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        val sub: List<Type> = when (combinerIsVoid) {
            TriState.YES -> combinerParameterList.typeList
            TriState.NO -> listOf(combinerSignature.returnType) + combinerParameterList.typeList
            TriState.UNKNOWN -> return target.withParameterTypes(TopTypeList)
        }
        // TODO type-check sub with existing list
        var newParameters = targetSignature.parameterTypes
        if (combinerIsVoid == TriState.NO) {
            newParameters = newParameters.removeAt(pos)
        }
        return targetSignature.withParameterTypes(newParameters)
    }

    fun guardWithTest(
        testExpr: PsiExpression,
        targetExpr: PsiExpression,
        fallbackExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val test = ssaAnalyzer.methodHandleType(testExpr, block) ?: bottomType
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        val fallback = ssaAnalyzer.methodHandleType(fallbackExpr, block) ?: bottomType
        // TODO proper handling of bottom/top
        val testSignature = test // (A...)boolean
        val targetSignature = target // (A... B...)T
        val fallbackSignature = fallback // (A... B...)T
        if (!testSignature.returnType.canBe(PsiTypes.booleanType())) return topType
        if (targetSignature != fallbackSignature) return topType
        val testParameterList = testSignature.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        val targetParameterList = targetSignature.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        if (testParameterList.size > targetParameterList.size) return topType
        if (testParameterList.typeList != targetParameterList.typeList.subList(
                0,
                testParameterList.size
            )
        ) {
            return topType
        }
        return targetSignature
    }

    fun insertArguments(
        targetExpr: PsiExpression,
        posExpr: PsiExpression,
        valueTypes: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        val pos = posExpr.nonNegativeInt() ?: return topType
        val parameterList = target.parameterTypes
        if (parameterList.compareSize(pos + valueTypes.size) == PartialOrder.LT) {
            // the index of the first value that is out of bounds
            val valueTypesIndex = parameterList.sizeOrNull()?.minus(pos)
            val expr = valueTypesIndex?.let { valueTypes.getOrNull(it) }
            if (expr != null) {
                return emitProblem(expr, message("problem.general.position.invalidIndexOffset", valueTypesIndex + pos))
            }
            return emitOutOfBounds(parameterList.sizeOrNull()?.toLong(), posExpr, pos.toLong(), false)
        }
        val new = parameterList.removeAt(pos, valueTypes.size)
        return target.withParameterTypes(new)
    }

    fun iteratedLoop(iterator: MethodHandleType, init: MethodHandleType, body: MethodHandleType): MethodHandleType =
        TODO()

    fun loop(clauses: List<List<MethodHandleType>>): MethodHandleType =
        TODO() // not sure if we can do anything about this

    fun permuteArguments(
        targetExpr: PsiExpression,
        newTypeExpr: PsiExpression,
        reorder: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: bottomType
        val newType = ssaAnalyzer.methodHandleType(newTypeExpr, block) ?: bottomType
        if (target.returnType.join(newType.returnType) == TopType) {
            return emitIncompatibleReturnTypes(targetExpr, target.returnType, newType.returnType)
        }
        val outParams = target.parameterTypes
        val inParams = newType.parameterTypes
        if (outParams.sizeMatches { it != reorder.size } == TriState.YES) {
            return emitProblem(
                newTypeExpr,
                message("problem.merging.permute.reorderLengthMismatch", reorder.size, outParams.sizeOrNull()!!)
            )
        }
        // if reorder array is unknown, just assume the input is correct
        val reorderInts = reorder.map { it.nonNegativeInt() ?: return newType }
        val resultType: MutableList<Type> = (newType.parameterTypes as? CompleteTypeLatticeElementList)
            ?.typeList?.toMutableList()
            ?: return topType
        for ((index, value) in reorderInts.withIndex()) {
            if (inParams.compareSize(value + 1) == PartialOrder.LT) {
                emitProblem<MethodHandleType>(
                    reorder[index],
                    message("problem.merging.permute.invalidReorderIndex", 0, inParams.sizeOrNull()!!, value)
                )
                resultType[index] = TopType
            } else if (outParams[index] != inParams[value]) {
                emitProblem<MethodHandleType>(
                    reorder[index],
                    message(
                        "problem.merging.permute.invalidReorderIndexType",
                        outParams[index],
                        inParams[value]
                    )
                )
                resultType[index] = TopType
            }
        }
        return complete(newType.returnType, resultType)
    }

    // fun permuteCoordinates() no VarHandle support, preview

    fun tableSwitch(
        fallbackExpr: PsiExpression,
        targetsExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        if (targetsExprs.isEmpty()) {
            emitProblem<MethodHandleType>(fallbackExpr.parent, message("problem.merging.tableSwitch.noCases"))
        }
        val fallback = ssaAnalyzer.methodHandleType(fallbackExpr, block) ?: bottomType
        var error = checkFirstParameter(fallback.parameterTypes, fallbackExpr)
        val targets = targetsExprs.map { ssaAnalyzer.methodHandleType(it, block) ?: bottomType }
        for ((index, target) in targets.withIndex()) {
            error = error or checkFirstParameter(target.parameterTypes, targetsExprs[index])
        }
        val cases = targets.map { it }
        var prev = fallback
        for ((index, case) in cases.withIndex()) {
            val (signature, identical) = prev.joinIdentical(case)
            if (identical == TriState.NO) {
                emitProblem<MethodHandleType>(targetsExprs[index], message("problem.merging.tableSwitch.notIdentical"))
                error = true
            }
            prev = signature
        }
        if (error) {
            return TopMethodHandleType
        }
        return prev
    }

    private fun checkFirstParameter(typeList: TypeList, context: PsiExpression): Boolean {
        if (typeList.compareSize(1) == PartialOrder.LT
            || typeList[0].match(PsiTypes.intType()) == TriState.NO
        ) {
            emitProblem<MethodHandleType>(context, message("problem.merging.tableSwitch.leadingInt"))
            return true
        }
        return false
    }

    fun tryFinally(
        targetExpr: PsiExpression,
        cleanupExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.methodHandleType(targetExpr, block) ?: topType
        val cleanup = ssaAnalyzer.methodHandleType(cleanupExpr, block) ?: topType
        val (newReturn, identical) = cleanup.returnType.joinIdentical(target.returnType)
        if (identical == TriState.NO) {
            emitProblem<MethodHandleType>(
                cleanupExpr,
                message("problem.merging.tryFinally.returnTypeMismatch", target.returnType, cleanup.returnType)
            )
        }
        val isVoid = target.returnType.match(PsiTypes.voidType())
        val leading = when (isVoid) {
            TriState.YES -> 1
            TriState.NO -> 2
            TriState.UNKNOWN -> return complete(newReturn, TopTypeList)
        }
        if (cleanup.parameterTypes.sizeMatches { it < leading } == TriState.YES) {
            emitProblem<MethodHandleType>(
                cleanupExpr,
                message(
                    "problem.merging.tryFinally.missingCleanupParameters",
                    leading,
                    cleanup.parameterTypes.sizeOrNull() ?: 0
                )
            )
        }
        // TODO 0th param must be <= Throwable
        if (isVoid == TriState.NO && cleanup.parameterTypes[1].joinIdentical(newReturn).second == TriState.NO) {
            emitProblem<MethodHandleType>(
                cleanupExpr,
                message("problem.merging.tryFinally.secondParameter", cleanup.parameterTypes[1], newReturn)
            )
        }
        val aList = cleanup.parameterTypes.dropFirst(leading) as? CompleteTypeLatticeElementList ?: return topType
        val targetParameterList = target.parameterTypes as? CompleteTypeLatticeElementList ?: return topType
        if (!targetParameterList.typeList.startsWith(aList.typeList)) return topType
        return target
    }

    fun whileLoop(
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        predExpr: PsiExpression,
        block: SsaConstruction.Block
    ) = doWhileLoop(initExpr, bodyExpr, predExpr, block) // same type behavior

    fun PsiExpression.nonNegativeInt(): Int? {
        return this.getConstantOfType<Int>()?.let {
            if (it < 0) {
                emitProblem<MethodHandleType>(this, message("problem.general.position.invalidIndexNegative", it))
                return null
            }
            return it
        }
    }

}

private fun TypeList.effectivelyIdenticalTo(other: TypeList): Boolean {
    if (this is CompleteTypeList && other is CompleteTypeList) {
        return typeList.effectivelyIdenticalTo(other.typeList)
    }
    return false
}

private fun <E> List<E>.startsWith(list: List<E>): Boolean {
    if (list.size > this.size) return false
    return this.subList(0, list.size) == list
}

