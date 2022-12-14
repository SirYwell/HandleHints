package de.sirywell.methodhandleplugin.dfa

import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.codeInspection.dataFlow.CommonDataflow.DataflowResult
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.controlFlow.ControlFlow
import com.intellij.psi.controlFlow.ReadVariableInstruction
import com.intellij.psi.controlFlow.WriteVariableInstruction
import de.sirywell.methodhandleplugin.*
import de.sirywell.methodhandleplugin.dfa.SsaConstruction.*
import de.sirywell.methodhandleplugin.mhtype.*

class SsaAnalyzer(private val controlFlow: ControlFlow, private val typeData: TypeData) {
    companion object {
        private val LOG = Logger.getInstance(SsaAnalyzer::class.java)
        private val objectMethods = setOf(
            "clone",
            "equals",
            "finalize",
            "getClass",
            "hashCode",
            "notify",
            "notifyAll",
            "toString",
            "wait"
        )
    }

    private val ssaConstruction = SsaConstruction<MhType>(controlFlow)

    @Suppress("UnstableApiUsage")
    private var commonDataflowCache: DataflowResult? = null

    fun doTraversal() {
        ssaConstruction.traverse(::onRead, ::onWrite)
    }

    private fun onRead(instruction: ReadVariableInstruction, index: Int, block: Block) {
        if (isUnrelated(instruction.variable)) return
        val value = ssaConstruction.readVariable(instruction.variable, block) ?: return
        if (value is Holder) {
            typeData[controlFlow.getElement(index)] = value.value
        } else if (value is Phi) {
            val type = value.blockToValue.values
                .flatMap { if (it is Holder) listOf(it.value) else resolvePhi(it as Phi) }
                .reduce { acc, mhType -> acc.join(mhType) }
            typeData[controlFlow.getElement(index)] = type
        }
    }

    private fun resolvePhi(phi: Phi<MhType>, mut: MutableList<MhType> = mutableListOf()): List<MhType> {
        phi.blockToValue.values.forEach {
            if (it is Holder) {
                mut.add(it.value)
            } else {
                resolvePhi(it as Phi<MhType>, mut)
            }
        }
        return mut.toList()
    }

    private fun onWrite(instruction: WriteVariableInstruction, index: Int, block: Block) {
        if (isUnrelated(instruction.variable)) return
        val expression = when (val element = controlFlow.getElement(index)) {
            is PsiAssignmentExpression -> element.rExpression!!
            is PsiDeclarationStatement -> instruction.variable.initializer!!
            else -> TODO("Not supported: ${element.javaClass}")
        }
        val mhType = resolveMhType(expression, block)
        ssaConstruction.writeVariable(instruction.variable, block, Holder(mhType ?: Bot))
        if (mhType != null) {
            typeData[controlFlow.getElement(index)] = mhType
        }
    }

    /**
     * Returns true if the variable type has no [MhType]
     */
    private fun isUnrelated(variable: PsiVariable): Boolean {
        return variable.type != methodTypeType(variable) && variable.type != methodHandleType(variable)
    }

    private fun resolveMhType(expression: PsiExpression, block: Block): MhType? {
        if (expression is PsiMethodCallExpression) {
            val arguments = expression.argumentList.expressions.asList()
            if (receiverIsMethodType(expression)) {
                return when (expression.methodName) {
                    "methodType" -> MethodTypeHelper.methodType(arguments.toPsiTypes() ?: return notConstant())
                    "describeConstable",
                    "descriptorString",
                    "hasPrimitives",
                    "hasWrappers",
                    "lastParameterType",
                    "parameterArray",
                    "parameterCount",
                    "parameterList",
                    "parameterType",
                    "toMethodDescriptorString", -> noMethodHandle()

                    in objectMethods -> noMethodHandle()
                    else -> warnUnsupported(expression, "MethodType")
                }
            } else if (receiverIsMethodHandles(expression)) {
                return methodHandles(expression, arguments, block)
            } else if (receiverIsMethodHandle(expression)) {
                return when (expression.methodName) {
                    "asCollector" -> TODO()
                    "asFixedArity" -> TODO()
                    "asSpreader" -> TODO()
                    "asType" -> TODO()
                    "asVarargsCollector" -> TODO()
                    "bindTo" -> {
                        if (arguments.size != 1) return noMatch()
                        val target = expression.methodExpression.qualifierExpression ?: return noMatch()
                        val objectType = arguments[0].type ?: return notConstant()
                        MethodHandleTransformer.bindTo(target.mhTypeOrNoMatch(block), objectType)
                    }

                    "withVarargs" -> TODO()
                    "describeConstable",
                    "invoke",
                    "invokeExact",
                    "invokeWithArguments" -> noMethodHandle()

                    in objectMethods -> noMethodHandle()
                    else -> warnUnsupported(expression, "MethodHandle")
                }
            }
        } else if (expression is PsiReferenceExpression) {
            val value = ssaConstruction.readVariable(expression.resolve() as PsiVariable, block)
            if (value is Holder) {
                return value.value
            }
        }
        return noMatch()
    }

