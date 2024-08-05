package de.sirywell.handlehints.dfa

import com.intellij.openapi.util.RecursionManager.doPreventingRecursion
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import de.sirywell.handlehints.type.TypeLatticeElement
import kotlin.reflect.KClass
import kotlin.reflect.cast

class TypeElementResolver<T : TypeLatticeElement<T>>(
    private val ssaAnalyzer: SsaAnalyzer,
    private val block: SsaConstruction.Block,
    private val bot: T,
    private val clazz: KClass<T>
) : JavaRecursiveElementVisitor() {

    var result: T? = null
        private set

    override fun visitSwitchExpression(expression: PsiSwitchExpression) {
        val rules = (expression.body ?: return).childrenOfType<PsiSwitchLabeledRuleStatement>()
        var r = bot
        for (rule in rules) {
            rule.body?.accept(this) ?: continue
            r = r.join(result ?: continue)
        }
        result = r
    }

    override fun visitExpressionStatement(statement: PsiExpressionStatement) {
        statement.expression.accept(this)
    }

    override fun visitExpression(expression: PsiExpression) {
        result = calculateType(expression)
    }

    override fun visitConditionalExpression(expression: PsiConditionalExpression) {
        val then = calculateType(expression.thenExpression ?: return) ?: bot
        val elsa = calculateType(expression.elseExpression ?: return) ?: bot
        result = then.join(elsa)
    }

    override fun visitParenthesizedExpression(expression: PsiParenthesizedExpression) {
        result = calculateType(expression.expression ?: return)
    }

    private fun calculateType(expression: PsiExpression): T? {
        val t: TypeLatticeElement<*> = (ssaAnalyzer.typeData[expression]
        // SsaAnalyzer might call us again. Prevent SOE
            ?: doPreventingRecursion(expression, true) { ssaAnalyzer.resolveType(expression, block) })
            ?: return null
        if (clazz.isInstance(t)) {
            return clazz.cast(t)
        }
        return null
    }

}