package de.sirywell.handlehints.inspection

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.parentOfType
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle
import de.sirywell.handlehints.getConstantLong
import de.sirywell.handlehints.methodName

@Suppress("UnstableApiUsage")
class AdjustPaddingFix(expression: PsiExpression, private val requiredPadding: Long) :
    PsiUpdateModCommandAction<PsiExpression>(expression) {
    override fun getFamilyName(): String {
        return MethodHandleBundle.message("problem.foreign.memory.layoutMismatch.adjustPadding")
    }

    override fun invoke(context: ActionContext, element: PsiExpression, updater: ModPsiUpdater) {
        val arguments = element.parentOfType<PsiExpressionList>() ?: return
        if (arguments.expressionCount > 1) {
            val index = arguments.expressions.indexOf(element)
            if (index > 0 && isPaddingLayoutCall(arguments.expressions[index - 1])) {
                val call = arguments.expressions[index - 1] as PsiMethodCallExpression
                val arg = call.argumentList.expressions[0]
                val commentTracker = CommentTracker()
                val c = arg.getConstantLong()
                if (c != null) {
                    PsiReplacementUtil.replaceExpression(arg, "${c + requiredPadding}", commentTracker)
                } else {
                    PsiReplacementUtil.replaceExpression(
                        arg,
                        "${commentTracker.text(arg)} + $requiredPadding",
                        commentTracker
                    )
                }
                return
            }
        }
        val elementFactory = PsiElementFactory.getInstance(context.project)
        val paddingExpression = elementFactory.createExpressionFromText(
            "java.lang.foreign.MemoryLayout.paddingLayout($requiredPadding)",
            element
        )
        arguments.addBefore(paddingExpression, element)
    }

    private fun isPaddingLayoutCall(expression: PsiExpression): Boolean {
        return expression is PsiMethodCallExpression
                && expression.methodName == "paddingLayout"
                && expression.argumentList.expressionCount == 1
    }
}