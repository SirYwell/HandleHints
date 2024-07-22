package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.type.*
import org.jetbrains.annotations.Nls

abstract class ProblemEmitter(protected val typeData: TypeData) {

    protected inline fun <reified T : TypeLatticeElement<T>> emitProblem(element: PsiElement, message: @Nls String): T {
        typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return topForType<T>()
    }

    protected inline fun <reified T : TypeLatticeElement<T>> emitProblem(
        element: PsiElement,
        message: @Nls String,
        vararg quickFixes: LocalQuickFix
    ): T {
        typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, *quickFixes)
        }
        return topForType<T>()
    }

    protected inline fun <reified T : TypeLatticeElement<T>> emitMustNotBeVoid(typeExpr: PsiExpression): T {
        return emitProblem(
            typeExpr,
            message("problem.merging.general.typeMustNotBe", PsiTypes.voidType().presentableText)
        )
    }

    protected inline fun <reified T : TypeLatticeElement<T>> emitMustBeReferenceType(
        refc: PsiExpression,
        referenceClass: Type
    ) {
        emitProblem<T>(
            refc, message(
                "problem.merging.general.referenceTypeExpectedReturn",
                referenceClass,
            )
        )
    }

    protected inline fun <reified T : TypeLatticeElement<T>> emitMustBeArrayType(
        refc: PsiExpression,
        referenceClass: Type
    ): T {
        return emitProblem(
            refc, message(
                "problem.merging.general.arrayTypeExpected",
                referenceClass,
            )
        )
    }

    protected fun emitIncompatibleReturnTypes(
        element: PsiElement,
        first: Type,
        second: Type
    ): MethodHandleType {
        return emitProblem(
            element, message(
                "problem.merging.general.incompatibleReturnType",
                first,
                second
            )
        )
    }

    protected inline fun <reified T : TypeLatticeElement<T>> emitOutOfBounds(
        size: Long?,
        targetExpr: PsiExpression,
        pos: Long,
        exclusive: Boolean
    ): T = if (size != null) {
        if (exclusive) {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsExcl", pos, size))
        } else {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsIncl", pos, size))
        }
    } else {
        emitProblem(targetExpr, message("problem.general.position.invalidIndex", pos))
    }
}