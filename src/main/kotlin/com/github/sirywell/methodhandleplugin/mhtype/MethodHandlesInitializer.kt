package com.github.sirywell.methodhandleplugin.mhtype

import com.github.sirywell.methodhandleplugin.MethodHandleSignature.Companion.create
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiType

/**
 * Contains methods from [java.lang.invoke.MethodHandles] that create
 * new [java.lang.invoke.MethodHandle]s.
 */
object MethodHandlesInitializer {

    fun arrayConstructor(arrayClass: PsiType): MhType {
        if (arrayClass !is PsiArrayType) return Top
        return MhExactType(create(arrayClass, listOf(PsiType.INT)))
    }

    fun arrayElementGetter(arrayClass: PsiType): MhType {
        return MhExactType(create((arrayClass as PsiArrayType).componentType, listOf<PsiType>(arrayClass, PsiType.INT)))
    }

    fun arrayElementSetter(arrayClass: PsiType): MhType {
        if (arrayClass !is PsiArrayType) return Top
        return MhExactType(create(PsiType.VOID, listOf(arrayClass, PsiType.INT, arrayClass.componentType)))
    }

    // arrayElementVarHandle() no VarHandle support

    fun arrayLength(arrayClass: PsiType): MhType {
        if (arrayClass !is PsiArrayType) return Top
        return MhExactType(create(PsiType.INT, listOf(arrayClass)))
    }

    // byteArray/BufferViewVarHandle() no VarHandle support

    fun constant(type: PsiType): MhType {
        return MhExactType(create(type, listOf()))
    }

    fun empty(mhType: MhType): MhType = mhType

    fun exactInvoker(mhType: MhType, methodHandleType: PsiType) = invoker(mhType, methodHandleType)

    fun identity(type: PsiType): MhType {
        return MhExactType(create(type, listOf(type)))
    }

    fun invoker(mhType: MhType, methodHandleType: PsiType): MhType {
        if (mhType !is MhSingleType) return Top
        val signature = mhType.signature
        val list = signature.parameters.toMutableList()
        list.add(0, methodHandleType)
        return mhType.withSignature(create(signature.returnType, list))
    }

    fun spreadInvoker(type: MhType, leadingArgCount: Int, objectType: PsiType): MhType {
        if (type !is MhSingleType) return Top
        if (leadingArgCount < 0) return Top
        val signature = type.signature
        if (leadingArgCount >= signature.parameters.size) return Top
        val keep = signature.parameters.subList(0, leadingArgCount).toMutableList()
        keep.add(objectType.createArrayType())
        return type.withSignature(create(signature.returnType, keep))
    }

    fun throwException(returnType: PsiType, exType: PsiType): MhType {
        return MhExactType(create(returnType, listOf(exType)))
    }

    fun zero(type: PsiType): MhType {
        return MhExactType(create(type, listOf()))
    }
}
