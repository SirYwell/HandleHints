package de.sirywell.handlehints.foreign

import com.intellij.psi.PsiExpression
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.CompleteMemoryLayoutList
import de.sirywell.handlehints.type.MemoryLayoutType
import de.sirywell.handlehints.type.StructLayoutType
import de.sirywell.handlehints.type.TopMemoryLayoutType

class MemoryLayoutHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    fun withByteAlignment(
        qualifier: PsiExpression,
        byteAlignmentExpr: PsiExpression,
        block: SsaConstruction.Block
    ): MemoryLayoutType {
        val byteAlignment = (byteAlignmentExpr.getConstantOfType<Int>()
            ?: byteAlignmentExpr.getConstantOfType<Long>()
            ?: return TopMemoryLayoutType).toLong()
        if (byteAlignment < 0 || byteAlignment.countOneBits() != 1) {
            return emitProblem(byteAlignmentExpr, message("problem.general.argument.notAPowerOfTwo", byteAlignment))
        }
        val target = ssaAnalyzer.memoryLayoutType(qualifier, block) ?: TopMemoryLayoutType
        return target.withByteAlignment(byteAlignment)
    }

    fun structLayout(arguments: List<PsiExpression>, block: SsaConstruction.Block): MemoryLayoutType {
        val members = arguments.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        val size = sumSize(members)
        val alignment = maxAlignment(members)
        return StructLayoutType(CompleteMemoryLayoutList(members), alignment, size)
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