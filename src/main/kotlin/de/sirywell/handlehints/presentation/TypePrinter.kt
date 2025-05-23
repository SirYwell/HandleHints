package de.sirywell.handlehints.presentation

import de.sirywell.handlehints.type.*

class TypePrinter : TypeVisitor<TypePrinter.PrintContext, Unit> {

    enum class ListEnclosing(val open: String, val close: String) {
        PARENTHESES("(", ")"),
        BRACKETS("[", "]"),
    }

    data class PrintContext(
        private val builder: StringBuilder,
        private val memoryLayoutSeparator: String,
        val listEnclosing: ListEnclosing
    ) :
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
        type.accept(this, PrintContext(stringBuilder, "", ListEnclosing.BRACKETS))
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

    override fun visit(type: AddressLayoutType, context: PrintContext) {
        context.append("a?")
        if (type.targetLayout != null) {
            context.append(":")
            type.targetLayout.accept(this, context)
        }
    }

    override fun visit(type: NormalValueLayoutType, context: PrintContext) {
        if (type == VOID_RETURN_TYPE) return // not an actual value layout
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.type.accept(this, context)
        context.append(type.byteSize ?: "?")
        type.name.accept(this, context)
    }

    override fun visit(type: StructLayoutType, context: PrintContext) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.memberLayouts.accept(this, context)
        type.name.accept(this, context)
    }

    override fun visit(type: UnionLayoutType, context: PrintContext) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.memberLayouts.accept(this, context.copy(memoryLayoutSeparator = "|"))
        type.name.accept(this, context)
    }

    override fun visit(type: SequenceLayoutType, context: PrintContext) {
        context.append("[")
        context.append(type.elementCount ?: "?").append(":")
        type.elementLayout.accept(this, context)
        context.append("]")
        type.name.accept(this, context)
    }

    override fun visit(type: PaddingLayoutType, context: PrintContext) {
        context.append("x")
        context.append(type.byteSize ?: "?")
        type.name.accept(this, context)
    }

    override fun visit(type: TopMemoryLayoutList, context: PrintContext) {
        context.append(context.listEnclosing.open)
        context.append("{⊤}")
        context.append(context.listEnclosing.close)
    }

    override fun visit(type: BotMemoryLayoutList, context: PrintContext) {
        context.append(context.listEnclosing.open)
        context.append("{⊥}")
        context.append(context.listEnclosing.close)
    }

    override fun visit(type: CompleteMemoryLayoutList, context: PrintContext) {
        context.append(context.listEnclosing.open)
        val cleanContext = context.copy(memoryLayoutSeparator = "", listEnclosing = ListEnclosing.BRACKETS)
        type.typeList.forEachIndexed { index, t ->
            if (index > 0) {
                context.emitMemoryLayoutSeparator()
            }
            t.accept(this, cleanContext)
        }
        context.append(context.listEnclosing.close)
    }

    override fun visit(type: IncompleteMemoryLayoutList, context: PrintContext) {
        context.append(context.listEnclosing.open)
        val cleanContext = context.copy(memoryLayoutSeparator = "", listEnclosing = ListEnclosing.BRACKETS)
        (0..type.knownTypes.lastKey()).map {
            if (it > 0) {
                context.emitMemoryLayoutSeparator()
            }
            type.parameterType(it).accept(this, cleanContext)
        }
        context.append(context.listEnclosing.close)
    }

    override fun visit(type: ExactLayoutName, context: PrintContext) {
        if (type.name != null) {
            context.append("(").append(type.name).append(")")
        }
    }

    override fun visit(type: TopLayoutName, context: PrintContext) {
        context.append("({⊤})")
    }

    override fun visit(type: BotLayoutName, context: PrintContext) {
        context.append("({⊥})")
    }

    override fun visit(type: SequenceElementType, context: PrintContext) {
        when (type.variant) {
            OpenSequenceElementVariant -> context.append("sequenceElement()")
            is SelectingOpenSequenceElementVariant -> context.append("sequenceElement(")
                .append((type.variant.start ?: "?").toString())
                .append(", ")
                .append((type.variant.step ?: "?").toString())
                .append(")")

            is SelectingSequenceElementVariant -> context.append("sequenceElement(")
                .append((type.variant.index ?: "?").toString())
                .append(")")
        }
    }

    override fun visit(type: GroupElementType, context: PrintContext) {
        when (type.variant) {
            is IndexGroupElementVariant -> context.append("groupElement(")
                .append((type.variant.index ?: "index?").toString())
                .append(")")

            is NameGroupElementVariant -> context.append("groupElement(")
                .append((type.variant.name ?: "name?").toString())
                .append(")")

        }
    }

    override fun visit(type: DereferenceElementType, context: PrintContext) {
        context.append("dereferenceElement()")
    }

    override fun visit(type: TopPathElementType, context: PrintContext) {
        context.append("{⊤}")
    }

    override fun visit(type: BotPathElementType, context: PrintContext) {
        context.append("{⊥}")
    }

    override fun visit(type: TopPathElementList, context: PrintContext) {
        context.append("{⊤...}")
    }

    override fun visit(type: BotPathElementList, context: PrintContext) {
        context.append("{⊥...}")
    }

    override fun visit(type: CompletePathElementList, context: PrintContext) {
        context.append(type.typeList.joinToString(separator = "->"))
    }

    override fun visit(type: IncompletePathElementList, context: PrintContext) {
        (0..type.knownTypes.lastKey()).map {
            type.parameterType(it).accept(this, context)
            context.append("->")
        }
        context.append("...")
    }

    override fun visit(type: CompleteFunctionDescriptorType, context: PrintContext) {
        type.parameterTypes.accept(
            this,
            context.copy(memoryLayoutSeparator = "", listEnclosing = ListEnclosing.PARENTHESES)
        )
        type.returnType.accept(this, context)
    }

    override fun visit(type: TopFunctionDescriptorType, context: PrintContext) {
        context.append("⊤")
    }

    override fun visit(type: BotFunctionDescriptorType, context: PrintContext) {
        context.append("⊥")
    }

    override fun visit(type: KnownInvocationBehavior, context: PrintContext) {

    }

    override fun visit(type: TopInvocationBehavior, context: PrintContext) {

    }

    override fun visit(type: BotInvocationBehavior, context: PrintContext) {

    }

}