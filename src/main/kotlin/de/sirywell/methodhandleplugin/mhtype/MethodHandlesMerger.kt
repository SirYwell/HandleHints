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
        if (target.signature !is CompleteSignature) return target
        if (handler.signature !is CompleteSignature) return handler
        val handlerParameterTypes = handler.signature.parameterTypes
        if (handlerParameterTypes.isEmpty()
            || (exType is DirectType && !handlerParameterTypes[0].canBe(exType.psiType))
        ) {
            emitProblem(handlerExpr, message("problem.merging.catchException.missingException", exType))
        }
        val returnType = if (target.signature.returnType() != handler.signature.returnType()) {
            incompatibleReturnTypes(targetExpr, target.signature.returnType(), handler.signature.returnType())
            TopType
        } else {
            target.signature.returnType
        }
        val comparableTypes = if (handlerParameterTypes.isEmpty()) {
            handlerParameterTypes
        } else {
            handlerParameterTypes.subList(1)
        }
        // TODO calculate common type of parameters
        if (!comparableTypes.effectivelyIdenticalTo(target.signature.parameterTypes)) {
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
        if (target.signature !is CompleteSignature) return target
        if (filter.signature !is CompleteSignature) return topType
        val parameters = target.signature.parameterTypes.toMutableList()
        val pos = posExpr.getConstantOfType<Int>() ?: return bottomType
        if (pos >= parameters.size || pos < 0) {
            return emitProblem(posExpr, message("problem.merging.collectArgs.invalidIndex", pos, parameters.size))
        }
        val returnType = filter.signature.returnType
        if (returnType is DirectType && returnType.psiType != PsiTypes.voidType()) {
            // the return type of the filter must match the replaced type
            if (parameters[pos] != returnType) {
                return topType
            }
            // the return value of filter will be passed to target at pos
            parameters.removeAt(pos)
        }
        val filterParameters = filter.signature.parameterTypes
        parameters.addAll(pos, filterParameters)
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
            iterations = init?.signature?.withReturnType(DirectType(PsiTypes.intType()))?.let { MethodHandleType(it) }
                ?: return bottomType
        }
        if (init == null || init.signature is BotSignature) {
            // just assume they are both the same (besides return type) for the rest of the logic
            val returnType = body.signature.returnType()
            init = MethodHandleType(iterations.signature.withReturnType(returnType))
        }
        if (iterations.signature is TopSignature || body.signature is TopSignature || init.signature is TopSignature) {
            return topType
        }
        if (!iterations.signature.returnType().canBe(PsiTypes.intType())) {
            return topType
        }
        if (body.signature is CompleteSignature) {
            val externalParameters: List<Type>
            val internalParameters = body.signature.parameterTypes
            val returnType = body.signature.returnType()
            if (init.signature is CompleteSignature) {
                if (returnType != (init.signature as CompleteSignature).returnType()) {
                    return topType
                }
            }
            if (returnType.canBe(PsiTypes.voidType())) {
                // (I A...)
                val size = internalParameters.size
                if (internalParameters.isEmpty() || !internalParameters[0].canBe(PsiTypes.intType())) {
                    return topType
                }
                externalParameters = if (size != 1) {
                    internalParameters.subList(1)
                } else {
                    (iterations.signature as CompleteSignature).parameterTypes
                }
            } else {
                // (V I A...)
                val size = internalParameters.size
                if (size < 2 || (internalParameters[0] != returnType || !internalParameters[1].canBe(PsiTypes.intType()))) {
                    return topType
                }
                externalParameters = if (size != 2) {
                    internalParameters.subList(2)
                } else {
                    (iterations.signature as CompleteSignature).parameterTypes
                }
            }
            if (!(init.signature as CompleteSignature).parameterTypes.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
            if (!(iterations.signature as CompleteSignature).parameterTypes.effectivelyIdenticalTo(externalParameters)) {
                return topType
            }
        }
        if (!iterations.signature.returnType().canBe(PsiTypes.intType())) {
            return topType
        }
        // this might not be very precise
        return MethodHandleType(iterations.signature.withReturnType(init.signature.returnType()))
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
        if (start.signature.returnType().join(end.signature.returnType()) == TopType) {
            return incompatibleReturnTypes(startExpr, start.signature.returnType(), end.signature.returnType())
        }
        if (start.signature !is CompleteSignature || end.signature !is CompleteSignature) {
            return bottomType // TODO not correct
        }
        if (start.signature.parameterTypes.effectivelyIdenticalTo(end.signature.parameterTypes)) {
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
        if (!pred.signature.returnType().canBe(PsiTypes.booleanType())) {
            return topType
        }
        val internalParams = (body.signature as? CompleteSignature)?.parameterTypes ?: return body
        if (internalParams != ((pred.signature as? CompleteSignature)?.parameterTypes ?: return pred)) {
            return topType
        }
        val returnType = init.signature.returnType().join(body.signature.returnType())
        if (returnType == TopType) {
            return topType
        }
        if (body.signature.returnType().canBe(PsiTypes.voidType())) {
            if (init.signature != body.signature) return topType
        } else {
            if (internalParams.isEmpty()) return topType
            if (internalParams[0].join(body.signature.returnType()) == TopType) return topType
            if (init.signature !is CompleteSignature) return init
            if (init.signature.parameterTypes != internalParams.subList(1, internalParams.size)) return topType
        }
        // TODO this is not always correct due to effectively identical parameter lists
        return MethodHandleType(init.signature)
    }

    fun dropArguments(target: MethodHandleType, pos: Int, valueTypes: List<PsiExpression>): MethodHandleType {
        val types = valueTypes.mapToTypes()
        if (target.signature !is CompleteSignature) return target
        val signature = target.signature
        if (pos > signature.parameterTypes.size) return topType
        val list = signature.parameterTypes.toMutableList()
        list.addAll(pos, types)
        return MethodHandleType(signature.withParameterTypes(list))
    }

    // fun dropArgumentsToMatch()

    // fun dropCoordinates() no VarHandle support, preview

    fun dropReturn(targetExpr: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        if (target.signature is CompleteSignature) {
            return MethodHandleType(target.signature.withReturnType(DirectType(PsiTypes.voidType())))
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
        if (target.signature !is CompleteSignature) return target
        if (newType.signature !is CompleteSignature) return newType
        if (target.signature.parameterTypes.size != newType.signature.parameterTypes.size) return topType
        return newType
    }

    fun filterArguments(target: MethodHandleType, pos: Int, filters: List<MethodHandleType>): MethodHandleType {
        // TODO proper handling of bottom/top
        if (target.signature !is CompleteSignature) return target
        if (filters.any { it.signature !is CompleteSignature }) return topType
        val targetSignature = target.signature
        if (pos + filters.size > targetSignature.parameterTypes.size) return topType
        val filtersCast = filters.map { it.signature as CompleteSignature }
        // filters must be unary operators T1 -> T2
        if (filtersCast.any { it.parameterTypes.size != 1 }) return topType
        // return type of filters[i] must be type of targetParameters[i + pos]
        if (targetSignature.parameterTypes.subList(pos)
                .zip(filtersCast)
                .any { (psiType, mhType) -> psiType.join(mhType.returnType) == TopType }
        )
            return topType
        // replace parameter types in range of [pos, pos + filters.size)
        val result = targetSignature.parameterTypes.mapIndexed { index, psiType ->
            if (index >= pos && index - pos < filters.size) filtersCast[index - pos].parameterTypes[0]
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
        if (target.signature !is CompleteSignature) return target
        if (filter.signature !is CompleteSignature || filter.signature.parameterTypes.size != 1) return topType
        if (target.signature.returnType.join(filter.signature.parameterTypes[0]) == TopType) return topType
        return MethodHandleType(target.signature.withReturnType(filter.signature.returnType))
    }

    // filterValue() no VarHandle support, preview

    fun foldArguments(target: MethodHandleType, pos: Int, combiner: MethodHandleType): MethodHandleType {
        // TODO proper handling of bottom/top
        if (target.signature !is CompleteSignature) return target
        if (combiner.signature !is CompleteSignature) return combiner
        val targetSignature = target.signature // (Z..., V, A[N]..., B...)T, where N = |combiner.parameters|
        val combinerSignature = combiner.signature // (A...)V
        if (pos >= targetSignature.parameterTypes.size) return topType
        // TODO if not direct type -> top?
        val combinerIsVoid =
            combinerSignature.returnType is DirectType && combinerSignature.returnType.psiType == PsiTypes.voidType()
        val sub: List<Type> = if (combinerIsVoid) {
            listOf(*combinerSignature.parameterTypes.toTypedArray())
        } else {
            listOf(combinerSignature.returnType, *combinerSignature.parameterTypes.toTypedArray())
        }
        val listStart = subListStart(targetSignature.parameterTypes, sub) ?: return topType
        val newParameters = targetSignature.parameterTypes.toMutableList()
        if (!combinerIsVoid) {
            newParameters.removeAt(listStart)
        }
        return MethodHandleType(targetSignature.withParameterTypes(newParameters))
    }

    private fun <T> subListStart(parameters: List<T>, sub: List<T>): Int? {
        // compare all windows with sub, pick window start of first match
        return parameters.windowed(sub.size) { it }
            .flatMapIndexed { index, window -> if (window == sub) listOf(index) else listOf() }
            .firstOrNull()
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
        if (test.signature !is CompleteSignature) return test
        if (target.signature !is CompleteSignature) return target
        if (fallback.signature !is CompleteSignature) return fallback
        val testSignature = test.signature // (A...)boolean
        val targetSignature = target.signature // (A... B...)T
        val fallbackSignature = fallback.signature // (A... B...)T
        if (!testSignature.returnType.canBe(PsiTypes.booleanType())) return topType
        if (targetSignature != fallbackSignature) return topType
        if (testSignature.parameterTypes.size > targetSignature.parameterTypes.size) return topType
        if (testSignature.parameterTypes != targetSignature.parameterTypes.subList(
                0,
                testSignature.parameterTypes.size
            )
        ) {
            return topType
        }
        return MethodHandleType(targetSignature)
    }

    fun insertArguments(target: MethodHandleType, pos: Int, valueTypes: List<PsiExpression>): MethodHandleType {
        if (target.signature !is CompleteSignature) return target
        val list = target.signature.parameterTypes.toMutableList()
        if (list.size < pos + valueTypes.size) return topType
        list.subList(pos, pos + valueTypes.size).clear() // drop
        return MethodHandleType(target.signature.withParameterTypes(list))
    }

    fun iteratedLoop(iterator: MethodHandleType, init: MethodHandleType, body: MethodHandleType): MethodHandleType = TODO()

    fun loop(clauses: List<List<MethodHandleType>>): MethodHandleType = TODO() // not sure if we can do anything about this

    fun permuteArguments(
        targetExpr: PsiExpression,
        newTypeExpr: PsiExpression,
        reorder: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val newType = ssaAnalyzer.mhType(newTypeExpr, block) ?: bottomType
        if (target.signature.returnType().join(newType.signature.returnType()) == TopType) {
            return incompatibleReturnTypes(targetExpr, target.signature.returnType(), newType.signature.returnType())
        }
        if (target.signature is CompleteSignature && newType.signature is CompleteSignature) {
            val outParams = target.signature.parameterTypes
            val nOut = outParams.size
            val inParams = newType.signature.parameterTypes
            val nIn = inParams.size
            if (nOut != reorder.size) {
                return emitProblem(
                    newTypeExpr,
                    message("problem.merging.permute.reorderLengthMismatch", reorder.size, nOut)
                )
            }
            // if reorder array is unknown, just assume the input is correct
            val reorderInts = reorder.map { it.getConstantOfType<Int>() ?: return newType }
            val resultType: MutableList<Type> = newType.signature.parameterTypes.toMutableList()
            for ((index, value) in reorderInts.withIndex()) {
                if (value < 0 || value >= nIn) {
                    emitProblem(reorder[index], message("problem.merging.permute.invalidReorderIndex", 0, nIn, value))
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
            return MethodHandleType(CompleteSignature(newType.signature.returnType, resultType))
        }
        if (target.signature is TopSignature || newType.signature is TopSignature) {
            return topType
        }
        return bottomType
    }

    // fun permuteCoordinates() no VarHandle support, preview

    fun tableSwitch(
        fallbackExpr: PsiExpression,
        targetsExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val fallback = ssaAnalyzer.mhType(fallbackExpr, block) ?: bottomType
        val targets = targetsExprs.map { ssaAnalyzer.mhType(it, block) ?: bottomType }
        if (fallback.signature is TopSignature) return topType
        if (targets.any { it.signature is TopSignature }) return topType
        val cases = targets.map { it.signature }
        // TODO actually join all signatures
        // TODO ensure first param is int
        if (cases.any { it.join(fallback.signature) == TopSignature }) return topType
        return MethodHandleType(fallback.signature)
    }

    fun tryFinally(
        targetExpr: PsiExpression,
        cleanupExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val target = ssaAnalyzer.mhType(targetExpr, block) ?: bottomType
        val cleanup = ssaAnalyzer.mhType(cleanupExpr, block) ?: bottomType
        if (target.signature is BotSignature || cleanup.signature is BotSignature) return bottomType
        if (target.signature !is CompleteSignature) return topType
        if (cleanup.signature !is CompleteSignature) return topType
        val cleanupSignature = cleanup.signature
        val targetSignature = target.signature
        // TODO should use join
        if (cleanupSignature.returnType != targetSignature.returnType) return topType
        val isVoid =
            targetSignature.returnType is DirectType && targetSignature.returnType.psiType == PsiTypes.voidType()
        val leading = if (isVoid) 1 else 2
        if (cleanupSignature.parameterTypes.size < leading) return topType
        // TODO 0th param must be <= Throwable
        if (!isVoid && cleanupSignature.parameterTypes[1] != cleanupSignature.returnType) return topType
        val aList = cleanupSignature.parameterTypes.subList(leading)
        if (!targetSignature.parameterTypes.startsWith(aList)) return topType
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
}

private fun <E> List<E>.startsWith(list: List<E>): Boolean {
    if (list.size > this.size) return false
    return this.subList(0, list.size) == list
}
