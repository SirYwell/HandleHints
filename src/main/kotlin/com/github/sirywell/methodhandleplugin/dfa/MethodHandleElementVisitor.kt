package com.github.sirywell.methodhandleplugin.dfa

import com.github.sirywell.methodhandleplugin.*
import com.github.sirywell.methodhandleplugin.MethodHandleBundle.message
import com.github.sirywell.methodhandleplugin.MethodTypeInlayHintsCollector.Companion.methodTypeKey
import com.github.sirywell.methodhandleplugin.mhtype.*
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.psi.*
import com.intellij.psi.controlFlow.*

class MethodHandleElementVisitor(private val problemsHolder: ProblemsHolder) : JavaElementVisitor() {
    override fun visitMethod(method: PsiMethod?) {
        if (method == null || method.body == null) {
            return
        }
        val controlFlowFactory = ControlFlowFactory.getInstance(method.project)
        val controlFlow: ControlFlow
        try {
            controlFlow = controlFlowFactory.getControlFlow(method.body!!, AllVariablesControlFlowPolicy.getInstance())
        } catch (_: AnalysisCanceledException) {
            return // stop
        }
        val visitor = ControlFlowVisitor(controlFlow)
        val instructions = controlFlow.instructions
        val edges = ControlFlowUtil.getEdges(controlFlow, 0)
        for (edge in edges.reversed()) {
            instructions[edge.myFrom].accept(visitor, edge.myFrom, edge.myTo)
        }
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        if (!receiverIsMethodHandle(expression)) return
        val target = expression.methodExpression.qualifierExpression ?: return
        val type = target.getUserData(methodTypeKey) ?: return
        if (type !is MhSingleType) return // ignore for now
        val (returnType, parameters) = type.signature
        when (expression.methodName) {
            "invoke" -> {
                checkArgumentsCount(parameters, expression)
            }
            "invokeExact" -> {
                checkArgumentsTypes(parameters, expression)
                checkArgumentsCount(parameters, expression)
                checkReturnType(returnType, expression)
            }
        }
    }

