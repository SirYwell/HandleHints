package de.sirywell.handlehints.inspection

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle
import de.sirywell.handlehints.methodName

class AdjustAlignmentFix(expression: PsiExpression, private val requiredAlignment: Long) :
    PsiUpdateModCommandAction<PsiExpression>(expression) {
    override fun getFamilyName(): String {
        return MethodHandleBundle.message("problem.foreign.memory.layoutMismatch.adjustAlignment")
    }

    override fun invoke(context: ActionContext, element: PsiExpression, updater: ModPsiUpdater) {
        val commentTracker = CommentTracker()
        if (element is PsiMethodCallExpression
            && element.methodName == "withByteAlignment"
            && element.argumentList.expressionCount == 1
        ) {
            PsiReplacementUtil.replaceExpression(
                element.argumentList.expressions.first(),
                "$requiredAlignment",
                commentTracker
            )
            return
        }
        PsiReplacementUtil.replaceExpression(
            element,
            "${commentTracker.text(element)}.withByteAlignment($requiredAlignment)",
            commentTracker
        )
    }
}