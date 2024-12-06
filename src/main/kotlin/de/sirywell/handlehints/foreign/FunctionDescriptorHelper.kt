package de.sirywell.handlehints.foreign

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.findPsiType
import de.sirywell.handlehints.getConstantOfType
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class FunctionDescriptorHelper(private val ssaAnalyzer: SsaAnalyzer) : ProblemEmitter(ssaAnalyzer.typeData) {

    fun of(
        resLayoutExpr: PsiExpression,
        argLayoutExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): FunctionDescriptorType {
        val resLayout = ssaAnalyzer.memoryLayoutType(resLayoutExpr, block) ?: TopMemoryLayoutType
        val argLayouts = argLayoutExprs.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        return CompleteFunctionDescriptorType(CompleteMemoryLayoutList(argLayouts), resLayout)
    }

    fun ofVoid(argLayoutExprs: List<PsiExpression>, block: SsaConstruction.Block): FunctionDescriptorType {
        val resLayout = VOID_RETURN_TYPE
        val argLayouts = argLayoutExprs.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        return CompleteFunctionDescriptorType(CompleteMemoryLayoutList(argLayouts), resLayout)
    }

    fun dropReturnLayout(qualifier: PsiExpression, block: SsaConstruction.Block): FunctionDescriptorType {
        val q = ssaAnalyzer.functionDescriptorType(qualifier, block) ?: TopFunctionDescriptorType
        return q.withReturnType(VOID_RETURN_TYPE)
    }

    fun appendArgumentLayouts(
        qualifier: PsiExpression,
        argumentExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): FunctionDescriptorType {
        val q = ssaAnalyzer.functionDescriptorType(qualifier, block) ?: TopFunctionDescriptorType
        val argLayouts = argumentExprs.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        val currentParams = q.parameterTypes as? CompleteMemoryLayoutList ?: return TopFunctionDescriptorType
        val newParams = currentParams.addAllAt(currentParams.size, CompleteMemoryLayoutList(argLayouts))
        return CompleteFunctionDescriptorType(newParams, q.returnType)
    }

    fun changeReturnLayout(
        qualifier: PsiExpression,
        newReturnExpr: PsiExpression,
        block: SsaConstruction.Block
    ): FunctionDescriptorType {
        val q = ssaAnalyzer.functionDescriptorType(qualifier, block) ?: TopFunctionDescriptorType
        val resLayout = ssaAnalyzer.memoryLayoutType(newReturnExpr, block) ?: TopMemoryLayoutType
        return q.withReturnType(resLayout)
    }

    fun insertArgumentLayouts(
        qualifier: PsiExpression,
        indexExpr: PsiExpression,
        addedLayoutExprs: List<PsiExpression>,
        block: SsaConstruction.Block
    ): FunctionDescriptorType {
        val q = ssaAnalyzer.functionDescriptorType(qualifier, block) ?: TopFunctionDescriptorType
        val index = indexExpr.getConstantOfType<Int>() ?: return TopFunctionDescriptorType
        val argLayouts = addedLayoutExprs.map { ssaAnalyzer.memoryLayoutType(it, block) ?: TopMemoryLayoutType }
        val currentParams = q.parameterTypes
        val newParams = currentParams.addAllAt(index, CompleteMemoryLayoutList(argLayouts))
        return CompleteFunctionDescriptorType(newParams, q.returnType)
    }

    fun toMethodType(qualifier: PsiExpression, block: SsaConstruction.Block): MethodHandleType {
        val q = ssaAnalyzer.functionDescriptorType(qualifier, block) ?: TopFunctionDescriptorType
        val memorySegmentType = ExactType(
            findPsiType("java.lang.foreign.MemorySegment", qualifier)
        )
        return q.toMethodType(memorySegmentType)
    }
}