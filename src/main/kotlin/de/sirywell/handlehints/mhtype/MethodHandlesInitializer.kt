package de.sirywell.handlehints.mhtype

import com.intellij.psi.*
import de.sirywell.handlehints.*
import de.sirywell.handlehints.MethodHandleBundle.message
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

private const val VAR_HANDLE_FQN = "java.lang.invoke.VarHandle"

/**
 * Contains methods from [java.lang.invoke.MethodHandles] that create
 * new [java.lang.invoke.MethodHandle]s.
 */
class MethodHandlesInitializer(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    private val topType = MethodHandleType(TopSignature)

    fun arrayConstructor(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()

        return MethodHandleType(complete(arrayType, listOf(ExactType.intType)))
    }

    fun arrayElementGetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = if (arrayType !is ExactType) {
            arrayType
        } else {
            ExactType((arrayType.psiType as PsiArrayType).componentType)
        }
        return MethodHandleType(complete(componentType, listOf(arrayType, ExactType.intType)))
    }

    fun arrayElementSetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = if (arrayType !is ExactType) {
            arrayType
        } else {
            ExactType((arrayType.psiType as PsiArrayType).componentType)
        }
        return MethodHandleType(complete(ExactType.voidType, listOf(arrayType, ExactType.intType, componentType)))
    }

    // arrayElementVarHandle() no VarHandle support

    fun arrayLength(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        return MethodHandleType(complete(ExactType.intType, listOf(arrayType)))
    }

    // byteArray/BufferViewVarHandle() no VarHandle support

    fun constant(typeExpr: PsiExpression, valueExpr: PsiExpression): MethodHandleType {
        val type = typeExpr.asType()
        val valueType = valueExpr.type?.let { ExactType(it) } ?: BotType
        if (type == ExactType.voidType) {
            return emitProblem(typeExpr, message("problem.creation.arguments.invalid.type", ExactType.voidType))
        }
        if (!typesAreCompatible(type, valueType, valueExpr)) {
            return emitProblem(valueExpr, message("problem.general.parameters.expected.type", type, valueType))
        }
        return MethodHandleType(complete(type, listOf()))
    }

    fun empty(mhType: MethodHandleType): MethodHandleType = mhType

    fun exactInvoker(mhType: MethodHandleType, methodHandleType: PsiType) = invoker(mhType, methodHandleType)

    fun identity(typeExpr: PsiExpression): MethodHandleType {
        val type = typeExpr.asType()
        if (type == ExactType.voidType) {
            return emitProblem(typeExpr, message("problem.merging.general.typeMustNotBe", ExactType.voidType))
        }
        return MethodHandleType(complete(type, listOf(type)))
    }

    fun invoker(mhType: MethodHandleType, methodHandleType: PsiType): MethodHandleType {
        val signature = mhType.signature
        val pt = signature.parameterList.addAllAt(0, CompleteParameterList(listOf(ExactType(methodHandleType))))
        return MethodHandleType(complete(signature.returnType, pt))
    }

    fun spreadInvoker(type: MethodHandleType, leadingArgCount: Int, objectType: PsiType): MethodHandleType {
        if (leadingArgCount < 0) return topType
        val signature = type.signature
        val parameterList = signature.parameterList as? CompleteParameterList ?: return topType
        if (leadingArgCount >= parameterList.size) return topType
        val keep = parameterList.parameterTypes.subList(0, leadingArgCount).toMutableList()
        keep.add(ExactType(objectType.createArrayType()))
        return MethodHandleType(complete(signature.returnType, keep))
    }

    fun throwException(returnTypeExpr: PsiExpression, exTypeExpr: PsiExpression): MethodHandleType {
        return MethodHandleType(complete(returnTypeExpr.asType(), listOf(exTypeExpr.asType())))
    }

    fun zero(type: Type): MethodHandleType {
        return MethodHandleType(complete(type, listOf()))
    }

    private fun PsiExpression.asArrayType(): Type {
        val referenceClass = this.asType()
        if (referenceClass is ExactType && referenceClass.psiType !is PsiArrayType) {
            emitMustBeArrayType(this, referenceClass)
            return TopType
        }
        return referenceClass
    }

    private fun typesAreCompatible(
        left: Type,
        right: Type,
        context: PsiElement
    ): Boolean {
        val l = (left as? ExactType)?.psiType ?: return true // assume compatible if unknown
        var r = (right as? ExactType)?.psiType ?: return true // assume compatible if unknown
        if (r is PsiPrimitiveType) {
            r.getBoxedType(context)?.let { r = it }
        }
        return l.isConvertibleFrom(r)
    }

    fun varHandleInvoker(
        accessModeExpr: PsiExpression,
        methodTypeExpr: PsiExpression,
        exact: Boolean,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val accessType = accessModeExpr.getConstantOfType<PsiField>()
            ?.let { accessTypeForAccessModeName(it.name) }
        var type = ssaAnalyzer.mhType(methodTypeExpr, block) ?: MethodHandleType(BotSignature)
        if (accessType != null) {
            type = checkAccessModeType(accessType, type, exact, methodTypeExpr)
        }
        val varHandleType = PsiType.getTypeByName(VAR_HANDLE_FQN, methodTypeExpr.project, methodTypeExpr.resolveScope)
        return MethodHandleType(
            type.signature.withParameterTypes(
                type.signature.parameterList.addAllAt(0, CompleteParameterList(listOf(ExactType(varHandleType))))
            )
        )
    }

    private fun checkAccessModeType(
        accessType: AccessType,
        type: MethodHandleType,
        exact: Boolean,
        context: PsiElement
    ): MethodHandleType {
        // TODO do more validation using accessType
        // e.g. minimum parameter count
        when (accessType) {
            AccessType.GET -> {
                if (exact && type.signature.returnType.match(PsiTypes.voidType()) == TriState.YES) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.notAllowedX", PsiTypes.voidType().presentableText)
                    )
                }

            }

            AccessType.SET -> {
                if (exact && type.signature.returnType.match(PsiTypes.voidType()) == TriState.NO) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.requiredX", PsiTypes.voidType().presentableText)
                    )
                }
            }

            AccessType.COMPARE_AND_SET -> {
                if (exact && type.signature.returnType.match(PsiTypes.booleanType()) == TriState.NO) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.requiredX", PsiTypes.booleanType().presentableText)
                    )
                }
                // 2 trailing param types must be equal
            }

            AccessType.COMPARE_AND_EXCHANGE -> {
                // 2 trailing param types must be equal
                // return type must match trailing param
            }

            AccessType.GET_AND_UPDATE -> {
                if (exact && type.signature.returnType.match(PsiTypes.voidType()) == TriState.YES) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.notAllowedX", PsiTypes.voidType().presentableText)
                    )
                }
                // return type must match trailing param
            }
        }
        return type
    }

}
