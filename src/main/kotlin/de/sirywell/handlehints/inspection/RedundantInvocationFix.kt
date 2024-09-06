package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethodCallExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle

/**
 * Convert calls of the form Class.method(p0, ..., pn) into p0,
 * and calls of the form object.method(p0, ..., pn) into object
 */
class RedundantInvocationFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return MethodHandleBundle.message("problem.general.invocation.redundant.remove")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val commentTracker = CommentTracker()
        when (val psiElement = descriptor.psiElement) {
            is PsiMethodCallExpression -> {
                val replacement = if (psiElement.resolveMethod()?.hasModifier(JvmModifier.STATIC) ?: return) {
                    psiElement.argumentList.expressions.first()
                } else {
                    psiElement.methodExpression.qualifierExpression
                }
                PsiReplacementUtil.replaceExpression(
                    psiElement,
                    replacement?.text!!,
                    commentTracker
                )
            }
        }
    }
}