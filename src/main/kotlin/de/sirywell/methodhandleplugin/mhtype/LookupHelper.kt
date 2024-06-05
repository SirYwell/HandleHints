package de.sirywell.methodhandleplugin.mhtype

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.*
import de.sirywell.methodhandleplugin.MethodHandleBundle
import de.sirywell.methodhandleplugin.asType
import de.sirywell.methodhandleplugin.dfa.SsaAnalyzer
import de.sirywell.methodhandleplugin.dfa.SsaConstruction
import de.sirywell.methodhandleplugin.type.*
import de.sirywell.methodhandleplugin.type.TopType
import org.jetbrains.annotations.Nls

class LookupHelper(private val ssaAnalyzer: SsaAnalyzer) {

    // MethodHandle factory methods

    fun findConstructor(refc: PsiExpression, typeExpr: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val type = ssaAnalyzer.mhType(typeExpr, block) ?: return MethodHandleType(BotSignature)
        if (type.signature !is CompleteSignature) return type
        if (!type.signature.returnType.canBe(PsiTypes.voidType())) {
            emitProblem(
                typeExpr,
                MethodHandleBundle.message(
                    "problem.merging.general.otherReturnTypeExpected",
                    type.signature.returnType,
                    PsiTypes.voidType().presentableText
                )
            )
        }
        val referenceClass = refc.asReferenceType()
        return MethodHandleType(type.signature.withReturnType(referenceClass))
    }

    fun findGetter(refc: PsiExpression, typeExpr: PsiExpression): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        val returnType = typeExpr.asNonVoidType()
        return MethodHandleType(complete(returnType, listOf(referenceClass)))
    }

    fun findSetter(refc: PsiExpression, typeExpr: PsiExpression): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        val paramType = typeExpr.asNonVoidType()
        return MethodHandleType(complete(DirectType(PsiTypes.voidType()), listOf(referenceClass, paramType)))
    }

    fun findSpecial(refc: PsiExpression, type: MethodHandleType, specialCaller: PsiExpression): MethodHandleType {
        refc.asReferenceType()
        if (type.signature !is CompleteSignature) return type
        // TODO inspection:  caller class must be a subclass below the method
        val paramType = specialCaller.asType()
        return prependParameter(type, paramType)
    }

    fun findStatic(refc: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        refc.asReferenceType()
        return mhType
    }

    fun findStaticGetter(refc: PsiExpression, type: PsiExpression): MethodHandleType {
        refc.asReferenceType()
        val returnType = type.asNonVoidType()
        return MethodHandleType(complete(returnType, listOf()))
    }

    fun findStaticSetter(refc: PsiExpression, type: PsiExpression): MethodHandleType {
        refc.asReferenceType()
        val paramType = type.asNonVoidType()
        return MethodHandleType(complete(DirectType(PsiTypes.voidType()), listOf(paramType)))
    }

    fun findVirtual(refc: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        var referenceClass = refc.asType()
        if (referenceClass.isPrimitive()) {
            referenceClass = TopType
            emitMustBeReferenceType(refc, referenceClass)
        }
        if (mhType.signature !is CompleteSignature) return mhType
        val paramType = refc.asType()
        // TODO not exactly correct, receiver could be restricted to lookup class
        return prependParameter(mhType, paramType)
    }

    private fun prependParameter(
        mhType: MethodHandleType,
        paramType: Type
    ): MethodHandleType {
        if (mhType.signature !is CompleteSignature) {
            return mhType
        }
        val pt = CompleteParameterList(listOf(paramType)).addAllAt(1, mhType.signature.parameterList)
        return MethodHandleType(mhType.signature.withParameterTypes(pt))
    }

    private fun emitProblem(element: PsiElement, message: @Nls String): MethodHandleType {
        ssaAnalyzer.typeData.reportProblem(element) {
            it.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return MethodHandleType(TopSignature)
    }

    private fun emitMustNotBeVoid(typeExpr: PsiExpression) {
        emitProblem(
            typeExpr,
            MethodHandleBundle.message("problem.merging.general.typeMustNotBe", PsiTypes.voidType().presentableText)
        )
    }

    private fun emitMustBeReferenceType(refc: PsiExpression, referenceClass: Type) {
        emitProblem(
            refc, MethodHandleBundle.message(
                "problem.merging.general.referenceTypeExpectedReturn",
                referenceClass,
            )
        )
    }

    private fun PsiExpression.asReferenceType(): Type {
        val referenceClass = this.asType()
        if (referenceClass.isPrimitive()) {
            emitMustBeReferenceType(this, referenceClass)
            return TopType
        }
        return referenceClass
    }

    private fun PsiExpression.asNonVoidType(): Type {
        val nonVoid = this.asType()
        val voidType = DirectType(PsiTypes.voidType())
        if (nonVoid == voidType) {
            emitMustNotBeVoid(this)
            return TopType
        }
        return nonVoid
    }
}
