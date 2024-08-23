package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethodCallExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle

class RedundantInvocationFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return MethodHandleBundle.message("problem.general.invocation.redundant.remove")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val commentTracker = CommentTracker()
        when (val psiElement = descriptor.psiElement) {
            is PsiMethodCallExpression -> {
                PsiReplacementUtil.replaceExpression(
                    psiElement,
                    psiElement.methodExpression.qualifierExpression?.text!!,
                    commentTracker
                )
            }
        }
    }
}