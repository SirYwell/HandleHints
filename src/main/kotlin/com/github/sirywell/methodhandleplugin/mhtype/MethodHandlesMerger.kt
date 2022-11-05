package com.github.sirywell.methodhandleplugin.mhtype

import com.github.sirywell.methodhandleplugin.MHS
import com.github.sirywell.methodhandleplugin.subList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiType.BOOLEAN
import com.intellij.psi.PsiType.VOID

/**
 * Contains methods to merge multiple [MhType]s into a new one,
 * provided by [java.lang.invoke.MethodHandles]
 */
object MethodHandlesMerger {

    // TODO effectively identical sequences for loops???

    fun catchException(target: MhType, exType: PsiType, handler: MhType) = target

    fun collectArguments(target: MhType, pos: Int, filter: MhType): MhType {
        if (anyBot(target, filter)) return Bot
        if (target !is MhSingleType) return target
        if (filter !is MhSingleType) return Top
        val parameters = target.signature.parameters.toMutableList()
        if (pos >= parameters.size) return Top
        if (filter.signature.returnType != VOID) {
            // the return type of the filter must match the replaced type
            if (parameters[pos] != filter.signature.returnType) {
                return Top
            }
            // the return value of filter will be passed to target at pos
            parameters.removeAt(pos)
        }
        val filterParameters = filter.signature.parameters
        parameters.addAll(pos, filterParameters)
        return constructCompatibleType(target.signature.withParameters(parameters), target, filter)
    }

    // fun collectCoordinates() no VarHandle support, preview

    fun countedLoop(iterations: MhType, init: MhType, body: MhType): MhType {
        if (anyBot(iterations, init, body)) return Bot
        if (iterations !is MhSingleType) return Top
        if (init !is MhSingleType) return Top
        if (body !is MhSingleType) return Top
        if (iterations.signature.returnType != PsiType.INT) return Top
        if (iterations.signature.parameters != init.signature.parameters) return Top
        if (init.signature.returnType != body.signature.returnType) return Top
        if (body.signature.returnType == VOID) {
            // (I A...)
            val parameters = body.signature.parameters
            val size = parameters.size
            // (I)
            if (size >= 1 && parameters[0] != PsiType.INT) return Top
            // for (I A...), A... must match signature of init
            if (size >= 2 && parameters.subList(1, parameters.size) != init.signature.parameters) return Top
        } else {
            // (V I A...)
            val parameters = body.signature.parameters
            val size = parameters.size
            // (V I)
            if (size >= 2 && (parameters[0] != init.signature.returnType || parameters[1] != PsiType.INT)) return Top
            // for (V I A...), A... must match signature of init
            if (size >= 3 && parameters.subList(2, parameters.size) != init.signature.parameters) return Top
        }
        // this might not be very precise
        return constructCompatibleType(init.signature, body, init, iterations)
    }

    fun countedLoop(start: MhType, end: MhType, init: MhType, body: MhType): MhType {
        if (anyBot(start, end, init, body)) return Bot
        if (start !is MhSingleType) return Top
        if (end !is MhSingleType) return Top
        if (start.signature != end.signature) return Top
        // types otherwise must be equal to countedLoop(iterations, init, body)
        return countedLoop(start, init, body)
    }

    fun doWhileLoop(init: MhType, body: MhType, pred: MhType): MhType {
        if (anyBot(init, body, pred)) return Bot
        if (init !is MhSingleType) return Top
        if (body !is MhSingleType) return Top
        if (pred !is MhSingleType) return Top
        if (pred.signature.returnType != BOOLEAN) return Top
        val internalParams = body.signature.parameters
        if (internalParams != pred.signature.parameters) return Top
        if (init.signature.returnType != body.signature.returnType) return Top
        if (body.signature.returnType == VOID) {
            if (init.signature != body.signature) return Top
        } else {
            if (internalParams.isEmpty()) return Top
            if (internalParams[0] != body.signature.returnType) return Top
            if (init.signature.parameters != internalParams.subList(1, internalParams.size)) return Top
        }
        return constructCompatibleType(init.signature, init, body, pred)
    }

