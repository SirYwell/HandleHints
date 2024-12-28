package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.parentOfType
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.inspection.RedundantInvocationFix
import de.sirywell.handlehints.type.BotVarHandleType
import de.sirywell.handlehints.type.KnownInvocationBehavior
import de.sirywell.handlehints.type.KnownInvocationBehavior.INVOKE
import de.sirywell.handlehints.type.KnownInvocationBehavior.INVOKE_EXACT
import de.sirywell.handlehints.type.VarHandleType

class VarHandleHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {
    fun withInvokeBehavior(targetExpr: PsiExpression, block: SsaConstruction.Block) : VarHandleType {
        return withBehavior(targetExpr, block, INVOKE)
    }

    fun withInvokeExactBehavior(targetExpr: PsiExpression, block: SsaConstruction.Block) : VarHandleType {
        return withBehavior(targetExpr, block, INVOKE_EXACT)
    }

    private fun withBehavior(
        targetExpr: PsiExpression,
        block: SsaConstruction.Block,
        behavior: KnownInvocationBehavior
    ): VarHandleType {
        val varHandleType = ssaAnalyzer.varHandleType(targetExpr, block) ?: BotVarHandleType
        val invocationBehavior = varHandleType.invocationBehavior
        if (invocationBehavior == behavior) {
            emitRedundant(
                targetExpr.parentOfType<PsiMethodCallExpression>()!!,
                message("problem.transforming.varHandleInvokeBehavior.redundant", behavior.readableName),
                RedundantInvocationFix()
            )
        }
        return varHandleType.withInvokeBehavior(behavior)
    }
}