package de.sirywell.handlehints.mhtype

class FunctionDescriptorTest : TypeAnalysisTestBase() {

    fun testFunctionDescriptorMethods() = doTypeCheckingTest()

    fun testLinkerDowncallHandle() = doInspectionAndTypeCheckingTest()
}