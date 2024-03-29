package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import de.sirywell.methodhandleplugin.MHS
import de.sirywell.methodhandleplugin.MethodHandleBundle.message
import de.sirywell.methodhandleplugin.effectivelyIdenticalTo
import de.sirywell.methodhandleplugin.subList

/**
 * Contains methods to merge multiple [MhType]s into a new one,
 * provided by [java.lang.invoke.MethodHandles]
 */
object MethodHandlesMerger {

    // TODO effectively identical sequences for loops???

    fun catchException(target: MhType, exType: PsiType, handler: MhType): MhType {
        if (target !is MhSingleType) return target
        if (handler !is MhSingleType) return handler
        if (handler.parameters.isEmpty() || !handler.parameters[0].isAssignableFrom(exType)) {
            return Top.inspect(message("problem.merging.catchException.missingException", exType.presentableText))
        }
        if (target.returnType != handler.returnType) return incompatibleReturnTypes(target, handler)
        if (!handler.parameters.subList(1).effectivelyIdenticalTo(target.parameters)) return Top
        return target
    }

    fun collectArguments(target: MhType, pos: Int, filter: MhType): MhType {
        if (anyBot(target, filter)) return Bot
        if (target !is MhSingleType) return target
        if (filter !is MhSingleType) return Top
        val parameters = target.parameters.toMutableList()
        if (pos >= parameters.size) return Top
        if (filter.returnType != PsiTypes.voidType()) {
            // the return type of the filter must match the replaced type
            if (parameters[pos] != filter.returnType) {
                return Top
            }
            // the return value of filter will be passed to target at pos
            parameters.removeAt(pos)
        }
        val filterParameters = filter.parameters
        parameters.addAll(pos, filterParameters)
        return constructCompatibleType(target.signature.withParameters(parameters), target, filter)
    }

    // fun collectCoordinates() no VarHandle support, preview

    fun countedLoop(iterations: MhType, init: MhType?, body: MhType): MhType {
        if (init is Bot) return Bot
        if (anyBot(iterations, body)) return Bot
        if (iterations !is MhSingleType) return Top
        if (init != null && init !is MhSingleType) return Top
        if (body !is MhSingleType) return Top
        if (iterations.returnType != PsiTypes.intType()) return Top
        val externalParameters: List<PsiType>
        val internalParameters = body.parameters
        val returnType = body.returnType
        if (init != null && (init as MhSingleType).returnType != returnType)
            return incompatibleReturnTypes(init, body)
        if (returnType == PsiTypes.voidType()) {
            // (I A...)
            val size = internalParameters.size
            if (internalParameters.isEmpty() || internalParameters[0] != PsiTypes.intType()) return Top
            externalParameters = if (size != 1) {
                internalParameters.subList(1)
            } else {
                iterations.parameters
            }
        } else {
            // (V I A...)
            val size = internalParameters.size
            if (size < 2 || (internalParameters[0] != returnType || internalParameters[1] != PsiTypes.intType())) return Top
            externalParameters = if (size != 2) {
                internalParameters.subList(2)
            } else {
                iterations.parameters
            }
        }
        if (init != null && !(init as MhSingleType).parameters.effectivelyIdenticalTo(externalParameters)) return Top
        if (!iterations.parameters.effectivelyIdenticalTo(externalParameters)) return Top
        // this might not be very precise
        return if (init is MhSingleType) {
            constructCompatibleType(init.signature, body, init, iterations)
        } else {
            constructCompatibleType(iterations.signature.withReturnType(body.returnType), body, iterations)
        }
    }

    fun countedLoop(start: MhType, end: MhType, init: MhType?, body: MhType): MhType {
        if (init is Bot) return Bot
        if (anyBot(start, end, body)) return Bot
        if (start !is MhSingleType) return Top
        if (end !is MhSingleType) return Top
        if (start.returnType != end.returnType) return incompatibleReturnTypes(start, end)
        if (start.parameters.effectivelyIdenticalTo(end.parameters)) return Top
        // types otherwise must be equal to countedLoop(iterations, init, body)
        return countedLoop(start, init, body)
    }

