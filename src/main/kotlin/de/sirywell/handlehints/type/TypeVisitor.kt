package de.sirywell.handlehints.type

interface TypeVisitor<C, R> {

    fun visit(type: BotType, context: C): R
    fun visit(type: TopType, context: C): R
    fun visit(type: ExactType, context: C): R
    fun visit(type: TopTypeList, context: C): R
    fun visit(type: BotTypeList, context: C): R
    fun visit(type: CompleteTypeList, context: C): R
    fun visit(type: IncompleteTypeList, context: C): R
    fun visit(type: BotVarHandleType, context: C): R
    fun visit(type: TopVarHandleType, context: C): R
    fun visit(type: CompleteVarHandleType, context: C): R
    fun visit(type: BotMethodHandleType, context: C): R
    fun visit(type: TopMethodHandleType, context: C): R
    fun visit(type: CompleteMethodHandleType, context: C): R
    fun visit(type: BotMemoryLayoutType, context: C): R
    fun visit(type: TopMemoryLayoutType, context: C): R
    fun visit(type: ValueLayoutType, context: C): R
    fun visit(type: StructLayoutType, context: C): R
    fun visit(type: PaddingLayoutType, context: C): R
    fun visit(type: TopMemoryLayoutList, context: C): R
    fun visit(type: BotMemoryLayoutList, context: C): R
    fun visit(type: CompleteMemoryLayoutList, context: C): R
    fun visit(type: IncompleteMemoryLayoutList, context: C): R
}