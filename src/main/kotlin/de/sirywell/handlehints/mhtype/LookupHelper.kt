package de.sirywell.handlehints.mhtype

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PsiTypesUtil
import de.sirywell.handlehints.*
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class LookupHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    // MethodHandle factory methods

    fun findConstructor(refc: PsiExpression, typeExpr: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val type = ssaAnalyzer.methodHandleType(typeExpr, block) ?: return BotMethodHandleType
        if (!type.returnType.canBe(PsiTypes.voidType())) {
            return emitProblem(
                typeExpr,
                MethodHandleBundle.message(
                    "problem.merging.general.otherReturnTypeExpected",
                    type.returnType,
                    PsiTypes.voidType().presentableText
                )
            )
        }
        val referenceClass = refc.asReferenceType()
        val methodHandleType = enhanceVarargsIfKnown(referenceClass, type) { it.constructors }
        return methodHandleType
                .withReturnType(referenceClass)
                .withVarargs(methodHandleType.varargs)
    }

    fun findGetter(refc: PsiExpression, typeExpr: PsiExpression): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        val returnType = typeExpr.asNonVoidType()
        return complete(returnType, listOf(referenceClass))
    }

    fun findSetter(refc: PsiExpression, typeExpr: PsiExpression): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        val paramType = typeExpr.asNonVoidType()
        return complete(ExactType.voidType, listOf(referenceClass, paramType))
    }

    fun findSpecial(
        refc: PsiExpression,
        nameExpr: PsiExpression,
        type: MethodHandleType,
        specialCaller: PsiExpression
    ): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        // TODO inspection:  caller class must be a subclass below the method
        val paramType = specialCaller.asType()
        val name = nameExpr.getConstantOfType<String>() ?: return prependParameter(type, paramType)
        return prependParameter(enhanceVarargsIfKnown(referenceClass, type) {
            it.findMethodsByName(name, true)
        }, paramType)
    }

    fun findStatic(refc: PsiExpression, nameExpr: PsiExpression, mhType: MethodHandleType): MethodHandleType {
        val type = refc.asReferenceType()
        val name = nameExpr.getConstantOfType<String>() ?: return mhType
        return enhanceVarargsIfKnown(type, mhType) {
            it.findMethodsByName(name, true)
        }
    }

    fun findStaticGetter(refc: PsiExpression, type: PsiExpression): MethodHandleType {
        refc.asReferenceType()
        val returnType = type.asNonVoidType()
        return complete(returnType, listOf())
    }

    fun findStaticSetter(refc: PsiExpression, type: PsiExpression): MethodHandleType {
        refc.asReferenceType()
        val paramType = type.asNonVoidType()
        return complete(ExactType.voidType, listOf(paramType))
    }

    fun findVirtual(refc: PsiExpression, nameExpr: PsiExpression, type: MethodHandleType): MethodHandleType {
        val referenceClass = refc.asReferenceType()
        // TODO not exactly correct, receiver could be restricted to lookup class
        val name = nameExpr.getConstantOfType<String>() ?: return prependParameter(type, referenceClass)
        return prependParameter(enhanceVarargsIfKnown(referenceClass, type) {
            it.findMethodsByName(name, true)
        }, referenceClass)
    }

    private fun prependParameter(
        mhType: MethodHandleType,
        paramType: Type
    ): MethodHandleType {
        val pt = CompleteTypeList(listOf(paramType)).addAllAt(1, mhType.parameterTypes)
        return mhType.withParameterTypes(pt).withVarargs(mhType.varargs)
    }

    private fun PsiExpression.asReferenceType(): Type {
        val referenceClass = this.asType()
        if (referenceClass.isPrimitive()) {
            emitMustBeReferenceType<Type>(this, referenceClass)
            return TopType
        }
        return referenceClass
    }

    private fun PsiExpression.asNonVoidType(): Type {
        val nonVoid = this.asType()
        val voidType = ExactType.voidType
        if (nonVoid == voidType) {
            emitMustNotBeVoid<Type>(this)
            return TopType
        }
        return nonVoid
    }

    private fun enhanceVarargsIfKnown(
        referenceClass: Type,
        methodHandleType: MethodHandleType,
        methods: (PsiClass) -> Array<PsiMethod>
    ): MethodHandleType {
        if (referenceClass is ExactType) {
            val method = PsiTypesUtil.getPsiClass(referenceClass.psiType)?.let {
                findMethodMatching(methodHandleType, methods(it))
            }
            val varargs = method?.isVarArgs.toTriState()
            return methodHandleType.withVarargs(varargs)
        }
        return methodHandleType
    }

    fun findStaticVarHandle(declExpr: PsiExpression, typeExpr: PsiExpression): VarHandleType {
        declExpr.asReferenceType() // problem reporting only, not needed otherwise
        val type = typeExpr.asNonVoidType()
        return CompleteVarHandleType(type, CompleteTypeList(listOf()))
    }

    fun findVarHandle(recvExpr: PsiExpression, typeExpr: PsiExpression): VarHandleType {
        val recv = recvExpr.asReferenceType()
        val type = typeExpr.asNonVoidType()
        return CompleteVarHandleType(type, CompleteTypeList(listOf(recv)))
    }
}