    private fun checkArgumentsTypes(parameters: List<PsiType>, expression: PsiMethodCallExpression) {
        if (expression.argumentList.expressionTypes.zip(parameters)
            .any { it.first != it.second }) {
            problemsHolder.registerProblem(
                expression.methodExpression as PsiExpression,
                message("problem.arguments.wrong.types",
                    parameters.map { it.presentableText },
                    expression.argumentList.expressionTypes.map { it.presentableText }
                ),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    private fun checkReturnType(returnType: PsiType, expression: PsiMethodCallExpression) {
        val parent = expression.parent
        if (parent !is PsiTypeCastExpression) {
            if (returnType != PsiType.getJavaLangObject(expression.manager, expression.resolveScope)) {
                problemsHolder.registerProblem(
                    expression.methodExpression as PsiExpression,
                    message("problem.returnType.not.object", returnType.presentableText),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        } else {
            val type = parent.castType?.type ?: return
            if (type != returnType) {
                problemsHolder.registerProblem(
                    parent.castType!!,
                    message("problem.returnType.wrong.cast", returnType.presentableText, type.presentableText),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }

    private fun checkArgumentsCount(
        parameters: List<PsiType>,
        expression: PsiMethodCallExpression
    ) {
        if (parameters.size != expression.argumentList.expressionCount) {
            problemsHolder.registerProblem(
                expression.methodExpression as PsiExpression,
                message("problem.arguments.count", parameters.size, expression.argumentList.expressionCount),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    class ControlFlowVisitor(private val controlFlow: ControlFlow) : ControlFlowInstructionVisitor() {
        private val map = mutableMapOf<PsiElement, MhType>()
        override fun visitReadVariableInstruction(instruction: ReadVariableInstruction, offset: Int, nextOffset: Int) {
            val element = controlFlow.getElement(offset)
            val mhType = map[instruction.variable]
            element.putUserData(methodTypeKey, mhType)
        }

        override fun visitWriteVariableInstruction(
            instruction: WriteVariableInstruction,
            offset: Int,
            nextOffset: Int
        ) {
            val element = controlFlow.getElement(offset)
            val expression = when (element) {
                is PsiAssignmentExpression -> element.rExpression!!
                is PsiDeclarationStatement -> instruction.variable.initializer!!
                else -> TODO("Not supported: ${element.javaClass}")
            }
            val mhType = map[expression] ?: resolveMhType(expression)?.also { map[expression] = it } ?: return
            map[element] = mhType
            map[instruction.variable] = mhType
            if (mhType is MhSingleType) {
                element.putUserData(methodTypeKey, mhType)
            }
        }

        private fun resolveMhType(expression: PsiExpression): MhType? {
            if (expression is PsiMethodCallExpression) {
                val arguments = expression.argumentList.expressions.asList()
                if (receiverIsMethodType(expression)) {
                    return when (expression.methodName) {
                        "methodType" -> MethodTypeHelper.methodType(arguments.toPsiTypes() ?: return notConstant())
                        else -> TODO("unsupported method MethodType${expression.methodName}")
                    }
                } else if (receiverIsMethodHandles(expression)) {
                    return methodHandles(expression, arguments)
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
                            MethodHandleTransformer.bindTo(target.mhType(), objectType)
                        }
                        "withVarargs" -> TODO()
                        "invoke", "invokeExact", "invokeWithArguments" -> noMethodHandle()
                        else -> TODO("unsupported method MethodHandle#${expression.methodName}")
                    }
                }
            } else if (expression is PsiReferenceExpression) {
                return map[expression.resolve()] ?: Top
            }
            return Top // TODO
        }

        private fun noMethodHandle(): MhType? = null

        private fun methodHandles(
            expression: PsiMethodCallExpression,
            arguments: List<PsiExpression>
        ): MhType {
            return when (expression.methodName) {
                "arrayConstructor" -> singleParameter(arguments, MethodHandlesInitializer::arrayConstructor)
                "arrayElementGetter" -> singleParameter(arguments, MethodHandlesInitializer::arrayElementGetter)
                "arrayElementSetter" -> singleParameter(arguments, MethodHandlesInitializer::arrayElementSetter)
                "arrayLength" -> singleParameter(arguments, MethodHandlesInitializer::arrayLength)
                "catchException" -> {
                    if (arguments.size != 3) return noMatch()
                    val exType = arguments[1].getConstantOfType<PsiType>() ?: return notConstant()
                    MethodHandlesMerger.catchException(arguments[0].mhType(), exType, arguments[2].mhType())
                }

                "collectArguments" -> {
                    if (arguments.size != 3) return noMatch()
                    val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                    return MethodHandlesMerger.collectArguments(arguments[0].mhType(), pos, arguments[2].mhType())
                }

                "constant" -> {
                    if (arguments.size != 2) return noMatch()
                    val type = arguments[0].getConstantOfType<PsiType>() ?: return notConstant()
                    // TODO warn if value is incompatible with type?
                    MethodHandlesInitializer.constant(type)
                }

                "countedLoop" -> justDelegate(arguments, min = 3, max = 4) {
                    if (arguments.size == 3) {
                        MethodHandlesMerger.countedLoop(it[0], it[1], it[2])
                    } else {
                        MethodHandlesMerger.countedLoop(it[0], it[1], it[2], it[3])
                    }
                }

                "doWhileLoop" -> justDelegate(arguments, exact = 3) {
                    // TODO init can be null
                    MethodHandlesMerger.doWhileLoop(it[0], it[1], it[2])
                }

                "dropArguments" -> {
                    if (arguments.size < 3) return noMatch()
                    val i = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                    val types = arguments.subList(2).toPsiTypes() ?: return notConstant()
                    MethodHandlesMerger.dropArguments(arguments[0].mhType(), i, types)
                }

                "dropArgumentsToMatch" -> notConstant() // likely too difficult to get something precise here
                "dropReturn" -> justDelegate(arguments, exact = 1) {
                    MethodHandlesMerger.dropReturn(it[0])
                }

                "empty" -> justDelegate(arguments, exact = 1) {
                    MethodHandlesInitializer.empty(it[0])
                }

                "exactInvoker" -> justDelegate(arguments, exact = 1) {
                    MethodHandlesInitializer.exactInvoker(it[0], methodHandleType(expression))
                }

                "explicitCastArguments" -> justDelegate(arguments, exact = 2) {
                    MethodHandlesMerger.explicitCastArguments(it[0], it[1])
                }

                "filterArguments" -> {
                    if (arguments.size < 2) return noMatch()
                    val target = arguments[0].mhType()
                    val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                    val filter = arguments.drop(2).map { it.mhType() }
                    MethodHandlesMerger.filterArguments(target, pos, filter)
                }

                "filterReturnValue" -> justDelegate(arguments, exact = 2) {
                    MethodHandlesMerger.filterReturnValue(it[0], it[1])
                }

                "foldArguments" -> {
                    val target: MhType
                    val combiner: MhType
                    val pos: Int
                    when (arguments.size) {
                        3 -> {
                            target = arguments[0].mhType()
                            pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                            combiner = arguments[2].mhType()
                        }

                        2 -> {
                            target = arguments[0].mhType()
                            pos = 0
                            combiner = arguments[1].mhType()
                        }

                        else -> return noMatch()
                    }
                    MethodHandlesMerger.foldArguments(target, pos, combiner)
                }

                "guardWithTest" -> justDelegate(arguments, exact = 3) {
                    MethodHandlesMerger.guardWithTest(it[0], it[1], it[2])
                }

                "identity" -> singleParameter(arguments, MethodHandlesInitializer::identity)
                "insertArguments" -> {
                    if (arguments.size < 2) return noMatch()
                    val target = arguments[0].mhType()
                    val pos = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                    MethodHandlesMerger.insertArguments(
                        target,
                        pos,
                        arguments.subList(2).map { it.type ?: return notConstant() })
                }

                "invoker" -> justDelegate(arguments, exact = 1) {
                    MethodHandlesInitializer.invoker(it[0], methodHandleType(expression))
                }

                "iteratedLoop" -> TODO("not implemented")
                "loop" -> notConstant()
                "permuteArguments" -> {
                    if (arguments.size < 2) noMatch()
                    else {
                        val reorder = arguments.subList(2)
                            .map { it.getConstantOfType<Int>() ?: return notConstant() }
                        MethodHandlesMerger.permuteArguments(arguments[0].mhType(), arguments[1].mhType(), reorder)
                    }
                }

                "spreadInvoker" -> {
                    if (arguments.size != 2) return noMatch()
                    val leadingArgCount = arguments[1].getConstantOfType<Int>() ?: return notConstant()
                    MethodHandlesInitializer.spreadInvoker(
                        arguments[0].mhType(),
                        leadingArgCount,
                        objectType(expression)
                    )
                }

                "tableSwitch" -> justDelegate(arguments, min = 1, factory = {
                    MethodHandlesMerger.tableSwitch(it[0], it.drop(1))
                })

                "throwException" -> {
                    if (arguments.size != 2) return noMatch()
                    val types = arguments.toPsiTypes() ?: return notConstant()
                    MethodHandlesInitializer.throwException(types[0], types[1])
                }

                "tryFinally" -> justDelegate(arguments, exact = 2) {
                    MethodHandlesMerger.tryFinally(it[0], it[1])
                }

                "zero" -> singleParameter(arguments, MethodHandlesInitializer::zero)
                else -> TODO("unsupported method MethodHandles#${expression.methodName}")
            }
        }

        private fun noMatch(): MhType {
            return Bot
        }

        private fun notConstant(): MhType {
            return Bot
        }

        override fun visitInstruction(instruction: Instruction?, offset: Int, nextOffset: Int) {
            super.visitInstruction(instruction, offset, nextOffset)
        }

        @Suppress("UnstableApiUsage")
        private inline fun <reified T> PsiExpression.getConstantOfType(): T? {
            return CommonDataflow.getDataflowResult(this)
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
            min: Int = 0,
            max: Int = Int.MAX_VALUE,
            factory: (List<MhType>) -> MhType
        ): MhType {
            if (arguments.size < min) return noMatch()
            if (arguments.size > max) return noMatch()
            return factory(arguments.map { it.mhType() })
        }

        private fun justDelegate(
            arguments: List<PsiExpression>,
            exact: Int,
            factory: (List<MhType>) -> MhType
        ): MhType {
            return justDelegate(arguments, min = exact, max = exact, factory)
        }

        private fun PsiExpression.mhType(): MhType {
            return resolveMhType(this)!! // TODO
        }

        private fun methodHandleType(element: PsiElement): PsiClassType {
            return PsiType.getTypeByName("java.lang.invoke.MethodHandle", element.project, element.resolveScope)
        }


        private fun objectType(element: PsiElement): PsiType {
            return PsiType.getJavaLangObject(element.manager, element.resolveScope)
        }
    }

}
