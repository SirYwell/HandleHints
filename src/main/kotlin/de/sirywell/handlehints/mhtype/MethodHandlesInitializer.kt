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

    private val topType = TopMethodHandleType

    fun arrayConstructor(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()

        return complete(arrayType, listOf(ExactType.intType))
    }

    fun arrayElementGetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = getComponentType(arrayType)
        return complete(componentType, listOf(arrayType, ExactType.intType))
    }

    fun arrayElementSetter(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = getComponentType(arrayType)
        return complete(ExactType.voidType, listOf(arrayType, ExactType.intType, componentType))
    }

    fun arrayElementVarHandle(arrayClass: PsiExpression): VarHandleType {
        val arrayType = arrayClass.asArrayType()
        val componentType = getComponentType(arrayType)
        return CompleteVarHandleType(componentType, CompleteParameterList(listOf(arrayType, ExactType.intType)))
    }

    private fun getComponentType(arrayType: Type) = if (arrayType !is ExactType) {
        arrayType
    } else {
        ExactType((arrayType.psiType as PsiArrayType).componentType)
    }

    fun arrayLength(arrayClass: PsiExpression): MethodHandleType {
        val arrayType = arrayClass.asArrayType()
        return complete(ExactType.intType, listOf(arrayType))
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
        return complete(type, listOf())
    }

    fun empty(mhType: MethodHandleType): MethodHandleType = mhType

    fun exactInvoker(mhType: MethodHandleType, methodHandleType: PsiType) = invoker(mhType, methodHandleType)

    fun identity(typeExpr: PsiExpression): MethodHandleType {
        val type = typeExpr.asType()
        if (type == ExactType.voidType) {
            return emitProblem(typeExpr, message("problem.merging.general.typeMustNotBe", ExactType.voidType))
        }
        return complete(type, listOf(type))
    }

    fun invoker(mhType: MethodHandleType, methodHandleType: PsiType): MethodHandleType {
        val pt = mhType.parameterList.addAllAt(0, CompleteParameterList(listOf(ExactType(methodHandleType))))
        return complete(mhType.returnType, pt)
    }

    fun spreadInvoker(type: MethodHandleType, leadingArgCount: Int, objectType: PsiType): MethodHandleType {
        if (leadingArgCount < 0) return topType
        val parameterList = type.parameterList as? CompleteParameterList ?: return topType
        if (leadingArgCount >= parameterList.size) return topType
        val keep = parameterList.parameterTypes.subList(0, leadingArgCount).toMutableList()
        keep.add(ExactType(objectType.createArrayType()))
        return complete(type.returnType, keep)
    }

    fun throwException(returnTypeExpr: PsiExpression, exTypeExpr: PsiExpression): MethodHandleType {
        return complete(returnTypeExpr.asType(), listOf(exTypeExpr.asType()))
    }

    fun zero(type: Type): MethodHandleType {
        return complete(type, listOf())
    }

    private fun PsiExpression.asArrayType(): Type {
        val referenceClass = this.asType()
        if (referenceClass is ExactType && referenceClass.psiType !is PsiArrayType) {
            return emitMustBeArrayType(this, referenceClass)
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
        var type = ssaAnalyzer.methodHandleType(methodTypeExpr, block) ?: BotMethodHandleType
        if (accessType != null) {
            type = checkAccessModeType(accessType, type, exact, methodTypeExpr)
        }
        val varHandleType = PsiType.getTypeByName(VAR_HANDLE_FQN, methodTypeExpr.project, methodTypeExpr.resolveScope)
        return type.withParameterTypes(
                type.parameterList.addAllAt(0, CompleteParameterList(listOf(ExactType(varHandleType))))
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
                if (exact && type.returnType.match(PsiTypes.voidType()) == TriState.YES) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.notAllowedX", PsiTypes.voidType().presentableText)
                    )
                }

            }

            AccessType.SET -> {
                if (exact && type.returnType.match(PsiTypes.voidType()) == TriState.NO) {
                    return emitProblem(
                        context,
                        message("problem.general.returnType.requiredX", PsiTypes.voidType().presentableText)
                    )
                }
            }

            AccessType.COMPARE_AND_SET -> {
                if (exact && type.returnType.match(PsiTypes.booleanType()) == TriState.NO) {
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
                if (exact && type.returnType.match(PsiTypes.voidType()) == TriState.YES) {
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
