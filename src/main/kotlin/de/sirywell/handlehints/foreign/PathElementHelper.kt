package de.sirywell.handlehints.foreign

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.getConstantLong
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class PathElementHelper(ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    fun sequenceElement(): PathElementType {
        return SequenceElementType(OpenSequenceElementVariant)
    }
    fun sequenceElement(indexExpr: PsiExpression): PathElementType {
        val index = indexExpr.getConstantLong()
        if ((index ?: Long.MAX_VALUE) < 0L) {
            return emitProblem(
                indexExpr,
                message("problem.general.argument.numericConditionMismatch", ">= 0", index!!)
            )
        }
        return SequenceElementType(SelectingSequenceElementVariant(index))
    }

    fun sequenceElement(startExpr: PsiExpression, stepExpr: PsiExpression): PathElementType {
        val start = startExpr.getConstantLong()
        val step = stepExpr.getConstantLong()
        if ((start ?: Long.MAX_VALUE) < 0L) {
            return emitProblem(
                startExpr,
                message("problem.general.argument.numericConditionMismatch", ">= 0", start!!)
            )
        }
        if ((step ?: 1L) == 0L) {
            return emitProblem(
                stepExpr,
                message("problem.general.argument.numericConditionMismatch", "!= 0", step!!)
            )
        }
        return SequenceElementType(SelectingOpenSequenceElementVariant(start, step))
    }

    fun groupElement(indexOrNameExpr: PsiExpression): PathElementType {
        if (indexOrNameExpr.type?.canonicalText == "java.lang.String") {
            return GroupElementType(NameGroupElementVariant(indexOrNameExpr.getConstantOfType()))
        }
        if (indexOrNameExpr.type in setOf(PsiTypes.longType(), PsiTypes.intType())) {
            val index = indexOrNameExpr.getConstantLong()
            if (index != null && index < 0) {
                return emitProblem(
                    indexOrNameExpr,
                    message("problem.general.argument.numericConditionMismatch", ">= 0", index)
                )

            }
            return GroupElementType(IndexGroupElementVariant(index))
        }
        return TopPathElementType
    }
}