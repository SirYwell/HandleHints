@file:Suppress("UnstableApiUsage")

package de.sirywell.methodhandleplugin.dfa

import com.google.common.graph.ElementOrder
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.google.common.graph.Traverser
import com.intellij.psi.PsiVariable
import com.intellij.psi.controlFlow.*
import java.util.*

// This implements SSA Construction as described in Chapter 2 in
// https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf
// (but for now, without trivial phi removal)
class SsaConstruction<T>(private val controlFlow: ControlFlow) {

    companion object {
        private val ENTRY_BLOCK = Block(listOf())
    }
    private val currentDefs = mutableMapOf<PsiVariable, MutableMap<Block, Value<T>>>()
    private val sealedBlocks = mutableSetOf<Block>()
    private val incompletePhis = mutableMapOf<Block, MutableMap<PsiVariable, Phi<T>>>()
    private val blocks = buildBlocks()

    private fun buildBlocks(): ImmutableGraph<Block> {
        val leaderSet = mutableSetOf(0)
        val leaderVisitor = LeaderVisitor(leaderSet)
        controlFlow.instructions.forEachIndexed { index, instruction -> instruction.accept(leaderVisitor, index, -1) }
        var currentBlockInstructions = mutableListOf<Int>()
        val blocks = mutableListOf<Block>()
        val instructionToBlock = mutableListOf<Block>()

        for (index in (0 until controlFlow.size)) {
            if (index in leaderSet && currentBlockInstructions.isNotEmpty()) {
                blocks.add(Block(currentBlockInstructions.toList()))
                currentBlockInstructions = mutableListOf(index)
            } else {
                currentBlockInstructions.add(index)
            }
        }

        if (currentBlockInstructions.isNotEmpty()) {
            blocks.add(Block(currentBlockInstructions.toList()))
        }

        for (block in blocks) {
            sealBlock(block)
            instructionToBlock.addAll(Collections.nCopies(block.instructions.size, block))
        }

        return buildGraph(blocks, instructionToBlock)
    }

    private fun buildGraph(
        blocks: MutableList<Block>,
        instructionToBlock: MutableList<Block>
    ): ImmutableGraph<Block> {
        val builder = GraphBuilder.directed()
            .nodeOrder<Block>(ElementOrder.insertion())
            .immutable<Block>()

        // special case: a single block does not have any edges,
        // but it should be still in the graph
        if (blocks.size == 1) {
            builder.addNode(blocks.first())
            return builder.build()
        }

        // ensure there is a block with inDegree == 0
        builder.putEdge(ENTRY_BLOCK, instructionToBlock[0])

        for (edge in ControlFlowUtil.getEdges(controlFlow, 0)) {
            if (edge.myTo >= controlFlow.size) {
                // when editing code, we seemingly can have a somewhat inconsistent state
                // val instruction = controlFlow.instructions[edge.myFrom]
                // assert(instruction is BranchingInstruction && instruction.role == Role.END) { "$instruction"}
                continue
            }
            val fromBlock = instructionToBlock[edge.myFrom]
            val toBlock = instructionToBlock[edge.myTo]
            if (fromBlock != toBlock) {
                builder.putEdge(fromBlock, toBlock)
            }
        }
        return builder.build()
    }

    fun writeVariable(variable: PsiVariable, block: Block, value: Value<T>) {
        currentDefs.computeIfAbsent(variable) { mutableMapOf() }[block] = value
    }

    fun readVariable(variable: PsiVariable, block: Block): Value<T>? {
        if (variable !in currentDefs) return null // unrelated variable
        if (block in currentDefs[variable]!!) {
            return currentDefs[variable]!![block]!!
        }
        return readVariableRecursive(variable, block)
    }

    private fun readVariableRecursive(variable: PsiVariable, block: Block): Value<T>? {
        if (variable !in currentDefs) return null
        val value: Value<T>
        if (block !in sealedBlocks) {
            value = Phi(block)
            incompletePhis.computeIfAbsent(block) { mutableMapOf() }[variable] = value
        } else if (blocks.inDegree(block) == 1) {
            value = readVariable(variable, blocks.predecessors(block).first())!!
        } else {
            val phi = Phi<T>(block)
            writeVariable(variable, block, phi)
            value = addPhiOperands(variable, phi)
        }
        writeVariable(variable, block, value)
        return value
    }

    private fun addPhiOperands(variable: PsiVariable, phi: Phi<T>): Value<T> {
        for (pred in blocks.predecessors(phi.block)) {
            phi.appendOperand(readVariable(variable, pred)!!, pred)
        }
        return tryRemoveTrivialPhi(phi)
    }

    private fun tryRemoveTrivialPhi(phi: Phi<T>): Value<T> {
        // very trivial phis only (none)
        return phi
    }

    private fun sealBlock(block: Block) {
        for ((variable, phi) in incompletePhis[block].orEmpty()) {
            addPhiOperands(variable, phi)
        }
        sealedBlocks.add(block)
    }

    fun traverse(
        onRead: (ReadVariableInstruction, Int, Block) -> Unit,
        onWrite: (WriteVariableInstruction, Int, Block) -> Unit
    ) {
        val list = Traverser.forGraph(blocks)
            .depthFirstPostOrder(blocks.nodes().find { blocks.inDegree(it) == 0 }!!)
            .reversed()
            .toList()
        val instructions = controlFlow.instructions
        for (block in list) {
            for (instrIndex in block.instructions) {
                when (val instruction = instructions[instrIndex]) {
                    is ReadVariableInstruction -> onRead(instruction, instrIndex, block)
                    is WriteVariableInstruction -> onWrite(instruction, instrIndex, block)
                }
            }
        }
    }

    data class Block(val instructions: List<Int>)

    class LeaderVisitor(private val leaders: MutableSet<Int>) : ControlFlowInstructionVisitor() {

        override fun visitBranchingInstruction(instruction: BranchingInstruction, offset: Int, nextOffset: Int) {
            for (i in (0 until instruction.nNext())) {
                leaders.add(instruction.getNext(offset, i))
            }
        }
    }

    sealed interface Value<T>
    data class Phi<T>(
        val block: Block,
        val blockToValue: MutableMap<Block, Value<T>> = mutableMapOf()
    ) : Value<T> {
        fun appendOperand(value: Value<T>, block: Block) {
            blockToValue[block] = value
        }
    }

    data class Holder<T>(val value: T) : Value<T>
}
