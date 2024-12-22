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

    fun testMethodHandlesByteArrayViewVarHandle() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesByteBufferViewVarHandle() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesCatchException() = doTypeCheckingTest()

    fun testMethodHandlesCollectArguments() = doTypeCheckingTest(true)

    fun testMethodHandlesConstantWithZero() = doInspectionTest()

    fun testMethodHandlesDropArguments() = doTypeCheckingTest()

    fun testMethodHandlesDropReturn() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesEmpty() = doTypeCheckingTest()

    fun testMethodHandlesFilterReturnValue() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesIdentity() = doTypeCheckingTest()

    fun testMethodHandlesTableSwitch() = doTypeCheckingTest()

    fun testMethodHandlesThrowException() = doTypeCheckingTest()

    fun testMethodHandlesTryFinally() = doInspectionAndTypeCheckingTest()

    fun testMethodHandlesZero() = doInspectionTest()

    fun testMethodHandleAsType() = doInspectionAndTypeCheckingTest()

    fun testMethodHandleWithVarargs() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeAppendParameterTypes() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeChangeParameterType() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeChangeReturnType() = doTypeCheckingTest()

    fun testMethodTypeCreateBasic() = doTypeCheckingTest()

    fun testMethodTypeCreateWithParameters() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeDropParameterTypes() = doTypeCheckingTest()

    fun testMethodTypeErase() = doTypeCheckingTest()

    fun testMethodTypeGeneric() = doTypeCheckingTest()

    fun testMethodTypeGenericMethodType() = doTypeCheckingTest()

    fun testMethodTypeInsertParameterTypes() = doInspectionAndTypeCheckingTest()

    fun testMethodTypeWrap() = doTypeCheckingTest()

    fun testMethodTypeUnwrap() = doTypeCheckingTest()

}