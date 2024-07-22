package de.sirywell.handlehints.mhtype

class MemoryLayoutTypeTest : TypeAnalysisTestBase() {

    fun testMemoryLayoutWithByteAlignment() = doTypeCheckingTest()

    fun testMemoryLayoutStructLayout() = doTypeCheckingTest()

    fun testMemoryLayoutPaddingLayout() = doTypeCheckingTest()

    fun testMemoryLayoutUnionLayout() = doTypeCheckingTest()

    fun testMemoryLayoutSequenceLayout() = doTypeCheckingTest()

    fun testMemoryLayoutVarHandle() = doInspectionAndTypeCheckingTest()

    fun testMemoryLayoutWithName() = doTypeCheckingTest()


    fun testMemoryLayoutStructLayoutInspection() = doInspectionTest()
}