package de.sirywell.methodhandleplugin.inspection

import com.intellij.codeInsight.daemon.impl.quickfix.AddTypeCastFix
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.*
import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import com.intellij.refactoring.introduceVariable.JavaIntroduceVariableHandlerBase
import de.sirywell.intellij.ReplaceMethodCallFix
import de.sirywell.methodhandleplugin.MethodHandleBundle
import de.sirywell.methodhandleplugin.TypeData
import de.sirywell.methodhandleplugin.methodName
import de.sirywell.methodhandleplugin.receiverIsMethodHandle
import de.sirywell.methodhandleplugin.type.CompleteSignature
import de.sirywell.methodhandleplugin.type.DirectType
import de.sirywell.methodhandleplugin.type.Type
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

class MethodHandleInvokeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return Visitor(holder)
    }

    inner class Visitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (!receiverIsMethodHandle(expression)) return
            val target = expression.methodExpression.qualifierExpression ?: return
            val typeData = TypeData.forFile(expression.containingFile)
            val type = typeData[target] ?: return
            if (type.signature !is CompleteSignature) return // ignore for now
            val (returnType, parameters) = type.signature
            when (expression.methodName) {
                "invoke" -> {
                    checkArgumentsCount(parameters, expression)
                }

                "invokeExact" -> {
                    checkArgumentsTypes(parameters, expression)
                    checkArgumentsCount(parameters, expression)
                    checkReturnType(returnType, expression)
                }
            }
        }

        private fun checkArgumentsTypes(parameters: List<Type>, expression: PsiMethodCallExpression) {
            if (expression.argumentList.expressionTypes.zip(parameters)
                    .any { it.first != it.second }
            ) {
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    MethodHandleBundle.message("problem.invocation.arguments.wrong.types",
                        parameters,
                        expression.argumentList.expressionTypes.map { it.presentableText }
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }

        private fun checkReturnType(returnType: Type, expression: PsiMethodCallExpression) {
            val parent = expression.parent
            if (parent is PsiExpressionStatement) {
                if (!returnType.canBe(PsiTypes.voidType())) {
                    val fixes: Array<LocalQuickFix> = if (returnType is DirectType) {
                        arrayOf(ReturnTypeInStatementFix(returnType.psiType))
                    } else {
                        emptyArray()
                    }
                    problemsHolder.registerProblem(
                        expression,
                        MethodHandleBundle.message(
                            "problem.invocation.returnType.not.void",
                            returnType
                        ),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        *fixes
                    )
                }
            } else if (returnType is DirectType
                && returnType.psiType == PsiTypes.voidType()
                && parent !is PsiExpressionStatement
            ) {
                problemsHolder.registerProblem(
                    expression,
                    MethodHandleBundle.message(
                        "problem.invocation.returnType.mustBeVoid"
                    )
                )
            } else if (parent !is PsiTypeCastExpression) {
                if (!returnType.canBe(PsiType.getJavaLangObject(expression.manager, expression.resolveScope))) {
                    val fixes: Array<LocalQuickFix> = if (returnType is DirectType) {
                        arrayOf(
                            LocalQuickFix.from(AddTypeCastFix(returnType.psiType, expression))!!,
                            ReplaceMethodCallFix("invoke")
                        )
                    } else {
                        arrayOf(ReplaceMethodCallFix("invoke"))
                    }

                    problemsHolder.registerProblem(
                        expression.methodExpression as PsiExpression,
                        MethodHandleBundle.message(
                            "problem.invocation.returnType.not.object",
                            returnType
                        ),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        *fixes
                    )
                }
            } else {
                val type = parent.castType?.type ?: return
                if (!returnType.canBe(type)) {
                    problemsHolder.registerProblem(
                        parent.castType!!,
                        MethodHandleBundle.message(
                            "problem.invocation.returnType.wrong.cast",
                            returnType,
                            type.presentableText
                        ),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }

        private fun checkArgumentsCount(
            parameters: List<Type>,
            expression: PsiMethodCallExpression
        ) {
            if (parameters.size != expression.argumentList.expressionCount) {
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    MethodHandleBundle.message(
                        "problem.invocation.arguments.count",
                        parameters.size,
                        expression.argumentList.expressionCount
                    ),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }

    class ReturnTypeInStatementFix(@SafeFieldForPreview private val returnType: PsiType) : LocalQuickFix {
        // if 241 is not supported anymore, this likely can be removed
        private val addTypeCast: MethodHandle = run {
            // find method with unspecified return type
            val method = AddTypeCastFix::class.java.getDeclaredMethod(
                "addTypeCast",
                Project::class.java,
                PsiExpression::class.java,
                PsiType::class.java
            )
            MethodHandles.lookup().unreflect(method)
        }

        override fun getFamilyName() =
            MethodHandleBundle.message("problem.invocation.returnType.fix.introduce.variable")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            var expr = descriptor.psiElement as PsiExpression
            val parent = expr.parent
            if (returnType != PsiType.getJavaLangObject(expr.manager, expr.resolveScope)) {
                addTypeCast(expr.project, expr, returnType)
                // sadly, addTypeCast does not return the replacement,
                // so we need to find it ourselves
                expr = parent.childrenOfType<PsiTypeCastExpression>().first()
            }
            val supportProvider = LanguageRefactoringSupport.INSTANCE.forLanguage(JavaLanguage.INSTANCE)
            val handler = supportProvider.introduceVariableHandler as JavaIntroduceVariableHandlerBase
            handler(project, FileEditorManager.getInstance(project).selectedTextEditor, expr)
        }

        // Disable preview for now, as it throws exceptions in its current state
        override fun getFileModifierForPreview(target: PsiFile) = null
    }
}
