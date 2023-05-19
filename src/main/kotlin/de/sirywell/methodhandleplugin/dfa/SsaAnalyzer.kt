package de.sirywell.methodhandleplugin.dfa

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
            is PsiField -> element.initializer!!
            else -> TODO("Not supported: ${element.javaClass}")
        }
        val mhType = typeData[expression] ?: resolveMhType(expression, block)
        mhType?.let { typeData[expression] = it }
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
        return resolveMhTypePlain(expression, block)?.at(expression)
    }
    private fun resolveMhTypePlain(expression: PsiExpression, block: Block): MhType? {
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
                                arguments[1].mhType(block) ?: return notConstant()
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
                        val mhType = qualifier?.mhType(block) ?: return noMatch()
                        if (arguments.isEmpty()) return noMatch()
                        MethodTypeHelper.insertParameterTypes(mhType, arguments[0], arguments.drop(1))
                    }

                    "changeParameterType" -> {
                        val mhType = qualifier?.mhType(block) ?: return noMatch()
                        if (arguments.size != 2) return noMatch()
                        val (num, type) = arguments
                        MethodTypeHelper.changeParameterType(mhType, num, type)
                    }

                    "changeReturnType" -> {
                        val mhType = qualifier?.mhType(block) ?: return noMatch()
                        if (arguments.size != 1) return noMatch()
                        val type = arguments[0]
                        MethodTypeHelper.changeReturnType(mhType, type)
                    }

                    "appendParameterTypes" ->
                        MethodTypeHelper.appendParameterTypes(qualifier?.mhType(block) ?: return noMatch(), arguments)

                    "erase" ->
                        MethodTypeHelper.erase(qualifier?.mhType(block) ?: return noMatch(), objectType(expression))

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
    ): MhType? {
        return when (expression.methodName) {
            "findConstructor" -> {
                if (arguments.size != 2) return noMatch()
                val mhType = arguments[1].mhType(block) ?: return notConstant()
                LookupHelper.findConstructor(arguments[0], mhType)
            }

            "findGetter" -> findAccessor(arguments, LookupHelper::findGetter)
            "findSetter" -> findAccessor(arguments, LookupHelper::findSetter)
            "findSpecial" -> {
                if (arguments.size != 4) return noMatch()
                val (refc, _, type, specialCaller) = arguments
                val t = type.mhType(block) ?: return notConstant()
                LookupHelper.findSpecial(refc, t, specialCaller)
            }

            "findStatic" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, _, type) = arguments
                val t = type.mhType(block) ?: return notConstant()
                LookupHelper.findStatic(refc, t)
            }

            "findStaticGetter" -> findAccessor(arguments, LookupHelper::findStaticGetter)
            "findStaticSetter" -> findAccessor(arguments, LookupHelper::findStaticSetter)
            "findVirtual" -> {
                if (arguments.size != 3) return noMatch()
                val (refc, _, type) = arguments
                val t = type.mhType(block) ?: return notConstant()
                LookupHelper.findVirtual(refc, t)
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
        resolver: (PsiExpression, PsiExpression) -> MhType
    ): MhType {
        if (arguments.size != 3) return noMatch()
        val (refc, _, type) = arguments
        return resolver(refc, type)
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
                MethodHandlesInitializer.constant(type)
            }

            "countedLoop" -> justDelegateAllowNull(arguments, block, min = 3, max = 4) {
                val mhType0 = it[0]
                val mhType1 = it[1]
                val mhType2 = it[2]
                if (mhType0 == null) return@justDelegateAllowNull noMatch()
                if (arguments.size == 3) {
                    if (mhType2 == null) return@justDelegateAllowNull noMatch()
                    MethodHandlesMerger.countedLoop(mhType0, mhType1, mhType2)
                } else {
                    if (mhType1 == null) return@justDelegateAllowNull noMatch()
                    val mhType3 = it[3] ?: return@justDelegateAllowNull noMatch()
                    MethodHandlesMerger.countedLoop(mhType0, mhType1, mhType2, mhType3)
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
    private fun justDelegateAllowNull(
        arguments: List<PsiExpression>,
        block: Block,
        min: Int = 0,
        max: Int = Int.MAX_VALUE,
        factory: (List<MhType?>) -> MhType
    ): MhType {
        if (arguments.size < min) return noMatch()
        if (arguments.size > max) return noMatch()
        return factory(arguments.map { it.mhType(block) })
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
        val mhType = resolveMhType(this, block)
        typeData[this] = mhType ?: return null
        return mhType
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
