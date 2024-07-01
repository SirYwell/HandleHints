package de.sirywell.handlehints.foreign

import com.intellij.codeInspection.LocalQuickFix.from
import com.intellij.psi.PsiExpression
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.getConstantLong
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
        return StructLayoutType(CompleteMemoryLayoutList(members), alignment, size)
    }

    private fun requiredAlignment(t: Long, byteAlignment: Long): Long {
        var ba = byteAlignment
        while ((t % ba).countOneBits() != 1) {
            ba = ba shr 1
        }
        return t % ba
    }

    fun paddingLayout(byteSizeExpr: PsiExpression): MemoryLayoutType {
        val size = byteSizeExpr.asLong() ?: return PaddingLayoutType(1, null)
        if (size <= 0) {
            return emitProblem(byteSizeExpr, message("problem.general.argument.numericConditionMismatch", "> 0", size))
        }
        return PaddingLayoutType(1, size)
    }

    private fun sumSize(list: List<MemoryLayoutType>): Long? {
        return list
            .map { it.byteSize ?: return null }
            .sum()
    }

    private fun maxAlignment(list: List<MemoryLayoutType>): Long? {
        return list
            .map { it.byteAlignment ?: return null }
            .maxOrNull() ?: 1 // if empty, the alignment is 1
    }
}