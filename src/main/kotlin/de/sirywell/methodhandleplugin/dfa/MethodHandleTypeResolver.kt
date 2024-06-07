package de.sirywell.methodhandleplugin.dfa

import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import de.sirywell.methodhandleplugin.type.BotSignature
import de.sirywell.methodhandleplugin.type.MethodHandleType

class MethodHandleTypeResolver(private val ssaAnalyzer: SsaAnalyzer, private val block: SsaConstruction.Block) :
    JavaRecursiveElementVisitor() {

    var result: MethodHandleType? = null
        private set

    override fun visitSwitchExpression(expression: PsiSwitchExpression) {
        val rules = (expression.body ?: return).childrenOfType<PsiSwitchLabeledRuleStatement>()
        var r = MethodHandleType(BotSignature)
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
        val then = calculateType(expression.thenExpression ?: return)
            ?: MethodHandleType(BotSignature)
        val elsa = calculateType(expression.elseExpression ?: return)
            ?: MethodHandleType(BotSignature)
        result = then.join(elsa)
    }

    override fun visitParenthesizedExpression(expression: PsiParenthesizedExpression) {
        result = calculateType(expression.expression ?: return)
    }

    private fun calculateType(expression: PsiExpression): MethodHandleType? {
        return ssaAnalyzer.typeData[expression] ?: ssaAnalyzer.resolveMhType(expression, block)
    }

}