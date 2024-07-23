package de.sirywell.handlehints.foreign

import com.intellij.codeInspection.LocalQuickFix.from
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType
import com.intellij.util.containers.headTail
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.getConstantLong
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.AdjustAlignmentFix
import de.sirywell.handlehints.inspection.AdjustPaddingFix
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class MemoryLayoutHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    fun withByteAlignment(
        qualifier: PsiExpression,
        byteAlignmentExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MemoryLayoutType {
        val byteAlignment = byteAlignmentExpr.asLong() ?: return TopMemoryLayoutType
        if (byteAlignment < 0 || byteAlignment.countOneBits() != 1) {
            return emitProblem(byteAlignmentExpr, message("problem.general.argument.notAPowerOfTwo", byteAlignment))
        }
        val target = ssaAnalyzer.memoryLayoutType(qualifier, block) ?: TopMemoryLayoutType
        if (target is StructLayoutType && target.memberLayouts.anyKnownMatches {
                (it.byteAlignment ?: 1) > byteAlignment // 1 is smallest alignment anyway, so that's a good fallback
            }
        ) {
            return emitProblem(byteAlignmentExpr, message("problem.foreign.memory.invalidAlignment", byteAlignment))
        }
        return target.withByteAlignment(byteAlignment)
    }

    private fun PsiExpression.asLong(): Long? {
        return getConstantLong()
    }

    @Suppress("UnstableApiUsage")
    fun structLayout(arguments: List<PsiExpression>, block: SsaConstruction.Block): MemoryLayoutType {
        val members = arguments.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        val size = sumSize(members)
        val alignment = maxAlignment(members)
        if (size != null) {
            var t = 0L
            members.forEachIndexed { index, type ->
                val byteAlignment = type.byteAlignment
                if (byteAlignment != null && t % byteAlignment != 0L) {
                    // be careful here, alignment of other layouts depends on inner layouts
                    // e.g. [j8i4].withByteAlignment(4) fails
                    val padding = byteAlignment - t % byteAlignment
                    val fixes = if (type is ValueLayoutType)
                        arrayOf(
                            from(AdjustPaddingFix(arguments[index], padding))!!,
                            from(AdjustAlignmentFix(arguments[index], requiredAlignment(t, byteAlignment)))!!
                        )
                    else
                        arrayOf(from(AdjustPaddingFix(arguments[index], padding))!!)
                    return emitProblem(
                        arguments[index], message(
                            "problem.foreign.memory.layoutMismatch",
                            byteAlignment, t
                        ),
                        *fixes
                    )
                }
                t += type.byteSize!! // size is not null, so all elements have non-null byteSize
            }
        }
        return StructLayoutType(CompleteMemoryLayoutList(members), alignment, size, WITHOUT_NAME)
    }

    fun unionLayout(arguments: List<PsiExpression>, block: SsaConstruction.Block): MemoryLayoutType {
        val members = arguments.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        val size = maxSize(members)
        val alignment = maxAlignment(members)
        return UnionLayoutType(CompleteMemoryLayoutList(members), alignment, size, WITHOUT_NAME)
    }

    private fun requiredAlignment(t: Long, byteAlignment: Long): Long {
        var ba = byteAlignment
        while ((t % ba).countOneBits() != 1) {
            ba = ba shr 1
        }
        return t % ba
    }

    fun sequenceLayout(
        elementCountExpr: PsiExpression,
        elementLayoutExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MemoryLayoutType {
        val elementCount = elementCountExpr.asLong()
        val elementLayout = ssaAnalyzer.memoryLayoutType(elementLayoutExpr, block) ?: TopMemoryLayoutType
        if (elementCount != null) {
            if (elementCount < 0) {
                return emitProblem(
                    elementCountExpr,
                    message("problem.general.argument.numericConditionMismatch", ">= 0", elementCount)
                )
            }

            // 0 is a sane fallback for this check as it is conservative
            try {
                Math.multiplyExact(elementLayout.byteSize ?: 0, elementCount)
            } catch (_: ArithmeticException) {
                return emitProblem(
                    elementCountExpr,
                    message("problem.foreign.memory.layoutSizeOverflow")
                )

            }
        }
        // if one is null, the result is 0
        if ((elementLayout.byteSize ?: 0) % (elementLayout.byteAlignment ?: 1) != 0L) {
            return emitProblem(
                elementLayoutExpr,
                message(
                    "problem.foreign.memory.alignmentMismatch",
                    elementLayout.byteSize!!,
                    elementLayout.byteAlignment!!
                )
            )
        }
        return SequenceLayoutType(elementLayout, elementCount, elementLayout.byteAlignment, WITHOUT_NAME)
    }

    fun paddingLayout(byteSizeExpr: PsiExpression): MemoryLayoutType {
        val size = byteSizeExpr.asLong() ?: return PaddingLayoutType(1, null, WITHOUT_NAME)
        if (size <= 0) {
            return emitProblem(byteSizeExpr, message("problem.general.argument.numericConditionMismatch", "> 0", size))
        }
        return PaddingLayoutType(1, size, WITHOUT_NAME)
    }

    fun withName(qualifier: PsiExpression, nameExpr: PsiExpression, block: SsaConstruction.Block): MemoryLayoutType {
        val layoutType = ssaAnalyzer.memoryLayoutType(qualifier, block) ?: TopMemoryLayoutType
        val name = nameExpr.getConstantOfType<String>()?.let { ExactLayoutName(it) } ?: TopLayoutName
        return layoutType.withName(name)
    }

    private fun sumSize(list: List<MemoryLayoutType>): Long? {
        return list
            .map { it.byteSize ?: return null }
            .sum()
    }

    private fun maxSize(list: List<MemoryLayoutType>): Long? {
        return list
            .map { it.byteSize ?: return null }
            .max()
    }

    private fun maxAlignment(list: List<MemoryLayoutType>): Long? {
        return list
            .map { it.byteAlignment ?: return null }
            .maxOrNull() ?: 1 // if empty, the alignment is 1
    }

    fun varHandle(
        qualifier: PsiExpression,
        arguments: List<PsiExpression>,
        methodExpr: PsiExpression,
        block: SsaConstruction.Block
    ): VarHandleType {
        val layoutType = ssaAnalyzer.memoryLayoutType(qualifier, block) ?: TopMemoryLayoutType
        val path = toPath(arguments, block)
        val memorySegmentType =
            PsiType.getTypeByName("java.lang.foreign.MemorySegment", qualifier.project, qualifier.resolveScope)
        return resolvePath(path, layoutType, mutableListOf(ExactType(memorySegmentType))) {
            if (it == -1) methodExpr
            else arguments[it]
        }
    }

    private tailrec fun resolvePath(
        path: List<IndexedValue<PathElementType>>,
        layoutType: MemoryLayoutType,
        coords: MutableList<Type>,
        contextElement: (Int) -> PsiExpression
    ): VarHandleType {
        if (layoutType == BotMemoryLayoutType) return BotVarHandleType
        else if (layoutType == TopMemoryLayoutType) return TopVarHandleType
        if (path.isEmpty()) {
            return when (layoutType) {
                is ValueLayoutType -> CompleteVarHandleType(layoutType.type, CompleteTypeList(coords))
                else -> emitProblem(contextElement(-1), message("problem.foreign.memory.pathTargetNotValueLayout"))
            }
        }
        val (head, tail) = path.headTail()
        val resolvedLayout = when (head.value) {
            BotPathElementType -> return BotVarHandleType // TODO does that make sense?
            TopPathElementType -> return TopVarHandleType
            is SequenceElementType -> sequenceElement(
                layoutType,
                // what is wrong with the Kotlin type system?
                IndexedValue(head.index, head.value as SequenceElementType),
                coords,
                contextElement
            )

            is GroupElementType -> groupElement(
                layoutType,
                IndexedValue(head.index, head.value as GroupElementType),
                contextElement
            )
        }
        return resolvePath(tail, resolvedLayout, coords, contextElement)
    }

    private fun groupElement(
        layoutType: MemoryLayoutType,
        head: IndexedValue<GroupElementType>,
        contextElement: (Int) -> PsiExpression
    ): MemoryLayoutType {
        return when (layoutType) {
            BotMemoryLayoutType -> return BotMemoryLayoutType
            TopMemoryLayoutType -> return TopMemoryLayoutType
            is StructLayoutType -> findByElementType(head, layoutType.memberLayouts, contextElement)
            is UnionLayoutType -> findByElementType(head, layoutType.memberLayouts, contextElement)
            is SequenceLayoutType,
            is PaddingLayoutType,
            is ValueLayoutType -> {
                return emitProblem(
                    contextElement(head.index),
                    message(
                        "problem.foreign.memory.pathElementMismatch",
                        "group",
                        layoutType::class.simpleName!!.replace("Type", "")
                    )
                )
            }
        }
    }

    private fun findByElementType(
        elementType: IndexedValue<GroupElementType>,
        memberLayouts: MemoryLayoutList,
        contextElement: (Int) -> PsiExpression
    ): MemoryLayoutType {
        return when (elementType.value.variant) {
            is IndexGroupElementVariant -> {
                val index = (elementType.value.variant as IndexGroupElementVariant).index
                if (index == null || index >= Int.MAX_VALUE) TopMemoryLayoutType
                else if (memberLayouts.compareSize((index + 1).toInt()) == PartialOrder.LT) {
                    // known index out of bounds
                    emitProblem(
                        contextElement(elementType.index),
                        message("problem.foreign.memory.pathGroupElementOutOfBounds", 0, index, memberLayouts.sizeOrNull()!!)
                    )
                } else memberLayouts[index.toInt()]
            }

            is NameGroupElementVariant -> {
                val name = (elementType.value.variant as NameGroupElementVariant).name
                if (name == null) TopMemoryLayoutType
                // we need to be conservative here: multiple memberLayouts can have the same name
                // so we must abort as soon as we find a layout with an unknown name
                else {
                    for (type in memberLayouts.partialList()) {
                        if (type.name !is ExactLayoutName) return TopMemoryLayoutType
                        else if ((type.name as ExactLayoutName).name == name) return type

                    }
                    return emitProblem(
                        contextElement(elementType.index),
                        message("problem.foreign.memory.pathGroupElementUnknownName", name)
                    )
                }
            }
        }
    }

    private fun sequenceElement(
        layoutType: MemoryLayoutType,
        head: IndexedValue<SequenceElementType>,
        coords: MutableList<Type>,
        contextElement: (Int) -> PsiExpression
    ): MemoryLayoutType {
        val inner = when (layoutType) {
            BotMemoryLayoutType -> return BotMemoryLayoutType
            TopMemoryLayoutType -> return TopMemoryLayoutType
            is SequenceLayoutType -> layoutType.elementLayout
            is PaddingLayoutType,
            is StructLayoutType,
            is UnionLayoutType,
            is ValueLayoutType -> {
                return emitProblem(
                    contextElement(head.index),
                    message(
                        "problem.foreign.memory.pathElementMismatch",
                        "sequence",
                        layoutType::class.simpleName!!.replace("Type", "")
                    )
                )
            }
        }
        val headVal = head.value
        when (headVal.variant) {
            OpenSequenceElementVariant -> coords.add(ExactType.longType)
            is SelectingOpenSequenceElementVariant -> coords.add(ExactType.longType)
            is SelectingSequenceElementVariant -> {
                headVal.variant.index?.let {
                    val c = layoutType.elementCount ?: Long.MAX_VALUE
                    if (it > c) {
                        return emitOutOfBounds(layoutType.elementCount, contextElement(head.index), it, true)
                    }
                }
            }
        }
        return inner
    }

    private fun toPath(
        arguments: List<PsiExpression>,
        block: SsaConstruction.Block
    ): List<IndexedValue<PathElementType>> {
        return arguments
            .map { ssaAnalyzer.pathElementType(it, block) ?: TopPathElementType }
            .withIndex()
            .toList()
    }
}