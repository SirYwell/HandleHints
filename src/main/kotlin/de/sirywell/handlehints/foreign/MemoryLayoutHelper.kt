package de.sirywell.handlehints.foreign

import com.intellij.psi.PsiExpression
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.MemoryLayoutType
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
}