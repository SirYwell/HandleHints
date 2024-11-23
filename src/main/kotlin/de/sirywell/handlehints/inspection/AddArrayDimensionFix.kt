package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiExpression
import com.siyeh.ig.PsiReplacementUtil
import com.siyeh.ig.psiutils.CommentTracker
import de.sirywell.handlehints.MethodHandleBundle

class AddArrayDimensionFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return MethodHandleBundle.message("problem.general.array.dimension.add")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val commentTracker = CommentTracker()
        val expression = descriptor.psiElement as? PsiClassObjectAccessExpression ?: return
        val operand = expression.operand
        val arrayType = operand.type.createArrayType()
        PsiReplacementUtil.replaceExpression(
            descriptor.psiElement as PsiExpression,
            "${arrayType.presentableText}.class",
            commentTracker
        )
    }
}
