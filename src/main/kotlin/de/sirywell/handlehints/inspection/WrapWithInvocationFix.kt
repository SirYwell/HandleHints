package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle

class WrapWithInvocationFix(private val methodName: String, private val static: Boolean) : LocalQuickFix {
    override fun getFamilyName(): String {
        return if (static) {
            MethodHandleBundle.message("problem.general.invocation.wrap", methodName)
        } else {
            MethodHandleBundle.message("problem.general.invocation.append", methodName)
        }
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val commentTracker = CommentTracker()
        val text = descriptor.psiElement.text
        val newText = if (static) {
            "$methodName($text)"
        } else {
            "$text.$methodName()"
        }
        PsiReplacementUtil.replaceExpression(
            descriptor.psiElement as PsiExpression,
            newText,
            commentTracker
        )

    }
}
