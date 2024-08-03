package de.sirywell.handlehints.mhtype

class MemoryLayoutTypeTest : TypeAnalysisTestBase() {

    fun testMemoryLayoutWithByteAlignment() = doTypeCheckingTest()

    fun testMemoryLayoutStructLayout() = doTypeCheckingTest()

    fun testMemoryLayoutPaddingLayout() = doTypeCheckingTest()

    fun testMemoryLayoutUnionLayout() = doTypeCheckingTest()

    fun testMemoryLayoutSequenceLayout() = doTypeCheckingTest()

    fun testMemoryLayoutVarHandle() = doInspectionAndTypeCheckingTest()

    // Java 22 not supported on 2023.3, but arrayElementVarHandle/scaleHandle methods are Java 22+
    // fun testMemoryLayoutScaleHandle() = doTypeCheckingTest()
    // fun testMemoryLayoutArrayElementVarHandle() = doTypeCheckingTest()

    fun testMemoryLayoutWithName() = doTypeCheckingTest()


    fun testMemoryLayoutStructLayoutInspection() = doInspectionTest()
}