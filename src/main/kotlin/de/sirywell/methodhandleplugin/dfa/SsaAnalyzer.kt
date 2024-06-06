package de.sirywell.methodhandleplugin.dfa

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.controlFlow.ControlFlow
import com.intellij.psi.controlFlow.ReadVariableInstruction
import com.intellij.psi.controlFlow.WriteVariableInstruction
import de.sirywell.methodhandleplugin.*
import de.sirywell.methodhandleplugin.dfa.SsaConstruction.*
import de.sirywell.methodhandleplugin.mhtype.*
import de.sirywell.methodhandleplugin.type.BotSignature
import de.sirywell.methodhandleplugin.type.MethodHandleType

class SsaAnalyzer(private val controlFlow: ControlFlow, val typeData: TypeData) {
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

    private val ssaConstruction = SsaConstruction<MethodHandleType>(controlFlow)
    private val methodHandlesMerger = MethodHandlesMerger(this)
    private val methodHandlesInitializer = MethodHandlesInitializer(this)
    private val methodHandleTransformer = MethodHandleTransformer(this)
    private val lookupHelper = LookupHelper(this)

    fun doTraversal() {
        ssaConstruction.traverse(::onRead, ::onWrite)
    }

    private fun onRead(instruction: ReadVariableInstruction, index: Int, block: Block) {
        if (isUnrelated(instruction.variable)) return
        val value = ssaConstruction.readVariable(instruction.variable, block)
        if (value is Holder) {
            typeData[controlFlow.getElement(index)] = value.value
        } else if (value is Phi) {
            val type = value.blockToValue.values
                .flatMap { if (it is Holder) listOf(it.value) else resolvePhi(it as Phi) }
                .reduce { acc, mhType -> acc.join(mhType) }
            typeData[controlFlow.getElement(index)] = type
        } else {
            typeData[controlFlow.getElement(index)] = typeData[instruction.variable] ?: return
        }
    }

    private fun <T> resolvePhi(phi: Phi<T>, mut: MutableList<T> = mutableListOf()): List<T> {
        phi.blockToValue.values.forEach {
            if (it is Holder) {
                mut.add(it.value)
            } else {
                resolvePhi(it as Phi<T>, mut)
            }
        }
        return mut.toList()
    }

    private fun onWrite(instruction: WriteVariableInstruction, index: Int, block: Block) {
        if (isUnrelated(instruction.variable)) return
        val expression = when (val element = controlFlow.getElement(index)) {
            is PsiAssignmentExpression -> element.rExpression!!
            is PsiDeclarationStatement -> instruction.variable.initializer!!
            is PsiField -> element.initializer!!
            else -> TODO("Not supported: ${element.javaClass}")
        }
        val mhType = typeData[expression] ?: resolveMhType(expression, block)
        mhType?.let { typeData[expression] = it }
        ssaConstruction.writeVariable(instruction.variable, block, Holder(mhType ?: notConstant()))
        if (mhType != null) {
            typeData[controlFlow.getElement(index)] = mhType
        }
    }

    /**
     * Returns true if the variable type has no [MethodHandleType]
     */
    private fun isUnrelated(variable: PsiVariable): Boolean {
        return variable.type != methodTypeType(variable) && variable.type != methodHandleType(variable)
    }

    private fun resolveMhType(expression: PsiExpression, block: Block): MethodHandleType? {
        return resolveMhTypePlain(expression, block)
    }

