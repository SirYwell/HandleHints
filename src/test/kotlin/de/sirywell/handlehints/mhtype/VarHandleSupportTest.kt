package de.sirywell.handlehints.mhtype

import de.sirywell.handlehints.AccessType
import de.sirywell.handlehints.accessTypeForAccessModeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.VarHandle.AccessMode

class VarHandleSupportTest {

    @Test
    fun accessTypeAllCovered() {
        for (value in AccessMode.entries) {
            assertNotNull(accessTypeForAccessModeName(value.name))
        }
    }

    @Test
    fun accessTypesHaveSameMethodType() {
        val mh = MethodHandles.arrayElementVarHandle(IntArray::class.java)
        val types = mutableMapOf<AccessType, MutableSet<MethodType>>()
        for (value in AccessMode.entries) {
            types.computeIfAbsent(accessTypeForAccessModeName(value.name) ?: continue) { mutableSetOf() }
                .add(mh.accessModeType(value))
        }
        types.forEach { (at, methodTypes) -> assertEquals("$at: $methodTypes", 1, methodTypes.size) }
    }
}