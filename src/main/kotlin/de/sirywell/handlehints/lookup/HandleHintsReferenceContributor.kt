package de.sirywell.handlehints.lookup

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.ExpressionLookupItem
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PsiJavaElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.PsiMethodPattern
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons
import de.sirywell.handlehints.TypeData
import de.sirywell.handlehints.foreign.PathTraverser
import de.sirywell.handlehints.type.*
import javax.swing.Icon
import kotlin.math.min
import kotlin.reflect.KClass

class HandleHintsReferenceContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        fillInPathElements(parameters, result)
    }

    private fun fillInPathElements(
        parameters: CompletionParameters,
        result: CompletionResultSet
    ) {
        val pos = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(pos)) {
            return
        }
        if (!PATH_METHODS_PATTERN.accepts(pos)) {
            return
        }
        val directCall = pos.parentOfType<PsiMethodCallExpression>() ?: return
        val index = directCall.argumentList.expressions.indexOfFirst { it.text == pos.text }
        if (index < 0) return
        val qualifier = directCall.methodExpression.qualifierExpression ?: return
        val qualifierMemoryLayoutType = TypeData.forFile(pos.containingFile)<MemoryLayoutType>(qualifier) ?: return
        val targeted = followPath(qualifierMemoryLayoutType, directCall.argumentList.expressions, index) ?: return
        val factory = JavaPsiFacade.getElementFactory(qualifier.project)
        val icon = IconManager.getInstance().getPlatformIcon(PlatformIcons.Method)
        if (targeted is GroupLayoutType) {
            targeted.memberLayouts.partialList().map { it.name }
                .filterIsInstance<ExactLayoutName>()
                .mapNotNull { it.name }
                .forEach {
                    val method = "groupElement(\"$it\")"
                    val short = "PathElement.$method"
                    val med = "MemoryLayout.$short"
                    val expressionText = "java.lang.foreign.$med"
                    val expression = factory.createExpressionFromText(expressionText, qualifier)
                    val lookupElement = lookupExpression(expression, icon, short, false, short, method, short, med, it)
                    result.consume(lookupElement)
                }
        } else if (targeted is SequenceLayoutType) {
            val method = "sequenceElement()"
            val short = "PathElement.$method"
            val med = "MemoryLayout.$short"
            val expressionText = "java.lang.foreign.$med"
            val expression = factory.createExpressionFromText(expressionText, qualifier)
            val lookupElement = lookupExpression(expression, icon, short, true, short, method, short, med)
            result.consume(lookupElement)
        } else if (targeted is AddressLayoutType && targeted.targetLayout != null) {
            val method = "dereferenceElement()"
            val short = "PathElement.$method"
            val med = "MemoryLayout.$short"
            val expressionText = "java.lang.foreign.$med"
            val expression = factory.createExpressionFromText(expressionText, qualifier)
            val lookupElement = lookupExpression(expression, icon, short, false, short, method, short, med)
            result.consume(lookupElement)
        }
    }

    private fun toPath(
        arguments: List<PsiExpression>
    ): List<PathElementType> {
        return arguments
            .map { TypeData.forFile(it.containingFile)<PathElementType>(it) ?: TopPathElementType }
    }

    // adapted from JavaMethodHandleCompletionContributor
    private fun lookupExpression(
        expression: PsiExpression,
        icon: Icon?,
        presentableText: String,
        moveIntoParens: Boolean,
        vararg lookupText: String
    ): LookupElement {
        val element: LookupElement = object : ExpressionLookupItem(expression, icon, presentableText, *lookupText) {
            override fun handleInsert(context: InsertionContext) {
                context.document.deleteString(context.startOffset, context.tailOffset)
                context.commitDocument()
                replaceText(context, getObject().text)
                // move caret behind inserted text, user most likely wants to continue there
                val end = context.tailOffset
                context.editor.caretModel.currentCaret.moveToOffset(if (moveIntoParens) end - 1 else end)
            }
        }
        return PrioritizedLookupElement.withPriority(element, 1.0)
    }

    // adapted from JavaMethodHandleCompletionContributor
    fun replaceText(context: InsertionContext, text: String) {
        val newElement = PsiUtilCore.getElementAtOffset(context.file, context.startOffset)
        val params = newElement.parent.parent
        val end = params.textRange.endOffset - 1
        val start = min(newElement.textRange.endOffset.toDouble(), end.toDouble()).toInt()

        context.document.replaceString(start, end, text)
        context.commitDocument()
        JavaCodeStyleManager.getInstance(context.project)
            .shortenClassReferences(context.file, context.startOffset, context.tailOffset)
    }

    private fun followPath(
        qualifierMemoryLayoutType: MemoryLayoutType,
        arguments: Array<PsiExpression>,
        untilIndex: Int
    ): MemoryLayoutType? {
        val preceding = arguments.toList().subList(0, untilIndex)
        val traverser = ReturningPathTraverser()
        return traverser.traverse(toPath(preceding), qualifierMemoryLayoutType, mutableListOf())
    }
}

private val PATH_METHODS_PATTERN: PsiJavaElementPattern.Capture<PsiElement> = PsiJavaPatterns.psiElement()
    .afterLeaf("(", ",")
    .withParent(
        PsiJavaPatterns.psiExpression().methodCallParameter(
            methodPattern(
                "arrayElementVarHandle",
                "byteOffset",
                "byteOffsetHandle",
                "select",
                "sliceHandle",
                "varHandle"
            )
        )
    )

// adapted from JavaMethodHandleCompletionContributor
private fun methodPattern(vararg methodNames: String): PsiMethodPattern {
    return PsiJavaPatterns.psiMethod().withName(*methodNames)
        .definedInClass("java.lang.foreign.MemoryLayout")
}

private class ReturningPathTraverser : PathTraverser<MemoryLayoutType?> {
    override fun invalidAddressDereference(head: IndexedValue<DereferenceElementType>) = TopMemoryLayoutType

    override fun pathElementAndLayoutTypeMismatch(
        head: IndexedValue<PathElementType>,
        memoryLayoutType: KClass<out MemoryLayoutType>,
        pathElementType: KClass<out PathElementType>
    ) = TopMemoryLayoutType

    override fun onGroupElementNameNotFound(
        elementType: IndexedValue<GroupElementType>,
        name: String
    ) = TopMemoryLayoutType

    override fun onGroupElementIndexOutOfBounds(
        elementType: IndexedValue<GroupElementType>,
        index: Long,
        memberLayouts: TypeLatticeElementList<MemoryLayoutType>
    ) = TopMemoryLayoutType

    override fun onSequenceElementIndexOutOfBounds(
        layoutType: SequenceLayoutType,
        index: Long,
        head: IndexedValue<SequenceElementType>
    ) = TopMemoryLayoutType

    override fun onComplete(layoutType: ValueLayoutType, coords: MutableList<Type>) = null

    override fun onPathEmpty(layoutType: MemoryLayoutType, coords: MutableList<Type>) = layoutType

    override fun onTopPathElement(
        path: List<IndexedValue<PathElementType>>,
        coords: MutableList<Type>,
        layoutType: MemoryLayoutType
    ) = null

    override fun onBottomPathElement(
        path: List<IndexedValue<PathElementType>>,
        coords: MutableList<Type>,
        layoutType: MemoryLayoutType
    ) = null

    override fun onTopLayout(path: List<IndexedValue<PathElementType>>, coords: MutableList<Type>) = null

    override fun onBottomLayout(
        path: List<IndexedValue<PathElementType>>,
        coords: MutableList<Type>
    ) = null

}