    fun doWhileLoop(init: MhType, body: MhType, pred: MhType): MhType {
        if (anyBot(init, body, pred)) return Bot
        if (init !is MhSingleType) return Top
        if (body !is MhSingleType) return Top
        if (pred !is MhSingleType) return Top
        if (pred.returnType != PsiTypes.booleanType()) return Top
        val internalParams = body.parameters
        if (internalParams != pred.parameters) return Top
        if (init.returnType != body.returnType) return incompatibleReturnTypes(init, body)
        if (body.returnType == PsiTypes.voidType()) {
            if (init.signature != body.signature) return Top
        } else {
            if (internalParams.isEmpty()) return Top
            if (internalParams[0] != body.returnType) return Top
            if (init.parameters != internalParams.subList(1, internalParams.size)) return Top
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
            is MhSingleType -> target.withSignature(target.signature.withReturnType(PsiTypes.voidType()))
            else -> target // Bot or Top
        }
    }

    fun explicitCastArguments(target: MhType, newType: MhType): MhType {
        if (anyBot(target, newType)) return Bot
        if (target !is MhSingleType) return Top
        if (newType !is MhSingleType) return Top
        if (target.parameters.size != newType.parameters.size) return Top
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
        if (filtersCast.any { it.parameters.size != 1 }) return Top
        // return type of filters[i] must be type of targetParameters[i + pos]
        if (targetSignature.parameters.subList(pos)
            .zip(filtersCast)
            .any { (psiType, mhType) -> psiType != mhType.returnType })
            return Top
        // replace parameter types in range of [pos, pos + filters.size)
        val result = targetSignature.parameters.mapIndexed { index, psiType ->
            if (index >= pos && index - pos < filters.size) filtersCast[index - pos].parameters[0]
            else psiType
        }
        return constructCompatibleType(targetSignature.withParameters(result), target, *filtersCast.toTypedArray())
    }

    // fun filterCoordinates() no VarHandle support, preview

    fun filterReturnValue(target: MhType, filter: MhType): MhType {
        if (anyBot(target, filter)) return Bot
        if (target !is MhSingleType) return Top
        if (filter !is MhSingleType || filter.parameters.size != 1) return Top
        if (target.returnType != filter.parameters[0]) return Top
        return constructCompatibleType(target.signature.withReturnType(filter.returnType), target, filter)
    }

    // filterValue() no VarHandle support, preview

    fun foldArguments(target: MhType, pos: Int, combiner: MhType): MhType {
        if (anyBot(target, combiner)) return Bot
        if (target !is MhSingleType) return Top
        if (combiner !is MhSingleType) return Top
        val targetSignature = target.signature // (Z..., V, A[N]..., B...)T, where N = |combiner.parameters|
        val combinerSignature = combiner.signature // (A...)V
        if (pos >= targetSignature.parameters.size) return Top
        val combinerIsVoid = combinerSignature.returnType == PsiTypes.voidType()
        val sub: List<PsiType> = if (combinerIsVoid) {
            listOf(*combinerSignature.parameters.toTypedArray())
        } else {
            listOf(combinerSignature.returnType, *combinerSignature.parameters.toTypedArray())
        }
        val listStart = subListStart(targetSignature.parameters, sub) ?: return Top
        val newParameters = targetSignature.parameters.toMutableList()
        if (!combinerIsVoid) {
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
        if (testSignature.returnType != PsiTypes.booleanType()) return Top
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
        if (target.returnType != newType.returnType) return incompatibleReturnTypes(target, newType)
        val outParams = target.parameters
        val nOut = outParams.size
        val inParams = newType.parameters
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
        val isVoid = targetSignature.returnType == PsiTypes.voidType()
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
        if (mhTypes.all { it is MhExactType }) {
            return MhExactType(signature)
        }
        return MhSubType(signature)
    }

    private fun anyBot(vararg types: MhType) = types.any { it is Bot }

    private fun incompatibleReturnTypes(
        first: MhSingleType,
        second: MhSingleType
    ) = Top.inspect(
        message(
            "problem.merging.general.incompatibleReturnType",
            first.returnType.presentableText,
            second.returnType.presentableText
        )
    )



}

private fun <E> List<E>.startsWith(list: List<E>): Boolean {
    if (list.size > this.size) return false
    return this.subList(0, list.size) == list
}
