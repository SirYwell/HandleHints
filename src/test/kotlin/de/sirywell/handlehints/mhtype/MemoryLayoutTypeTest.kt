package de.sirywell.handlehints.mhtype

class MemoryLayoutTypeTest : TypeAnalysisTestBase() {

    fun testMemoryLayoutWithByteAlignment() = doTypeCheckingTest()
}