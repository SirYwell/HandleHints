package de.sirywell.handlehints.foreign

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType
import de.sirywell.handlehints.TriState
import de.sirywell.handlehints.dfa.SsaAnalyzer
import de.sirywell.handlehints.dfa.SsaConstruction
import de.sirywell.handlehints.inspection.ProblemEmitter
import de.sirywell.handlehints.type.*

class LinkerHelper(
    ssaAnalyzer: SsaAnalyzer,
    private val functionDescriptorHelper: FunctionDescriptorHelper
) : ProblemEmitter(ssaAnalyzer.typeData) {

    fun downcallHandle(
        functionDescriptor: PsiExpression,
        options: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        if (options.isNotEmpty()) return TopMethodHandleType // TODO
        // TODO inspections for options (i.e. duplicates, out-of-bounds indexes...)
        val original = functionDescriptorHelper.toMethodType(functionDescriptor, block)
        val leadingParams = buildLeadingParams(original, functionDescriptor)
        val paramList = original.parameterTypes.addAllAt(0, leadingParams)
        return original.withParameterTypes(paramList)
    }

    private fun buildLeadingParams(methodType: MethodHandleType, context: PsiExpression): TypeList {
        val memorySegmentType = ExactType(
            PsiType.getTypeByName(
                "java.lang.foreign.MemorySegment",
                context.project,
                context.resolveScope
            )
        )
        val (_, identical) = methodType.returnType.joinIdentical(memorySegmentType)
        return when (identical) {
            TriState.NO -> {
                // return type does not need allocation
                CompleteTypeList(
                    listOf(
                        memorySegmentType
                    )
                )
            }
            TriState.YES -> {
                // return type needs allocation
                CompleteTypeList(
                    listOf(
                        memorySegmentType,
                        ExactType(
                            PsiType.getTypeByName(
                                "java.lang.foreign.SegmentAllocator",
                                context.project,
                                context.resolveScope
                            )
                        )
                    )
                )
            }
            TriState.UNKNOWN -> {
                // we only know that MemorySegment is a leading param, everything else is uncertain
                IncompleteTypeList(mapOf(0 to memorySegmentType).toSortedMap())
            }
        }
    }

    fun downcallHandle(
        address: PsiExpression,
        functionDescriptor: PsiExpression,
        options: List<PsiExpression>,
        block: SsaConstruction.Block
    ): MethodHandleType {
        val original = downcallHandle(functionDescriptor, options, block)
        // perform an unchecked "bindTo"
        return original.withParameterTypes(original.parameterTypes.dropFirst(1))

    }
}