    fun dropArguments(target: MhType, pos: Int, valueTypes: List<PsiType>): MhType {
        if (anyBot(target)) return Bot
        if (target !is MhSingleType) return Top
        val signature = target.signature
        if (pos > signature.parameters.size) return Top
        val list = signature.parameters.toMutableList()
        list.addAll(pos, valueTypes)
        return target.withSignature(signature.withParameters(list))
    }

    // fun dropArgumentsToMatch()

    // fun dropCoordinates() no VarHandle support, preview

    fun dropReturn(target: MhType): MhType {
        return when (target) {
            is MhSingleType -> target.withSignature(target.signature.withReturnType(VOID))
            else -> target // Bot or Top
        }
    }

    fun explicitCastArguments(target: MhType, newType: MhType): MhType {
        if (anyBot(target, newType)) return Bot
        if (target !is MhSingleType) return Top
        if (newType !is MhSingleType) return Top
        if (target.signature.parameters.size != newType.signature.parameters.size) return Top
        return newType
    }

    fun filterArguments(target: MhType, pos: Int, filters: List<MhType>): MhType {
        if (anyBot(target, *filters.toTypedArray())) return Bot
        if (target !is MhSingleType) return Top
        if (filters.any { it !is MhSingleType }) return Top
        val targetSignature = target.signature
        if (pos + filters.size > targetSignature.parameters.size) return Top
        val filtersCast = filters.map { it as MhSingleType }
        // filters must be unary operators T1 -> T2
        if (filtersCast.any { it.signature.parameters.size != 1 }) return Top
        // return type of filters[i] must be type of targetParameters[i + pos]
        if (targetSignature.parameters.subList(pos)
            .zip(filtersCast)
            .any { (psiType, mhType) -> psiType != mhType.signature.returnType })
            return Top
        // replace parameter types in range of [pos, pos + filters.size)
        val result = targetSignature.parameters.mapIndexed { index, psiType ->
            if (index >= pos && index - pos < filters.size) filtersCast[index - pos].signature.parameters[0]
            else psiType
        }
        return constructCompatibleType(targetSignature.withParameters(result), target, *filtersCast.toTypedArray())
    }

    // fun filterCoordinates() no VarHandle support, preview

    fun filterReturnValue(target: MhType, filter: MhType): MhType {
        if (anyBot(target, filter)) return Bot
        if (target !is MhSingleType) return Top
        if (filter !is MhSingleType || filter.signature.parameters.size != 1) return Top
        if (target.signature.returnType != filter.signature.parameters[0]) return Top
        return constructCompatibleType(target.signature.withReturnType(filter.signature.returnType), target, filter)
    }

    // filterValue() no VarHandle support, preview

    fun foldArguments(target: MhType, pos: Int, combiner: MhType): MhType {
        if (anyBot(target, combiner)) return Bot
        if (target !is MhSingleType) return Top
        if (combiner !is MhSingleType) return Top
        val targetSignature = target.signature // (Z..., V, A[N]..., B...)T, where N = |combiner.parameters|
        val combinerSignature = combiner.signature // (A...)V
        if (pos >= targetSignature.parameters.size) return Top
        val combinerIsVoid = combinerSignature.returnType == VOID
        val sub: List<PsiType> = if (combinerIsVoid) {
            listOf(*combinerSignature.parameters.toTypedArray())
        } else {
            listOf(combinerSignature.returnType, *combinerSignature.parameters.toTypedArray())
        }
        val listStart = subListStart(targetSignature.parameters, sub) ?: return Top
        val newParameters = targetSignature.parameters.toMutableList()
        if (combinerIsVoid) {
            newParameters.removeAt(listStart)
        }
        return constructCompatibleType(targetSignature.withParameters(newParameters), target, combiner)
    }

    private fun subListStart(parameters: List<PsiType>, sub: List<PsiType>): Int? {
        // compare all windows with sub, pick window start of first match
        return parameters.windowed(sub.size) { it }
            .flatMapIndexed { index, window -> if (window == sub) listOf(index) else listOf() }
            .firstOrNull()
    }

