package de.sirywell.handlehints.mhtype

class MethodHandleInspectionsTest : TypeAnalysisTestBase() {

    fun testVoidInIdentity() = doInspectionTest()

    fun testWrongArgumentTypeInConstant() = doInspectionTest()

    fun testVoidInConstant() = doInspectionTest()

    fun testFinalFields() = doTypeCheckingTest()

    fun testInitialTypes() = doTypeCheckingTest()

    fun testLookupFindConstructor() = doTypeCheckingTest()

    fun testLookupFindGetter() = doTypeCheckingTest()

    fun testLookupFindSetter() = doTypeCheckingTest()

    fun testLookupFindSpecial() = doTypeCheckingTest()

    fun testLookupFindStatic() = doTypeCheckingTest()

    fun testLookupFindStaticGetter() = doTypeCheckingTest()

    fun testLookupFindStaticSetter() = doTypeCheckingTest()

    fun testLookupFindStaticVarHandle() = doTypeCheckingTest()

    fun testLookupFindVarHandle() = doTypeCheckingTest()

    fun testLookupFindVirtual() = doTypeCheckingTest()

    fun testMethodHandlesArrayConstructor() = doTypeCheckingTest()

    fun testMethodHandlesArrayElementGetter() = doTypeCheckingTest()

    fun testMethodHandlesArrayElementSetter() = doTypeCheckingTest()

    fun testMethodHandlesArrayElementVarHandle() = doTypeCheckingTest()

    fun testMethodHandlesArrayLength() = doTypeCheckingTest()

    fun testMethodHandlesCatchException() = doTypeCheckingTest()

    fun testMethodHandlesCollectArguments() = doTypeCheckingTest(true)

    fun testMethodHandlesDropArguments() = doTypeCheckingTest()

    fun testMethodHandlesDropReturn() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesEmpty() = doTypeCheckingTest()

    fun testMethodHandlesIdentity() = doTypeCheckingTest()

    fun testMethodHandlesTableSwitch() = doTypeCheckingTest()

    fun testMethodHandlesThrowException() = doTypeCheckingTest()

    fun testMethodHandlesZero() = doInspectionTest()

    fun testMethodHandleWithVarargs() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeAppendParameterTypes() = doTypeCheckingTest()

    fun testMethodTypeChangeParameterType() = doTypeCheckingTest()

    fun testMethodTypeChangeReturnType() = doTypeCheckingTest()

    fun testMethodTypeCreateBasic() = doTypeCheckingTest()

    fun testMethodTypeCreateWithParameters() = doTypeCheckingTest()

    fun testMethodTypeDropParameterTypes() = doTypeCheckingTest()

    fun testMethodTypeErase() = doTypeCheckingTest()

    fun testMethodTypeGeneric() = doTypeCheckingTest()

    fun testMethodTypeGenericMethodType() = doTypeCheckingTest()

    fun testMethodTypeInsertParameterTypes() = doTypeCheckingTest()

    fun testMethodTypeWrap() = doTypeCheckingTest()

    fun testMethodTypeUnwrap() = doTypeCheckingTest()

}