    private fun resolveMhTypePlain(expression: PsiExpression, block: Block): MethodHandleType? {
        if (expression is PsiLiteralExpression && expression.value == null) return null
        if (expression is PsiMethodCallExpression) {
            val arguments = expression.argumentList.expressions.asList()
            val methodExpression = expression.methodExpression
            val qualifier = methodExpression.qualifierExpression
            if (receiverIsMethodType(expression)) {
                return when (expression.methodName) {
                    "methodType" -> {
                        if (arguments.isEmpty()) return noMatch()
                        if (arguments.size == 2 && arguments[1].type == methodTypeType(expression)) {
                            MethodTypeHelper.methodType(
                                arguments[0],
                                arguments[1].mhType(block) ?: notConstant()
                            )
                        } else {
                            MethodTypeHelper.methodType(arguments)
                        }
                    }

                    "unwrap" -> MethodTypeHelper.unwrap(qualifier?.mhType(block) ?: return noMatch())
                    "wrap" -> MethodTypeHelper.wrap(expression, qualifier?.mhType(block) ?: return noMatch())
                    "dropParameterTypes" -> {
                        val mhType = qualifier?.mhType(block) ?: return noMatch()
                        if (arguments.size != 2) return noMatch()
                        val (start, end) = arguments
                        MethodTypeHelper.dropParameterTypes(mhType, start, end)
                    }

                    "insertParameterTypes" -> {
                        val mhType = qualifier?.mhType(block) ?: notConstant()
                        if (arguments.isEmpty()) return noMatch()
                        MethodTypeHelper.insertParameterTypes(mhType, arguments[0], arguments.drop(1))
                    }

                    "changeParameterType" -> {
                        val mhType = qualifier?.mhType(block) ?: notConstant()
                        if (arguments.size != 2) return noMatch()
                        val (num, type) = arguments
                        MethodTypeHelper.changeParameterType(mhType, num, type)
                    }

                    "changeReturnType" -> {
                        val mhType = qualifier?.mhType(block) ?: notConstant()
                        if (arguments.size != 1) return noMatch()
                        val type = arguments[0]
                        MethodTypeHelper.changeReturnType(mhType, type)
                    }

                    "appendParameterTypes" ->
                        MethodTypeHelper.appendParameterTypes(qualifier?.mhType(block) ?: return noMatch(), arguments)

                    "erase" ->
                        MethodTypeHelper.erase(qualifier?.mhType(block) ?: return noMatch(), expression)

                    "generic" ->
                        MethodTypeHelper.generic(qualifier?.mhType(block) ?: return noMatch(), objectType(expression))

                    "genericMethodType" -> {
                        val size = arguments.size
                        if (size != 1 && arguments.size != 2) return noMatch()
                        val finalArray =
                            if (size == 1) false else arguments[1].getConstantOfType<Boolean>() ?: return notConstant()
                        MethodTypeHelper.genericMethodType(arguments[0], finalArray, objectType(expression))
                    }

                    "fromMethodDescriptorString" -> notConstant() // not supported

                    "describeConstable",
                    "descriptorString",
                    "hasPrimitives",
                    "hasWrappers",
                    "lastParameterType",
                    "parameterArray",
                    "parameterCount",
                    "parameterList",
                    "parameterType",
                    "toMethodDescriptorString",
                        -> noMethodHandle()

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
                        val target = qualifier ?: return noMatch()
                        methodHandleTransformer.bindTo(target, arguments[0], block)
                    }

                    "withVarargs" -> TODO()
                    "describeConstable",
                    "invoke",
                    "invokeExact",
                    "invokeWithArguments" -> noMethodHandle()

                    in objectMethods -> noMethodHandle()
                    else -> warnUnsupported(expression, "MethodHandle")
                }
            } else if (receiverIsLookup(expression)) {
                return lookup(expression, arguments, block)
            }
        } else if (expression is PsiReferenceExpression) {
            val variable = expression.resolve() as? PsiVariable ?: return noMatch()
            val value = ssaConstruction.readVariable(variable, block)
            if (value is Holder) {
                return value.value
            } else if (variable is PsiField) {
                return typeData[variable] ?: noMatch()
            }
        }
        return noMatch()
    }

    private fun lookup(
        expression: PsiMethodCallExpression,
        arguments: List<PsiExpression>,
        block: Block
    ): MethodHandleType? {
        return when (expression.methodName) {
            "findConstructor" -> {
                if (arguments.size != 2) return noMatch()
                lookupHelper.findConstructor(arguments[0], arguments[1], block)
            }

            "findGetter" -> findAccessor(arguments, lookupHelper::findGetter)
            "findSetter" -> findAccessor(arguments, lookupHelper::findSetter)
            "findSpecial" -> {
                if (arguments.size != 4) return noMatch()
                val (refc, _, type, specialCaller) = arguments
                val t = type.mhType(block) ?: return notConstant()
                lookupHelper.findSpecial(refc, t, specialCaller)
            }

            "findStatic" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, _, type) = arguments
                val t = type.mhType(block) ?: return notConstant()
                lookupHelper.findStatic(refc, t)
            }

            "findStaticGetter" -> findAccessor(arguments, lookupHelper::findStaticGetter)
            "findStaticSetter" -> findAccessor(arguments, lookupHelper::findStaticSetter)
            "findVirtual" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, _, type) = arguments
                val t = type.mhType(block) ?: return notConstant()
                lookupHelper.findVirtual(refc, t)
            }

            "accessClass",
            "defineClass",
            "defineHiddenClass",
            "defineHiddenClassWithClassData",
            "dropLookupMode",
            "ensureInitialized",
            "findClass",
            "findStaticVarHandle",
            "findVarHandle",
            "hasFullPrivilegeAccess",
            "hasPrivateAccess",
            "in",
            "lookupClass",
            "previousLookupClass",
            "revealDirect",
            "unreflect",
            "unreflectConstructor",
            "unreflectGetter",
            "unreflectSetter",
            "unreflectSpecial",
            "unreflectVarHandle"
                -> noMethodHandle()

            in objectMethods -> noMethodHandle()
            else -> warnUnsupported(expression, "MethodHandles.Lookup")
        }
    }

    private fun findAccessor(
        arguments: List<PsiExpression>,
        resolver: (PsiExpression, PsiExpression) -> MethodHandleType
    ): MethodHandleType? {
        if (arguments.size != 3) return noMatch()
        val (refc, _, type) = arguments
        return resolver(refc, type)
    }

    private fun noMethodHandle(): MethodHandleType? = null

    private fun methodHandles(
        expression: PsiMethodCallExpression,
        arguments: List<PsiExpression>,
        block: Block
    ): MethodHandleType? {
        return when (expression.methodName) {
            "arrayConstructor" -> singleParameter(arguments, methodHandlesInitializer::arrayConstructor)
            "arrayElementGetter" -> singleParameter(arguments, methodHandlesInitializer::arrayElementGetter)
            "arrayElementSetter" -> singleParameter(arguments, methodHandlesInitializer::arrayElementSetter)
            "arrayLength" -> singleParameter(arguments, methodHandlesInitializer::arrayLength)
            "catchException" -> {
                if (arguments.size != 3) return noMatch()
                methodHandlesMerger.catchException(arguments[0], arguments[1], arguments[2], block)
            }

            "collectArguments" -> {
                if (arguments.size != 3) return noMatch()
                methodHandlesMerger.collectArguments(arguments[0], arguments[1], arguments[2], block)
            }

            "constant" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesInitializer.constant(arguments[0], arguments[1])
            }

            "countedLoop" -> {
                when (arguments.size) {
                    3 -> methodHandlesMerger.countedLoop(arguments[0], arguments[1], arguments[2], block)
                    4 -> methodHandlesMerger.countedLoop(arguments[0], arguments[1], arguments[2], arguments[3], block)
                    else -> noMatch()
                }
            }

            "doWhileLoop" -> {
                if (arguments.size != 3) {
                    return noMatch()
                }
                methodHandlesMerger.doWhileLoop(arguments[0], arguments[1], arguments[2], block)
            }

            "dropArguments" -> {
                if (arguments.size < 3) return noMatch()
                val i = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val target = arguments[0].mhType(block) ?: return noMatch()
                methodHandlesMerger.dropArguments(target, i, arguments.subList(2))
            }

            "dropArgumentsToMatch" -> notConstant() // likely too difficult to get something precise here
            "dropReturn" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesMerger.dropReturn(arguments[0], block)
            }

            "empty" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.empty(arguments[0].mhType(block) ?: return noMatch())
            }

            "exactInvoker" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.exactInvoker(
                    arguments[0].mhType(block) ?: return noMatch(),
                    methodHandleType(expression)
                )
            }

            "explicitCastArguments" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesMerger.explicitCastArguments(arguments[0], arguments[1], block)
            }

            "filterArguments" -> {
                if (arguments.size < 2) return noMatch()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val filter = arguments.drop(2).map { it.mhType(block) ?: return noMatch() }
                methodHandlesMerger.filterArguments(target, pos, filter)
            }

            "filterReturnValue" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesMerger.filterReturnValue(arguments[0], arguments[1], block)
            }

            "foldArguments" -> {
                val target: MethodHandleType
                val combiner: MethodHandleType
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
                methodHandlesMerger.foldArguments(target, pos, combiner)
            }

            "guardWithTest" -> {
                if (arguments.size != 3) return noMatch()
                methodHandlesMerger.guardWithTest(arguments[0], arguments[1], arguments[2], block)
            }

            "identity" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.identity(arguments[0])
            }

            "insertArguments" -> {
                if (arguments.size < 2) return noMatch()
                val target = arguments[0].mhType(block) ?: return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                methodHandlesMerger.insertArguments(
                    target,
                    pos,
                    arguments.subList(2)
                )
            }

            "invoker" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.invoker(
                    arguments[0].mhType(block) ?: return noMatch(),
                    methodHandleType(expression)
                )
            }

            "iteratedLoop" -> TODO("not implemented")
            "loop" -> notConstant()
            "permuteArguments" -> {
                if (arguments.size < 2) noMatch()
                else {
                    methodHandlesMerger.permuteArguments(
                        arguments[0],
                        arguments[1],
                        arguments.subList(2),
                        block
                    )
                }
            }

            "spreadInvoker" -> {
                if (arguments.size != 2) return noMatch()
                val leadingArgCount = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                methodHandlesInitializer.spreadInvoker(
                    arguments[0].mhTypeOrNoMatch(block) ?: return noMatch(),
                    leadingArgCount,
                    objectType(expression)
                )
            }

            "tableSwitch" -> {
                if (arguments.isEmpty()) return noMatch()
                methodHandlesMerger.tableSwitch(arguments[0], arguments.drop(1), block)
            }

            "throwException" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesInitializer.throwException(arguments[0], arguments[1])
            }

            "tryFinally" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesMerger.tryFinally(arguments[0], arguments[1], block)
            }

            "zero" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.zero(arguments[0].asType())
            }

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

    private fun noMatch(): MethodHandleType? {
        return null
    }

    private fun notConstant(): MethodHandleType {
        return MethodHandleType(BotSignature)
    }

    private fun singleParameter(
        arguments: List<PsiExpression>,
        factory: (PsiExpression) -> MethodHandleType
    ): MethodHandleType? {
        if (arguments.size != 1) return noMatch()
        return factory(arguments.first())
    }

    @JvmName("mhType_extension")
    private fun PsiExpression.mhType(block: Block): MethodHandleType? {
        val mhType = resolveMhType(this, block)
        typeData[this] = mhType ?: return null
        return mhType
    }

    fun mhType(expression: PsiExpression, block: Block) = expression.mhType(block)

    private fun PsiExpression.mhTypeOrNoMatch(block: Block): MethodHandleType? {
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

    private fun warnUnsupported(expression: PsiMethodCallExpression, className: String): MethodHandleType {
        LOG.warnOnce("Unsupported method $className#${expression.methodName}")
        return MethodHandleType(BotSignature)
    }
}
