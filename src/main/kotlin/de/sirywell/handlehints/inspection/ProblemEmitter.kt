package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.type.MethodHandleType
import de.sirywell.handlehints.type.TopMethodHandleType
import de.sirywell.handlehints.type.Type
import org.jetbrains.annotations.Nls

abstract class ProblemEmitter(private val typeData: TypeData) {

    protected fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return TopMethodHandleType
    }

    protected fun emitMustNotBeVoid(typeExpr: PsiExpression) {
        emitProblem(
            typeExpr,
            message("problem.merging.general.typeMustNotBe", PsiTypes.voidType().presentableText)
        )
    }

    protected fun emitMustBeReferenceType(refc: PsiExpression, referenceClass: Type) {
        emitProblem(
            refc, message(
                "problem.merging.general.referenceTypeExpectedReturn",
                referenceClass,
            )
        )
    }

    protected fun emitMustBeArrayType(refc: PsiExpression, referenceClass: Type) {
        emitProblem(
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

    protected fun emitOutOfBounds(
        size: Int?,
        targetExpr: PsiExpression,
        pos: Int,
        exclusive: Boolean
    ) = if (size != null) {
        if (exclusive) {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsExcl", pos, size))
        } else {
            emitProblem(targetExpr, message("problem.general.position.invalidIndexKnownBoundsIncl", pos, size))
        }
    } else {
        emitProblem(targetExpr, message("problem.general.position.invalidIndex", pos))
    }
}