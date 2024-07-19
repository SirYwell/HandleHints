package de.sirywell.handlehints.foreign

import com.intellij.codeInspection.LocalQuickFix.from
import com.intellij.psi.PsiExpression
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
}