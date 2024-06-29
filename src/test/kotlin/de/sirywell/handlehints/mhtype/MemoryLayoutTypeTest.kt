package de.sirywell.handlehints.mhtype

class MemoryLayoutTypeTest : TypeAnalysisTestBase() {

    fun testMemoryLayoutWithByteAlignment() = doTypeCheckingTest()

    fun testMemoryLayoutStructLayout() = doTypeCheckingTest()

    fun testMemoryLayoutPaddingLayout() = doTypeCheckingTest()


    fun testMemoryLayoutStructLayoutInspection() = doInspectionTest()
}