    private fun noMethodHandle(): MhType? = null

    private fun methodHandles(
        expression: PsiMethodCallExpression,
        arguments: List<PsiExpression>,
        block: Block
    ): MhType? {
        return when (expression.methodName) {
            "arrayConstructor" -> singleParameter(arguments, MethodHandlesInitializer::arrayConstructor)
            "arrayElementGetter" -> singleParameter(arguments, MethodHandlesInitializer::arrayElementGetter)
            "arrayElementSetter" -> singleParameter(arguments, MethodHandlesInitializer::arrayElementSetter)
            "arrayLength" -> singleParameter(arguments, MethodHandlesInitializer::arrayLength)
            "catchException" -> {
                if (arguments.size != 3) return noMatch()
                val exType = arguments[1].getConstantOfType<PsiType>() ?: return notConstant()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val handler = arguments[2].mhType(block) ?: return noMatch()
                MethodHandlesMerger.catchException(target, exType, handler)
            }

            "collectArguments" -> {
                if (arguments.size != 3) return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val filter = arguments[2].mhType(block) ?: return noMatch()
                MethodHandlesMerger.collectArguments(target, pos, filter)
            }

            "constant" -> {
                if (arguments.size != 2) return noMatch()
                val type = arguments[0].getConstantOfType<PsiType>() ?: return notConstant()
                // TODO warn if value is incompatible with type?
                MethodHandlesInitializer.constant(type)
            }

            "countedLoop" -> justDelegate(arguments, block, min = 3, max = 4) {
                if (arguments.size == 3) {
                    MethodHandlesMerger.countedLoop(it[0], it[1], it[2])
                } else {
                    MethodHandlesMerger.countedLoop(it[0], it[1], it[2], it[3])
                }
            }

            "doWhileLoop" -> justDelegate(arguments, block, exact = 3) {
                // TODO init can be null
                MethodHandlesMerger.doWhileLoop(it[0], it[1], it[2])
            }

            "dropArguments" -> {
                if (arguments.size < 3) return noMatch()
                val i = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val types = arguments.subList(2).toPsiTypes() ?: return notConstant()
                val target = arguments[0].mhType(block) ?: return noMatch()
                MethodHandlesMerger.dropArguments(target, i, types)
            }

            "dropArgumentsToMatch" -> notConstant() // likely too difficult to get something precise here
            "dropReturn" -> justDelegate(arguments, block, exact = 1) {
                MethodHandlesMerger.dropReturn(it[0])
            }

            "empty" -> justDelegate(arguments, block, exact = 1) {
                MethodHandlesInitializer.empty(it[0])
            }

            "exactInvoker" -> justDelegate(arguments, block, exact = 1) {
                MethodHandlesInitializer.exactInvoker(it[0], methodHandleType(expression))
            }

            "explicitCastArguments" -> justDelegate(arguments, block, exact = 2) {
                MethodHandlesMerger.explicitCastArguments(it[0], it[1])
            }

            "filterArguments" -> {
                if (arguments.size < 2) return noMatch()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val filter = arguments.drop(2).map { it.mhType(block) ?: return noMatch() }
                MethodHandlesMerger.filterArguments(target, pos, filter)
            }

            "filterReturnValue" -> justDelegate(arguments, block, exact = 2) {
                MethodHandlesMerger.filterReturnValue(it[0], it[1])
            }

            "foldArguments" -> {
                val target: MhType
                val combiner: MhType
                val pos: Int
                when (arguments.size) {
                    3 -> {
                        target = arguments[0].mhType(block) ?: return noMatch()
                        pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                        combiner = arguments[2].mhType(block) ?: return noMatch()
                    }

                    2 -> {
                        target = arguments[0].mhType(block) ?: return noMatch()
                        pos = 0
                        combiner = arguments[1].mhType(block) ?: return noMatch()
                    }

                    else -> return noMatch()
                }
                MethodHandlesMerger.foldArguments(target, pos, combiner)
            }

            "guardWithTest" -> justDelegate(arguments, block, exact = 3) {
                MethodHandlesMerger.guardWithTest(it[0], it[1], it[2])
            }

            "identity" -> singleParameter(arguments, MethodHandlesInitializer::identity)
            "insertArguments" -> {
                if (arguments.size < 2) return noMatch()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                MethodHandlesMerger.insertArguments(
                    target,
                    pos,
                    arguments.subList(2).map { it.type ?: return notConstant() }
                )
            }

            "invoker" -> justDelegate(arguments, block, exact = 1) {
                MethodHandlesInitializer.invoker(it[0], methodHandleType(expression))
            }

            "iteratedLoop" -> TODO("not implemented")
            "loop" -> notConstant()
            "permuteArguments" -> {
                if (arguments.size < 2) noMatch()
                else {
                    val reorder = arguments.subList(2)
                        .map { it.getConstantOfType<Int>() ?: return notConstant() }
                    MethodHandlesMerger.permuteArguments(
                        arguments[0].mhTypeOrNoMatch(block),
                        arguments[1].mhTypeOrNoMatch(block),
                        reorder
                    )
                }
            }

            "spreadInvoker" -> {
                if (arguments.size != 2) return noMatch()
                val leadingArgCount = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                MethodHandlesInitializer.spreadInvoker(
                    arguments[0].mhTypeOrNoMatch(block),
                    leadingArgCount,
                    objectType(expression)
                )
            }

            "tableSwitch" -> justDelegate(arguments, block, min = 1, factory = {
                MethodHandlesMerger.tableSwitch(it[0], it.drop(1))
            })

            "throwException" -> {
                if (arguments.size != 2) return noMatch()
                val types = arguments.toPsiTypes() ?: return notConstant()
                MethodHandlesInitializer.throwException(types[0], types[1])
            }

            "tryFinally" -> justDelegate(arguments, block, exact = 2) {
                MethodHandlesMerger.tryFinally(it[0], it[1])
            }

            "zero" -> singleParameter(arguments, MethodHandlesInitializer::zero)
            "byteArrayViewVarHandle",
            "byteBufferViewVarHandle",
            "classData",
            "classDataAt",
            "lookup",
            "privateLookupIn",
            "publicLookup",
            "reflectAs" -> noMethodHandle()

            in objectMethods -> noMethodHandle()
            else -> warnUnsupported(expression, "MethodHandles")
        }
    }

