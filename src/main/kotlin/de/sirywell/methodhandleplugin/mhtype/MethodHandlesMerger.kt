package de.sirywell.methodhandleplugin.mhtype

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiTypes
import de.sirywell.methodhandleplugin.*
import de.sirywell.methodhandleplugin.MethodHandleBundle.message
import de.sirywell.methodhandleplugin.dfa.SsaAnalyzer
import de.sirywell.methodhandleplugin.dfa.SsaConstruction
import de.sirywell.methodhandleplugin.type.*
import de.sirywell.methodhandleplugin.type.TopType
import org.jetbrains.annotations.Nls

/**
 * Contains methods to merge multiple [MethodHandleType]s into a new one,
 * provided by [java.lang.invoke.MethodHandles]
 */
class MethodHandlesMerger(private val ssaAnalyzer: SsaAnalyzer) {

    private val bottomType = MethodHandleType(BotSignature)
    private val topType = MethodHandleType(TopSignature)

    // TODO effectively identical sequences for loops???

    fun catchException(
        targetExpr: PsiExpression,
        exTypeExpr: PsiExpression,
        handlerExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val exType = exTypeExpr.asType()
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val handler = ssaAnalyzer.mhType(handlerExpr, block) ?: bottomType
        val handlerParameterTypes = handler.signature.parameterList
        if (handlerParameterTypes is CompleteParameterList && (handlerParameterTypes.size == 0
                    || (exType is ExactType && !handlerParameterTypes[0].canBe(exType.psiType)))
        ) {
            emitProblem(handlerExpr, message("problem.merging.catchException.missingException", exType))
        }
        val returnType = if (target.signature.returnType != handler.signature.returnType) {
            incompatibleReturnTypes(targetExpr, target.signature.returnType, handler.signature.returnType)
            TopType
        } else {
            target.signature.returnType
        }
        val comparableTypes = handlerParameterTypes.dropFirst(1)
        // TODO calculate common type of parameters
        if (!comparableTypes.effectivelyIdenticalTo(target.signature.parameterList)) {
            emitProblem(
                targetExpr,
                message(
                    "problem.merging.general.effectivelyIdenticalParametersExpected",
                    target.signature,
                    comparableTypes
                )
            )
        }
        return MethodHandleType(target.signature.withReturnType(returnType))
    }

    fun collectArguments(
        targetExpr: PsiExpression,
        posExpr: PsiExpression,
        filterExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: return bottomType
        val filter = ssaAnalyzer.mhType(filterExpr, block) ?: return bottomType
        var parameters = target.signature.parameterList
        val pos = posExpr.nonNegativeInt() ?: return bottomType
        if (parameters is CompleteParameterList && pos >= parameters.size) {
            return emitProblem(
                posExpr,
                message("problem.general.position.invalidIndexKnownBoundsExcl", pos, parameters.size)
            )
        }
        val returnType = filter.signature.returnType
        if (returnType is ExactType && returnType.psiType != PsiTypes.voidType()) {
            // the return type of the filter must match the replaced type
            if (parameters[pos] != returnType) {
                return topType
            }
            // the return value of filter will be passed to target at pos
            parameters = parameters.removeAt(pos)
        }
        val filterParameters = filter.signature.parameterList
        parameters = parameters.addAllAt(pos, filterParameters)
        return MethodHandleType(target.signature.withParameterTypes(parameters))
    }

    // fun collectCoordinates() no VarHandle support, preview

