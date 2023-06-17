package de.sirywell.methodhandleplugin

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSupportTest {

    @Test
    fun testSubListZero() {
        val original = listOf(0, 1, 2)
        assertEquals(original, original.subList(0))
    }

    @Test
    fun testSubListOne() {
        val original = listOf(0, 1, 2)
        assertEquals(listOf(1, 2), original.subList(1))
    }

    @Test
    fun testOutOfBoundsEmpty() {
        listOf<Int>().subList(10)
    }
}