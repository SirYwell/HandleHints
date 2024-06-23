package de.sirywell.handlehints.dfa

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.controlFlow.ControlFlow
import com.intellij.psi.controlFlow.ReadVariableInstruction
import com.intellij.psi.controlFlow.WriteVariableInstruction
import de.sirywell.handlehints.*
import de.sirywell.handlehints.dfa.SsaConstruction.*
import de.sirywell.handlehints.mhtype.*
import de.sirywell.handlehints.type.*

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

    private val ssaConstruction = SsaConstruction<TypeLatticeElement<*>>(controlFlow)
    private val methodHandlesMerger = MethodHandlesMerger(this)
    private val methodHandlesInitializer = MethodHandlesInitializer(this)
    private val methodHandleTransformer = MethodHandleTransformer(this)
    private val lookupHelper = LookupHelper(this)
    private val methodTypeHelper = MethodTypeHelper(this)

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
                .reduce { acc, mhType -> join(acc, mhType) }
            typeData[controlFlow.getElement(index)] = type
        } else {
            typeData[controlFlow.getElement(index)] = typeData[instruction.variable] ?: return
        }
    }

    private fun join(first: TypeLatticeElement<*>, second: TypeLatticeElement<*>): TypeLatticeElement<*> {
        if (first is MethodHandleType && second is MethodHandleType) {
            return first.join(second)
        }
        if (first is VarHandleType && second is VarHandleType) {
            return first.join(second)
        }
        throw AssertionError("unexpected join: $first - $second")
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
            is PsiInstanceOfExpression -> element.operand
            is PsiSwitchLabeledRuleStatement -> element.enclosingSwitchBlock?.expression ?: return
            else -> TODO("Not supported: ${element.javaClass}")
        }
        val type = typeData<MethodHandleType>(expression) ?: resolveType(expression, block)
        type?.let { typeData[expression] = it }
        ssaConstruction.writeVariable(instruction.variable, block, Holder(type ?: notConstant()))
        if (type != null) {
            typeData[controlFlow.getElement(index)] = type
        }
    }

    /**
     * Returns true if the variable type has no [MethodHandleType]
     */
    private fun isUnrelated(variable: PsiVariable): Boolean {
        return isUnrelated(variable.type, variable)    }

    private fun isUnrelated(type: PsiType, context: PsiElement): Boolean {
        return type != methodTypeType(context)
                && type != methodHandleType(context)
                && type != varHandleType(context)
    }

    fun resolveType(expression: PsiExpression, block: Block): TypeLatticeElement<*>? {
        if (expression.type == null || isUnrelated(expression.type!!, expression)) {
            return noMatch() // unrelated
        }
        return resolveMhTypePlain(expression, block)
    }

    private fun resolveMhTypePlain(expression: PsiExpression, block: Block): TypeLatticeElement<*>? {
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
                            methodTypeHelper.methodType(
                                arguments[0],
                                arguments[1].methodHandleType(block) ?: notConstant()
                            )
                        } else {
                            methodTypeHelper.methodType(arguments)
                        }
                    }

                    "unwrap" -> methodTypeHelper.unwrap(qualifier?.methodHandleType(block) ?: return noMatch())
                    "wrap" -> methodTypeHelper.wrap(expression, qualifier?.methodHandleType(block) ?: return noMatch())
                    "dropParameterTypes" -> {
                        val mhType = qualifier?.methodHandleType(block) ?: return noMatch()
                        if (arguments.size != 2) return noMatch()
                        val (start, end) = arguments
                        methodTypeHelper.dropParameterTypes(mhType, start, end)
                    }

                    "insertParameterTypes" -> {
                        val mhType = qualifier?.methodHandleType(block) ?: notConstant()
                        if (arguments.isEmpty()) return noMatch()
                        methodTypeHelper.insertParameterTypes(mhType, arguments[0], arguments.drop(1))
                    }

                    "changeParameterType" -> {
                        val mhType = qualifier?.methodHandleType(block) ?: notConstant()
                        if (arguments.size != 2) return noMatch()
                        val (num, type) = arguments
                        methodTypeHelper.changeParameterType(mhType, num, type)
                    }

                    "changeReturnType" -> {
                        val mhType = qualifier?.methodHandleType(block) ?: notConstant()
                        if (arguments.size != 1) return noMatch()
                        val type = arguments[0]
                        methodTypeHelper.changeReturnType(mhType, type)
                    }

                    "appendParameterTypes" ->
                        methodTypeHelper.appendParameterTypes(qualifier?.methodHandleType(block) ?: return noMatch(), arguments)

                    "erase" ->
                        methodTypeHelper.erase(qualifier?.methodHandleType(block) ?: return noMatch(), expression)

                    "generic" ->
                        methodTypeHelper.generic(qualifier?.methodHandleType(block) ?: return noMatch(), objectType(expression))

                    "genericMethodType" -> {
                        val size = arguments.size
                        if (size != 1 && arguments.size != 2) return noMatch()
                        val finalArray =
                            if (size == 1) false else arguments[1].getConstantOfType<Boolean>() ?: return notConstant()
                        methodTypeHelper.genericMethodType(arguments[0], finalArray, objectType(expression))
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
                    "asFixedArity" -> {
                        if (arguments.isNotEmpty()) return noMatch()
                        methodHandleTransformer.asFixedArity(qualifier?.methodHandleType(block)?: notConstant())
                    }
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
                    "type" -> qualifier?.methodHandleType(block)
                        ?.signature?.withVarargs(TriState.NO) // if ever used somewhere else, assume non-varargs
                        ?.let { MethodHandleType(it) }

                    in objectMethods -> noMethodHandle()
                    else -> warnUnsupported(expression, "MethodHandle")
                }
            } else if (receiverIsLookup(expression)) {
                return lookup(expression, arguments, block)
            }
        } else if (expression is PsiReferenceExpression) {
            val variable = expression.resolve() as? PsiVariable ?: return noMatch()
            val value = ssaConstruction.readVariable(variable, block)
            return if (value is Holder) {
                value.value
            } else if (variable is PsiField) {
                typeData[variable] ?: noMatch()
            } else {
                // avoid StackOverflowError due to calling this method in MethodHandleTypeResolver
                noMatch()
            }
        }
        // TODO is there a better way for this?
        val resolver = HandleTypeResolver(this, block, MethodHandleType(BotSignature), MethodHandleType::class)
        val resolver2 = HandleTypeResolver(this, block, BotVarHandleType, VarHandleType::class)
        expression.accept(resolver)
        expression.accept(resolver2)
        return resolver.result
    }

    private fun lookup(
        expression: PsiMethodCallExpression,
        arguments: List<PsiExpression>,
        block: Block
    ): TypeLatticeElement<*>? {
        return when (expression.methodName) {
            "findConstructor" -> {
                if (arguments.size != 2) return noMatch()
                lookupHelper.findConstructor(arguments[0], arguments[1], block)
            }

            "findGetter" -> findAccessor(arguments, lookupHelper::findGetter)
            "findSetter" -> findAccessor(arguments, lookupHelper::findSetter)
            "findSpecial" -> {
                if (arguments.size != 4) return noMatch()
                val (refc, name, type, specialCaller) = arguments
                val t = type.methodHandleType(block) ?: return notConstant()
                lookupHelper.findSpecial(refc, name, t, specialCaller)
            }

            "findStatic" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, name, type) = arguments
                val t = type.methodHandleType(block) ?: return notConstant()
                lookupHelper.findStatic(refc, name, t)
            }

            "findStaticGetter" -> findAccessor(arguments, lookupHelper::findStaticGetter)
            "findStaticSetter" -> findAccessor(arguments, lookupHelper::findStaticSetter)
            "findVirtual" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, name, type) = arguments
                val t = type.methodHandleType(block) ?: return notConstant()
                lookupHelper.findVirtual(refc, name, t)
            }
            "findStaticVarHandle" -> findAccessor(arguments, lookupHelper::findStaticVarHandle)
            "findVarHandle" -> findAccessor(arguments, lookupHelper::findVarHandle)

            "accessClass",
            "defineClass",
            "defineHiddenClass",
            "defineHiddenClassWithClassData",
            "dropLookupMode",
            "ensureInitialized",
            "findClass",
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

    private fun <T> findAccessor(
        arguments: List<PsiExpression>,
        resolver: (PsiExpression, PsiExpression) -> T
    ): T? {
        if (arguments.size != 3) return noMatch()
        val (refc, _, type) = arguments
        return resolver(refc, type)
    }

    private fun noMethodHandle(): MethodHandleType? = null

    private fun methodHandles(
        expression: PsiMethodCallExpression,
        arguments: List<PsiExpression>,
        block: Block
    ): TypeLatticeElement<*>? {
        return when (expression.methodName) {
            "arrayConstructor" -> singleParameter(arguments, methodHandlesInitializer::arrayConstructor)
            "arrayElementGetter" -> singleParameter(arguments, methodHandlesInitializer::arrayElementGetter)
            "arrayElementSetter" -> singleParameter(arguments, methodHandlesInitializer::arrayElementSetter)
            "arrayElementVarHandle" -> singleParameter(arguments, methodHandlesInitializer::arrayElementVarHandle)
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
                methodHandlesMerger.dropArguments(arguments[0], i, arguments.subList(2), block)
            }

            "dropArgumentsToMatch" -> notConstant() // likely too difficult to get something precise here
            "dropReturn" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesMerger.dropReturn(arguments[0], block)
            }

            "empty" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.empty(arguments[0].methodHandleType(block) ?: return noMatch())
            }

            "exactInvoker" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.exactInvoker(
                    arguments[0].methodHandleType(block) ?: return noMatch(),
                    methodHandleType(expression)
                )
            }

            "explicitCastArguments" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesMerger.explicitCastArguments(arguments[0], arguments[1], block)
            }

            "filterArguments" -> {
                if (arguments.size < 2) return noMatch()
                val target = arguments[0].methodHandleType(block) ?: return noMatch()
                val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                val filter = arguments.drop(2).map { it.methodHandleType(block) ?: return noMatch() }
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
                        target = arguments[0].methodHandleType(block) ?: return noMatch()
                        pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                        combiner = arguments[2].methodHandleType(block) ?: return noMatch()
                    }

                    2 -> {
                        target = arguments[0].methodHandleType(block) ?: return noMatch()
                        pos = 0
                        combiner = arguments[1].methodHandleType(block) ?: return noMatch()
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
                methodHandlesMerger.insertArguments(
                    arguments[0],
                    arguments[1],
                    arguments.subList(2),
                    block
                )
            }

            "invoker" -> {
                if (arguments.size != 1) return noMatch()
                methodHandlesInitializer.invoker(
                    arguments[0].methodHandleType(block) ?: return noMatch(),
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
                    arguments[0].methodHandleType(block) ?: return noMatch(),
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

            "varHandleExactInvoker",
            "varHandleInvoker" -> {
                if (arguments.size != 2) return noMatch()
                methodHandlesInitializer.varHandleInvoker(
                    arguments[0],
                    arguments[1],
                    expression.methodName == "varHandleExactInvoker",
                    block
                )
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

    private fun <T: TypeLatticeElement<*>> noMatch(): T? {
        return null
    }

    private fun notConstant(): MethodHandleType {
        return MethodHandleType(BotSignature)
    }

    private fun singleParameter(
        arguments: List<PsiExpression>,
        factory: (PsiExpression) -> TypeLatticeElement<*>
    ): TypeLatticeElement<*>? {
        if (arguments.size != 1) return noMatch()
        return factory(arguments.first())
    }

    @JvmName("type_extension")
    private fun PsiExpression.type(block: Block): TypeLatticeElement<*>? {
        val mhType = resolveType(this, block)
        typeData[this] = mhType ?: return null
        return mhType
    }

    @JvmName("methodHandleType_extension")
    private fun PsiExpression.methodHandleType(block: Block): MethodHandleType? {
        val type = resolveType(this, block) as? MethodHandleType ?: return null
        typeData[this] = type
        return type
    }

    fun type(expression: PsiExpression, block: Block) = expression.type(block)

    fun methodHandleType(expression: PsiExpression, block: Block) = expression.methodHandleType(block)

    private fun methodHandleType(element: PsiElement): PsiClassType {
        return PsiType.getTypeByName("java.lang.invoke.MethodHandle", element.project, element.resolveScope)
    }

    private fun varHandleType(element: PsiElement): PsiClassType {
        return PsiType.getTypeByName("java.lang.invoke.VarHandle", element.project, element.resolveScope)
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
