package de.sirywell.handlehints.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiTypes
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.type.*
import org.jetbrains.annotations.Nls

abstract class ProblemEmitter(protected val typeData: TypeData) {

    protected inline fun <reified T : TypeLatticeElement<*>> emitProblem(element: PsiElement, message: @Nls String): T {
        typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return topForType<T>()
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

    protected inline fun <reified T : TypeLatticeElement<*>> emitOutOfBounds(
        size: Int?,
        targetExpr: PsiExpression,
        pos: Int,
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