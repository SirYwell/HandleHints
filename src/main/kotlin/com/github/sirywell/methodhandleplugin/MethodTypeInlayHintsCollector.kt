package com.github.sirywell.methodhandleplugin

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.refactoring.suggested.endOffset
import kotlinx.collections.immutable.toImmutableList

@Suppress("UnstableApiUsage")
class MethodTypeInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    private val methodTypeKey = Key<MethodType>(MethodTypeInlayHintsCollector::class.qualifiedName!!)

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (element is PsiMethodCallExpression && isJavaLangInvoke(element)) {
            val methodType = findMethodType(element)
            element.putUserData(methodTypeKey, methodType)
            println(methodType)
            sink.addInlineElement(element.endOffset, true, createInlayPresentation(methodType.toString()), true)
        }
        return true
    }

    data class MethodType(
        val returnType: PsiType,
        val parameters: List<PsiType>
    ) {
        override fun toString(): String {
            return "(${parameters.joinToString(",") { it.presentableText }})${returnType.presentableText}"
        }
    }

    private fun findMethodType(element: PsiMethodCallExpression): MethodType {
        if (receiverIsMethodType(element) && element.methodName == "methodType") {
            val returnType = (element.argumentList.expressions[0] as PsiClassObjectAccessExpression).operand.type
            val parameters = mutableListOf<PsiType>()
            for (expr in element.argumentList.expressions.drop(1)) {
                parameters.add((expr as PsiClassObjectAccessExpression).operand.type)
            }
            return MethodType(returnType, parameters.toImmutableList())
        } else if (receiverIsMethodHandles(element) && element.methodName == "zero") {
            return MethodType(
                (element.argumentList.expressions[0] as PsiClassObjectAccessExpression).operand.type,
                listOf()
            )
        } else if (receiverIsMethodHandle(element) && element.methodName in setOf("invoke", "invokeExact")) {
            return element.getUserData(methodTypeKey) ?: findMethodType(element.methodExpression.qualifier!!)
        } else if (receiverIsMethodHandles(element) && element.methodName == "empty") {
            return element.getUserData(methodTypeKey) ?: findMethodType(element.argumentList.expressions[0])
        }
        return unknown
    }

    private fun findMethodType(element: PsiElement): MethodType {
        val methodType = element.getUserData(methodTypeKey)
        if (methodType != null) return methodType
        return when (element) {
            is PsiReference -> element.resolve()?.let(::findMethodType)
            is PsiVariable -> element.initializer?.let(::findMethodType)
            is PsiMethodCallExpression -> findMethodType(element)
            else -> null
        } ?: unknown
    }

    private val unknown = MethodType(PsiType.NULL, listOf())

    private val PsiMethodCallExpression.methodName
        get() = this.methodExpression.referenceName

    private fun isJavaLangInvoke(element: PsiMethodCallExpression) =
        (element.resolveMethod()?.containingClass?.containingFile as? PsiJavaFile)?.packageName == "java.lang.invoke"

    private fun receiverIsInvokeClass(element: PsiMethodCallExpression, className: String) =
        element.resolveMethod()?.containingClass?.qualifiedName == "java.lang.invoke.$className"

    private fun receiverIsMethodHandle(element: PsiMethodCallExpression) =
        receiverIsInvokeClass(element, "MethodHandle")

    private fun receiverIsMethodHandles(element: PsiMethodCallExpression) =
        receiverIsInvokeClass(element, "MethodHandles")

    private fun receiverIsMethodType(element: PsiMethodCallExpression) = receiverIsInvokeClass(element, "MethodType")

    private fun createInlayPresentation(text: String): InlayPresentation {
        return InsetPresentation(factory.roundWithBackground(factory.text(text)))
    }


}
