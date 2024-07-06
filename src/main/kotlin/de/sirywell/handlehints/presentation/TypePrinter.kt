package de.sirywell.handlehints.presentation

import de.sirywell.handlehints.type.*

class TypePrinter : TypeVisitor<TypePrinter.PrintContext, Unit> {

    data class PrintContext(private val builder: StringBuilder, private val memoryLayoutSeparator: String) :
        Appendable by builder,
        CharSequence by builder {

        fun append(value: Any): PrintContext {
            append(value.toString())
            return this
        }

        fun emitMemoryLayoutSeparator(): PrintContext {
            append(memoryLayoutSeparator)
            return this
        }
    }

    fun print(type: TypeLatticeElement<*>): String {
        val stringBuilder = StringBuilder()
        type.accept(this, PrintContext(stringBuilder, ""))
        return stringBuilder.toString()
    }

    override fun visit(type: BotType, context: PrintContext) {
        context.append("⊥")
    }

    override fun visit(type: TopType, context: PrintContext) {
        context.append("⊤")
    }

    override fun visit(type: ExactType, context: PrintContext) {
        context.append(type.psiType.presentableText)
    }

    override fun visit(type: TopTypeList, context: PrintContext) {
        context.append("({⊤})")
    }

    override fun visit(type: BotTypeList, context: PrintContext) {
        context.append("({⊥})")
    }

    override fun visit(type: CompleteTypeList, context: PrintContext) {
        context.append("(")
        type.typeList.forEachIndexed { index, t ->
            if (index != 0) {
                context.append(",")
            }
            t.accept(this, context)
        }
        context.append(")")
    }

    override fun visit(type: IncompleteTypeList, context: PrintContext) {
        context.append("(")
        type.knownTypes.onEachIndexed { index, (pos, t) ->
            if (index != 0) {
                context.append(",")
            }
            context.append("$pos").append("=")
            t.accept(this, context)
        }
        context.append(")")
    }

    override fun visit(type: BotVarHandleType, context: PrintContext) {
        context.append("⊥")
    }

    override fun visit(type: TopVarHandleType, context: PrintContext) {
        context.append("⊤")
    }

    override fun visit(type: CompleteVarHandleType, context: PrintContext) {
        type.coordinateTypes.accept(this, context)
        context.append("(")
        type.variableType.accept(this, context)
        context.append(")")
    }

    override fun visit(type: BotMethodHandleType, context: PrintContext) {
        context.append("⊥")
    }

    override fun visit(type: TopMethodHandleType, context: PrintContext) {
        context.append("⊤")
    }

    override fun visit(type: CompleteMethodHandleType, context: PrintContext) {
        type.parameterTypes.accept(this, context)
        type.returnType.accept(this, context)
    }

    override fun visit(type: BotMemoryLayoutType, context: PrintContext) {
        context.append("⊥")
    }

    override fun visit(type: TopMemoryLayoutType, context: PrintContext) {
        context.append("⊤")
    }

    override fun visit(type: ValueLayoutType, context: PrintContext) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.type.accept(this, context)
        context.append(type.byteSize ?: "?")
    }

    override fun visit(type: StructLayoutType, context: PrintContext) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.memberLayouts.accept(this, context)
    }

    override fun visit(type: UnionLayoutType, context: PrintContext) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.memberLayouts.accept(this, context.copy(memoryLayoutSeparator = "|"))
    }

    override fun visit(type: SequenceLayoutType, context: PrintContext) {
        context.append("[")
        context.append(type.elementCount ?: "?").append(":")
        type.elementLayout.accept(this, context)
        context.append("]")
    }

    override fun visit(type: PaddingLayoutType, context: PrintContext) {
        context.append("x")
        context.append(type.byteSize ?: "?")
    }

    override fun visit(type: TopMemoryLayoutList, context: PrintContext) {
        context.append("[{⊤}]")
    }

    override fun visit(type: BotMemoryLayoutList, context: PrintContext) {
        context.append("[{⊥}]")
    }

    override fun visit(type: CompleteMemoryLayoutList, context: PrintContext) {
        context.append("[")
        val cleanContext = context.copy(memoryLayoutSeparator = "")
        type.typeList.forEachIndexed { index, t ->
            if (index > 0) {
                context.emitMemoryLayoutSeparator()
            }
            t.accept(this, cleanContext)
        }
        context.append("]")
    }

    override fun visit(type: IncompleteMemoryLayoutList, context: PrintContext) {
        context.append("[")
        val cleanContext = context.copy(memoryLayoutSeparator = "")
        (0..type.knownTypes.lastKey()).map {
            if (it > 0) {
                context.emitMemoryLayoutSeparator()
            }
            type.parameterType(it).accept(this, cleanContext)
        }
        context.append("]")
    }

}