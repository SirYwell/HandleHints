package de.sirywell.handlehints.type

import de.sirywell.handlehints.TriState

@TypeInfo(TopFunctionDescriptorType::class)
sealed interface FunctionDescriptorType : FunctionType<FunctionDescriptorType, MemoryLayoutType> {
    override fun withParameterTypes(parameterTypes: List<MemoryLayoutType>): FunctionDescriptorType {
        return withParameterTypes(CompleteMemoryLayoutList(parameterTypes))
    }

    fun toMethodType(memorySegmentType: Type): MethodHandleType
}

data object BotFunctionDescriptorType : FunctionDescriptorType, BotTypeLatticeElement<FunctionDescriptorType> {
    override fun joinIdentical(other: FunctionDescriptorType) = other to TriState.UNKNOWN

    override fun withReturnType(returnType: MemoryLayoutType) =
        CompleteFunctionDescriptorType(parameterTypes, returnType)

    override fun withParameterTypes(parameterTypes: MemoryLayoutList) =
        CompleteFunctionDescriptorType(parameterTypes, returnType)

    override fun toMethodType(memorySegmentType: Type) = BotMethodHandleType

    override fun parameterTypeAt(index: Int) = BotMemoryLayoutType

    override val returnType get() = BotMemoryLayoutType

    override val parameterTypes get() = BotMemoryLayoutList

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}


data object TopFunctionDescriptorType : FunctionDescriptorType, TopTypeLatticeElement<FunctionDescriptorType> {
    override fun joinIdentical(other: FunctionDescriptorType) = this to TriState.UNKNOWN

    override fun withReturnType(returnType: MemoryLayoutType) = this

    override fun withParameterTypes(parameterTypes: MemoryLayoutList) = this

    override fun toMethodType(memorySegmentType: Type) = TopMethodHandleType

    override fun parameterTypeAt(index: Int) = TopMemoryLayoutType

    override val returnType get() = TopMemoryLayoutType

    override val parameterTypes get() = TopMemoryLayoutList

    override fun self() = this
    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C) = visitor.visit(this, context)
}

data class CompleteFunctionDescriptorType(
    override val parameterTypes: TypeLatticeElementList<MemoryLayoutType>,
    override val returnType: MemoryLayoutType
) : FunctionDescriptorType, CompleteFunctionType<FunctionDescriptorType, MemoryLayoutType> {
    override fun withParameterTypes(parameterTypes: List<MemoryLayoutType>): FunctionDescriptorType {
        return withParameterTypes(CompleteMemoryLayoutList(parameterTypes))
    }

    override fun toMethodType(memorySegmentType: Type): MethodHandleType {
        return CompleteMethodHandleType(
            memoryLayoutToType(returnType, memorySegmentType),
            mapMemoryLayoutListToTypeList(parameterTypes, memorySegmentType),
            TriState.NO
        )
    }

    override fun <C, R> accept(visitor: TypeVisitor<C, R>, context: C): R {
        return visitor.visit(this, context)
    }

    override fun copy(
        returnType: MemoryLayoutType,
        parameterTypes: TypeLatticeElementList<MemoryLayoutType>
    ): FunctionDescriptorType {
        return CompleteFunctionDescriptorType(parameterTypes, returnType)
    }
}

private fun mapMemoryLayoutListToTypeList(parameterTypes: MemoryLayoutList, memorySegmentType: Type): TypeList {
    return when (parameterTypes) {
        is BotTypeLatticeElementList -> BotTypeList
        is TopTypeLatticeElementList -> TopTypeList
        is CompleteTypeLatticeElementList -> {
            val params = parameterTypes.typeList.map { memoryLayoutToType(it, memorySegmentType) }
            CompleteTypeList(params)
        }

        is IncompleteTypeLatticeElementList -> {
            val params = parameterTypes.knownTypes.mapValues { (_, v) -> memoryLayoutToType(v, memorySegmentType) }
            IncompleteTypeList(params.toSortedMap())
        }
    }
}

private fun memoryLayoutToType(layoutType: MemoryLayoutType, memorySegmentType: Type): Type {
    return when (layoutType) {
        BotMemoryLayoutType -> BotType
        TopMemoryLayoutType -> TopType
        is StructLayoutType,
        is UnionLayoutType,
        is PaddingLayoutType,
        is SequenceLayoutType,
        is AddressLayoutType -> memorySegmentType

        is NormalValueLayoutType -> layoutType.type
    }
}
