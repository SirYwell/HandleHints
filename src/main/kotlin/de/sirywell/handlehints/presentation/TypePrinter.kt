package de.sirywell.handlehints.presentation

import de.sirywell.handlehints.type.*

class TypePrinter : TypeVisitor<StringBuilder, Unit> {

    fun print(type: TypeLatticeElement<*>): String {
        val stringBuilder = StringBuilder()
        type.accept(this, stringBuilder)
        return stringBuilder.toString()
    }

    override fun visit(type: BotType, context: StringBuilder) {
        context.append("⊥")
    }

    override fun visit(type: TopType, context: StringBuilder) {
        context.append("⊤")
    }

    override fun visit(type: ExactType, context: StringBuilder) {
        context.append(type.psiType.presentableText)
    }

    override fun visit(type: TopTypeList, context: StringBuilder) {
        context.append("({⊤})")
    }

    override fun visit(type: BotTypeList, context: StringBuilder) {
        context.append("({⊥})")
    }

    override fun visit(type: CompleteTypeList, context: StringBuilder) {
        context.append("(")
        type.typeList.forEachIndexed { index, t ->
            if (index != 0) {
                context.append(",")
            }
            t.accept(this, context)
        }
        context.append(")")
    }

    override fun visit(type: IncompleteTypeList, context: StringBuilder) {
        context.append("(")
        type.knownTypes.onEachIndexed { index, (pos, t) ->
            if (index != 0) {
                context.append(",")
            }
            context.append(pos).append("=")
            t.accept(this, context)
        }
        context.append(")")
    }

    override fun visit(type: BotVarHandleType, context: StringBuilder) {
        context.append("⊥")
    }

    override fun visit(type: TopVarHandleType, context: StringBuilder) {
        context.append("⊤")
    }

    override fun visit(type: CompleteVarHandleType, context: StringBuilder) {
        type.coordinateTypes.accept(this, context)
        context.append("(")
        type.variableType.accept(this, context)
        context.append(")")
    }

    override fun visit(type: BotMethodHandleType, context: StringBuilder) {
        context.append("⊥")
    }

    override fun visit(type: TopMethodHandleType, context: StringBuilder) {
        context.append("⊤")
    }

    override fun visit(type: CompleteMethodHandleType, context: StringBuilder) {
        type.parameterTypes.accept(this, context)
        type.returnType.accept(this, context)
    }

    override fun visit(type: BotMemoryLayoutType, context: StringBuilder) {
        context.append("⊥")
    }

    override fun visit(type: TopMemoryLayoutType, context: StringBuilder) {
        context.append("⊤")
    }

    override fun visit(type: ValueLayoutType, context: StringBuilder) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.type.accept(this, context)
        context.append(type.byteSize ?: "?")
    }

    override fun visit(type: StructLayoutType, context: StringBuilder) {
        if (type.byteSize == null || type.byteSize != type.byteAlignment) {
            context.append(type.byteAlignment ?: "?").append("%")
        }
        type.memberLayouts.accept(this, context)
    }

    override fun visit(type: PaddingLayoutType, context: StringBuilder) {
        context.append("x").append(type.byteSize ?: "?")
    }

    override fun visit(type: TopMemoryLayoutList, context: StringBuilder) {
        context.append("[{⊤}]")
    }

    override fun visit(type: BotMemoryLayoutList, context: StringBuilder) {
        context.append("[{⊥}]")
    }

    override fun visit(type: CompleteMemoryLayoutList, context: StringBuilder) {
        context.append("[")
        type.typeList.forEach { it.accept(this, context) }
        context.append("]")
    }

    override fun visit(type: IncompleteMemoryLayoutList, context: StringBuilder) {
        context.append("[")
        (0..type.knownTypes.lastKey()).map { type.parameterType(it) }.forEach { it.accept(this, context) }
        context.append("]")
    }

}