    fun countedLoop(
        iterationsExpr: PsiExpression,
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        var iterations = ssaAnalyzer.mhType(iterationsExpr, block) ?: bottomType
        var init = ssaAnalyzer.mhType(initExpr, block)
        val body = ssaAnalyzer.mhType(bodyExpr, block) ?: bottomType
        if (iterations.signature is BotSignature) {
            // iterations and init have an effectively identical parameter list
            // so if one is missing, just use the other, but assert return type
            iterations = init?.signature?.withReturnType(ExactType.intType)?.let { MethodHandleType(it) }
                ?: return bottomType
        }
        if (init == null || init.signature is BotSignature) {
            // just assume they are both the same (besides return type) for the rest of the logic
            val returnType = body.signature.returnType
            init = MethodHandleType(iterations.signature.withReturnType(returnType))
        }
        if (iterations.signature is TopSignature || body.signature is TopSignature || init.signature is TopSignature) {
            return topType
        }
        if (!iterations.signature.returnType.canBe(PsiTypes.intType())) {
            return topType
        }
        if (body.signature is CompleteSignature) {
            val externalParameters: ParameterList
            val internalParameters = body.signature.parameterList
            val returnType = body.signature.returnType
            if (init.signature is CompleteSignature) {
                if (returnType != (init.signature as CompleteSignature).returnType) {
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
                    (iterations.signature as CompleteSignature).parameterList
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
                    (iterations.signature as CompleteSignature).parameterList
                }
            }
            if (!(init.signature as CompleteSignature).parameterList.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
            if (!(iterations.signature as CompleteSignature).parameterList.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
        }
        if (!iterations.signature.returnType.canBe(PsiTypes.intType())) {
            return topType
        }
        // this might not be very precise
        return MethodHandleType(iterations.signature.withReturnType(init.signature.returnType))
    }

    fun countedLoop(
        startExpr: PsiExpression,
        endExpr: PsiExpression,
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val start = ssaAnalyzer.mhType(startExpr, block) ?: bottomType
        val end = ssaAnalyzer.mhType(endExpr, block) ?: bottomType
        if (start.signature.returnType.join(end.signature.returnType) == TopType) {
            return incompatibleReturnTypes(startExpr, start.signature.returnType, end.signature.returnType)
        }
        if (start.signature !is CompleteSignature || end.signature !is CompleteSignature) {
            return bottomType // TODO not correct
        }
        val startParameterList = start.signature.parameterList as? CompleteParameterList ?: return topType
        val endParameterList = end.signature.parameterList as? CompleteParameterList ?: return topType
        if (startParameterList.parameterTypes.effectivelyIdenticalTo(endParameterList.parameterTypes)) {
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
        val init = ssaAnalyzer.mhType(initExpr, block) ?: bottomType
        val body = ssaAnalyzer.mhType(bodyExpr, block) ?: bottomType
        val pred = ssaAnalyzer.mhType(predExpr, block) ?: bottomType
        if (!pred.signature.returnType.canBe(PsiTypes.booleanType())) {
            return topType
        }
        val internalParams = (body.signature as? CompleteSignature)?.parameterList ?: return body
        if (internalParams != ((pred.signature as? CompleteSignature)?.parameterList ?: return pred)) {
            return topType
        }
        val returnType = init.signature.returnType.join(body.signature.returnType)
        if (returnType == TopType) {
            return topType
        }
        if (body.signature.returnType.canBe(PsiTypes.voidType())) {
            if (init.signature != body.signature) return topType
        } else {
            if (internalParams.hasSize(0) == TriState.YES) return topType
            if (internalParams[0].join(body.signature.returnType) == TopType) return topType
            if (init.signature !is CompleteSignature) return init
            if (init.signature.parameterList != internalParams.dropFirst(1)) return topType
        }
        // TODO this is not always correct due to effectively identical parameter lists
        return MethodHandleType(init.signature)
    }

    fun dropArguments(
        targetExpr: PsiExpression,
        pos: Int,
        valueTypes: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val types = valueTypes.mapToTypes()
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val signature = target.signature
        if (signature.parameterList.compareSize(pos) == PartialOrder.LT) {
            return emitOutOfBounds(signature.parameterList.sizeOrNull(), targetExpr, pos, false)
        }
        val list = signature.parameterList.addAllAt(pos, CompleteParameterList(types))
        return MethodHandleType(signature.withParameterTypes(list))
    }

    // fun dropArgumentsToMatch()

    // fun dropCoordinates() no VarHandle support, preview

    fun dropReturn(targetExpr: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        if (target.signature is CompleteSignature) {
            return MethodHandleType(target.signature.withReturnType(ExactType.voidType))
        }
        return target
    }

    fun explicitCastArguments(
        targetExpr: PsiExpression,
        newTypeExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val newType = ssaAnalyzer.mhType(newTypeExpr, block) ?: bottomType
        // TODO proper handling of bottom/top
        val newTypeParameterList = newType.signature.parameterList
        if (newTypeParameterList !is CompleteParameterList) return newType // result param types unknown
        val targetParameterList = target.signature.parameterList
        if (targetParameterList !is CompleteParameterList) return target // source param types unknown
        if (targetParameterList.size != newTypeParameterList.size) return topType
        return newType
    }

    fun filterArguments(target: MethodHandleType, pos: Int, filters: List<MethodHandleType>): MethodHandleType {
        // TODO proper handling of bottom/top
        if (target.signature !is CompleteSignature) return target
        if (filters.any { it.signature !is CompleteSignature }) return topType
        val targetSignature = target.signature
        val targetParameterList = targetSignature.parameterList
        if (targetParameterList.sizeMatches { pos + filters.size > it } == TriState.YES) return topType
        val filtersCast = filters.map { it.signature as CompleteSignature }
        // filters must be unary operators T1 -> T2
        if (filtersCast.any { it.parameterList.hasSize(1) == TriState.NO }) return topType
        // return type of filters[i] must be type of targetParameters[i + pos]
        val targetParams = (targetParameterList as? CompleteParameterList ?: return topType).parameterTypes
        if (targetParams.subList(pos)
                .zip(filtersCast)
                .any { (psiType, mhType) -> psiType.join(mhType.returnType) == TopType }
        )
            return topType
        // replace parameter types in range of [pos, pos + filters.size)
        val result = targetParams.mapIndexed { index, psiType ->
            if (index >= pos && index - pos < filters.size) filtersCast[index - pos].parameterList[0]
            else psiType
        }
        return MethodHandleType(targetSignature.withParameterTypes(result))
    }

    // fun filterCoordinates() no VarHandle support, preview

    fun filterReturnValue(
        targetExpr: PsiExpression,
        filterExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val filter = ssaAnalyzer.mhType(filterExpr, block) ?: bottomType
        // TODO proper handling of bottom/top
        if (filter.signature !is CompleteSignature || filter.signature.parameterList.hasSize(1) == TriState.NO) {
            return topType
        }
        if (target.signature.returnType.join(filter.signature.parameterList[0]) == TopType) return topType
        return MethodHandleType(target.signature.withReturnType(filter.signature.returnType))
    }

    // filterValue() no VarHandle support, preview

    fun foldArguments(target: MethodHandleType, pos: Int, combiner: MethodHandleType): MethodHandleType {
        // TODO proper handling of bottom/top
        val targetSignature = target.signature // (Z..., V, A[N]..., B...)T, where N = |combiner.parameters|
        val combinerSignature = combiner.signature // (A...)V
        if (targetSignature.parameterList.sizeMatches { pos >= it } == TriState.YES) return topType
        val combinerIsVoid = combinerSignature.returnType.match(PsiTypes.voidType())
        val combinerParameterList = combinerSignature.parameterList as? CompleteParameterList ?: return topType
        val sub: List<Type> = when (combinerIsVoid) {
            TriState.YES -> combinerParameterList.parameterTypes
            TriState.NO -> listOf(combinerSignature.returnType) + combinerParameterList.parameterTypes
            TriState.UNKNOWN -> return MethodHandleType(target.signature.withParameterTypes(TopParameterList))
        }
        // TODO type-check sub with existing list
        var newParameters = targetSignature.parameterList
        if (combinerIsVoid == TriState.NO) {
            newParameters = newParameters.removeAt(pos)
        }
        return MethodHandleType(targetSignature.withParameterTypes(newParameters))
    }

    fun guardWithTest(
        testExpr: PsiExpression,
        targetExpr: PsiExpression,
        fallbackExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val test = ssaAnalyzer.mhType(testExpr, block) ?: bottomType
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val fallback = ssaAnalyzer.mhType(fallbackExpr, block) ?: bottomType
        // TODO proper handling of bottom/top
        val testSignature = test.signature // (A...)boolean
        val targetSignature = target.signature // (A... B...)T
        val fallbackSignature = fallback.signature // (A... B...)T
        if (!testSignature.returnType.canBe(PsiTypes.booleanType())) return topType
        if (targetSignature != fallbackSignature) return topType
        val testParameterList = testSignature.parameterList as? CompleteParameterList ?: return topType
        val targetParameterList = targetSignature.parameterList as? CompleteParameterList ?: return topType
        if (testParameterList.size > targetParameterList.size) return topType
        if (testParameterList.parameterTypes != targetParameterList.parameterTypes.subList(
                0,
                testParameterList.size
            )
        ) {
            return topType
        }
        return MethodHandleType(targetSignature)
    }

    fun insertArguments(
        targetExpr: PsiExpression,
        posExpr: PsiExpression,
        valueTypes: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val pos = posExpr.nonNegativeInt() ?: return topType
        val parameterList = target.signature.parameterList
        if (parameterList.compareSize(pos + valueTypes.size) == PartialOrder.LT) {
            // the index of the first value that is out of bounds
            val valueTypesIndex = parameterList.sizeOrNull()?.minus(pos)
            val expr = valueTypesIndex?.let { valueTypes.getOrNull(it) }
            if (expr != null) {
                return emitProblem(expr, message("problem.general.position.invalidIndexOffset", valueTypesIndex + pos))
            }
            return emitOutOfBounds(parameterList.sizeOrNull(), posExpr, pos, false)
        }
        val new = parameterList.removeAt(pos, valueTypes.size)
        return MethodHandleType(target.signature.withParameterTypes(new))
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
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val newType = ssaAnalyzer.mhType(newTypeExpr, block) ?: bottomType
        if (target.signature.returnType.join(newType.signature.returnType) == TopType) {
            return incompatibleReturnTypes(targetExpr, target.signature.returnType, newType.signature.returnType)
        }
        val outParams = target.signature.parameterList
        val inParams = newType.signature.parameterList
        if (outParams.sizeMatches { it != reorder.size } == TriState.YES) {
            return emitProblem(
                newTypeExpr,
                message("problem.merging.permute.reorderLengthMismatch", reorder.size, outParams.sizeOrNull()!!)
            )
        }
        // if reorder array is unknown, just assume the input is correct
        val reorderInts = reorder.map { it.nonNegativeInt() ?: return newType }
        val resultType: MutableList<Type> = (newType.signature.parameterList as? CompleteParameterList)
            ?.parameterTypes?.toMutableList()
            ?: return topType
        for ((index, value) in reorderInts.withIndex()) {
            if (inParams.compareSize(value + 1) == PartialOrder.LT) {
                emitProblem(
                    reorder[index],
                    message("problem.merging.permute.invalidReorderIndex", 0, inParams.sizeOrNull()!!, value)
                )
                resultType[index] = TopType
            } else if (outParams[index] != inParams[value]) {
                emitProblem(
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
        return MethodHandleType(complete(newType.signature.returnType, resultType))
    }

    // fun permuteCoordinates() no VarHandle support, preview

    fun tableSwitch(
        fallbackExpr: PsiExpression,
        targetsExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        if (targetsExprs.isEmpty()) {
            emitProblem(fallbackExpr.parent, message("problem.merging.tableSwitch.noCases"))
        }
        val fallback = ssaAnalyzer.mhType(fallbackExpr, block) ?: bottomType
        var error = checkFirstParameter(fallback.signature.parameterList, fallbackExpr)
        val targets = targetsExprs.map { ssaAnalyzer.mhType(it, block) ?: bottomType }
        for ((index, target) in targets.withIndex()) {
            error = error or checkFirstParameter(target.signature.parameterList, targetsExprs[index])
        }
        val cases = targets.map { it.signature }
        var prev = fallback.signature
        for ((index, case) in cases.withIndex()) {
            val (signature, identical) = prev.joinIdentical(case)
            if (identical == TriState.NO) {
                emitProblem(targetsExprs[index], message("problem.merging.tableSwitch.notIdentical"))
                error = true
            }
            prev = signature
        }
        if (error) {
            return MethodHandleType(TopSignature)
        }
        return MethodHandleType(prev)
    }

    private fun checkFirstParameter(parameterList: ParameterList, context: PsiExpression): Boolean {
        if (parameterList.compareSize(1) == PartialOrder.LT
            || parameterList[0].match(PsiTypes.intType()) == TriState.NO
        ) {
            emitProblem(context, message("problem.merging.tableSwitch.leadingInt"))
            return true
        }
        return false
    }

    fun tryFinally(
        targetExpr: PsiExpression,
        cleanupExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val cleanup = ssaAnalyzer.mhType(cleanupExpr, block) ?: bottomType
        if (target.signature is BotSignature || cleanup.signature is BotSignature) return bottomType
        val cleanupSignature = cleanup.signature
        val targetSignature = target.signature
        // TODO should use join
        if (cleanupSignature.returnType != targetSignature.returnType) return topType
        val isVoid = targetSignature.returnType.match(PsiTypes.voidType())
        val leading = when (isVoid) {
            TriState.YES -> 1
            TriState.NO -> 2
            TriState.UNKNOWN -> return MethodHandleType(complete(targetSignature.returnType, TopParameterList))
        }
        if (cleanupSignature.parameterList.sizeMatches { it < leading } == TriState.YES) return topType
        // TODO 0th param must be <= Throwable
        if (isVoid == TriState.NO && cleanupSignature.parameterList[1] != cleanupSignature.returnType) return topType
        val aList = cleanupSignature.parameterList.dropFirst(leading) as? CompleteParameterList ?: return topType
        val targetParameterList = targetSignature.parameterList as? CompleteParameterList ?: return topType
        if (!targetParameterList.parameterTypes.startsWith(aList.parameterTypes)) return topType
        return MethodHandleType(targetSignature)
    }

    fun whileLoop(
        initExpr: PsiExpression,
        bodyExpr: PsiExpression,
        predExpr: PsiExpression,
        block: SsaConstruction.Block
    ) = doWhileLoop(initExpr, bodyExpr, predExpr, block) // same type behavior

    private fun incompatibleReturnTypes(
        element: PsiElement,
        first: Type,
        second: Type
    ): MethodHandleType {
        return emitProblem(
            element, message(
                "problem.merging.general.incompatibleReturnType",
                first,
                second
            )
        )
    }

    private fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        ssaAnalyzer.typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return topType
    }

    private fun emitOutOfBounds(
        size: Int?,
        targetExpr: PsiExpression,
        pos: Int,
        exclusive: Boolean
    ) = if (size != null) {
        if (exclusive) {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsExcl", pos, size))
        } else {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsIncl", pos, size))
        }
    } else {
        emitProblem(targetExpr, message("problem.general.position.invalidIndex", pos))
    }

    fun PsiExpression.nonNegativeInt(): Int? {
        return this.getConstantOfType<Int>()?.let {
            if (it < 0) {
                emitProblem(this, message("problem.general.position.invalidIndexNegative", it))
                return null
            }
            return it
        }
    }

}

private fun ParameterList.effectivelyIdenticalTo(other: ParameterList): Boolean {
    if (this is CompleteParameterList && other is CompleteParameterList) {
        return parameterTypes.effectivelyIdenticalTo(other.parameterTypes)
    }
    return false
}

private fun <E> List<E>.startsWith(list: List<E>): Boolean {
    if (list.size > this.size) return false
    return this.subList(0, list.size) == list
}