    fun guardWithTest(test: MhType, target: MhType, fallback: MhType): MhType {
        if (anyBot(test, target, fallback)) return Bot
        if (test !is MhSingleType) return Top
        if (target !is MhSingleType) return Top
        if (fallback !is MhSingleType) return Top
        val testSignature = test.signature // (A...)boolean
        val targetSignature = target.signature // (A... B...)T
        val fallbackSignature = fallback.signature // (A... B...)T
        if (testSignature.returnType != BOOLEAN) return Top
        if (targetSignature != fallbackSignature) return Top
        if (testSignature.parameters.size > targetSignature.parameters.size) return Top
        if (testSignature.parameters != targetSignature.parameters.subList(0, testSignature.parameters.size)) return Top
        return constructCompatibleType(targetSignature, test, target, fallback)
    }

    fun insertArguments(target: MhType, pos: Int, valueTypes: List<PsiType>): MhType {
        if (anyBot(target)) return Bot
        if (target !is MhSingleType) return Top
        val list = target.signature.parameters.toMutableList()
        if (list.size < pos + valueTypes.size) return Top
        list.subList(pos, pos + valueTypes.size).clear() // drop
        return target.withSignature(target.signature.withParameters(list))
    }

    fun iteratedLoop(iterator: MhType, init: MhType, body: MhType): MhType = TODO()

    fun loop(clauses: List<List<MhType>>): MhType = TODO() // not sure if we can do anything about this

    fun permuteArguments(target: MhType, newType: MhType, reorder: List<Int>): MhType {
        if (anyBot(target, newType)) return Bot
        if (target !is MhSingleType) return Top
        if (newType !is MhSingleType) return Top
        if (target.signature.returnType != newType.signature.returnType) return Top
        val outParams = target.signature.parameters
        val nOut = outParams.size
        val inParams = newType.signature.parameters
        val nIn = inParams.size
        if (nOut != reorder.size) return Top
        if (reorder.any { it >= nIn || it < 0 }) return Top
        for ((n, i) in reorder.withIndex()) {
            if (outParams[n] != inParams[i]) return Top
        }
        return constructCompatibleType(newType.signature, target, newType)
    }

    // fun permuteCoordinates() no VarHandle support, preview

    fun tableSwitch(fallback: MhType, targets: List<MhType>): MhType {
        if (anyBot(fallback, *targets.toTypedArray())) return Bot
        if (fallback !is MhSingleType) return Top
        if (targets.any { it !is MhSingleType }) return Top
        val cases = targets.map { it as MhSingleType }
        if (cases.any { it.signature != fallback.signature }) return Top
        return constructCompatibleType(fallback.signature, fallback, *cases.toTypedArray())
    }

    fun tryFinally(target: MhType, cleanup: MhType): MhType {
        if (anyBot(target, cleanup)) return Bot
        if (target !is MhSingleType) return Top
        if (cleanup !is MhSingleType) return Top
        val cleanupSignature = cleanup.signature
        val targetSignature = target.signature
        if (cleanupSignature.returnType != targetSignature.returnType) return Top
        val isVoid = targetSignature.returnType == VOID
        val leading = if (isVoid) 1 else 2
        if (cleanupSignature.parameters.size < leading) return Top
        // TODO 0th param must be <= Throwable
        if (!isVoid && cleanupSignature.parameters[1] != cleanupSignature.returnType) return Top
        val aList = cleanupSignature.parameters.subList(leading)
        if (!targetSignature.parameters.startsWith(aList)) return Top
        return constructCompatibleType(targetSignature, target, cleanup)
    }

    fun whileLoop(init: MhType, pred: MhType, body: MhType) = doWhileLoop(init, pred, body) // same type behavior

    /**
     * Constructs a specific MhType variant depending on the types of [mhTypes]
     */
    private fun constructCompatibleType(signature: MHS, vararg mhTypes: MhSingleType): MhType {
        val hasConflictingTypes = mhTypes.filterNot { it is MhExactType }.windowed(2) { (a, b) ->
            a is MhSubType && b is MhSuperType || a is MhSuperType && b is MhSubType
        }.any { it }
        if (hasConflictingTypes) {
            return Top
        }
        if (mhTypes.all { it is MhExactType }) {
            return MhExactType(signature)
        }
        if (mhTypes.any { it is MhSubType }) {
            return MhSubType(signature)
        }
        // one must be MhSuperType
        return MhSuperType(signature)
    }

    private fun anyBot(vararg types: MhType) = types.any { it is Bot }


}

private fun <E> List<E>.startsWith(list: List<E>): Boolean {
    if (list.size > this.size) return false
    return this.subList(0, list.size) == list
}