    private fun noMatch(): MhType {
        return Bot
    }

    private fun notConstant(): MhType {
        return Bot
    }

    @Suppress("UnstableApiUsage")
    private inline fun <reified T> PsiExpression.getConstantOfType(): T? {
        if (commonDataflowCache == null) {
            commonDataflowCache = CommonDataflow.getDataflowResult(this)
        }
        return commonDataflowCache
            ?.getDfType(this)
            ?.getConstantOfType(T::class.java)
    }

    private fun List<PsiExpression>.toPsiTypes(): List<PsiType>? {
        return this.map { it.getConstantOfType<PsiType>() ?: return null }
    }

    private inline fun <reified T> singleParameter(arguments: List<PsiExpression>, factory: (T) -> MhType): MhType {
        if (arguments.size != 1) return noMatch()
        val type = arguments[0].getConstantOfType<T>() ?: return notConstant()
        return factory(type)
    }

    private fun justDelegate(
        arguments: List<PsiExpression>,
        block: Block,
        min: Int = 0,
        max: Int = Int.MAX_VALUE,
        factory: (List<MhType>) -> MhType
    ): MhType {
        if (arguments.size < min) return noMatch()
        if (arguments.size > max) return noMatch()
        return factory(arguments.map { it.mhType(block) ?: noMatch() })
    }

    private fun justDelegate(
        arguments: List<PsiExpression>,
        block: Block,
        exact: Int,
        factory: (List<MhType>) -> MhType
    ): MhType {
        return justDelegate(arguments, block, min = exact, max = exact, factory)
    }

    private fun PsiExpression.mhType(block: Block): MhType? {
        return resolveMhType(this, block)
    }

    private fun PsiExpression.mhTypeOrNoMatch(block: Block): MhType {
        return this.mhType(block) ?: noMatch()
    }

    private fun methodHandleType(element: PsiElement): PsiClassType {
        return PsiType.getTypeByName("java.lang.invoke.MethodHandle", element.project, element.resolveScope)
    }

    private fun methodTypeType(element: PsiElement): PsiClassType {
        return PsiType.getTypeByName("java.lang.invoke.MethodType", element.project, element.resolveScope)
    }

    private fun objectType(element: PsiElement): PsiType {
        return PsiType.getJavaLangObject(element.manager, element.resolveScope)
    }

    private fun warnUnsupported(expression: PsiMethodCallExpression, className: String): Bot {
        LOG.warnOnce("Unsupported method $className#${expression.methodName}")
        return Bot
    }
}
