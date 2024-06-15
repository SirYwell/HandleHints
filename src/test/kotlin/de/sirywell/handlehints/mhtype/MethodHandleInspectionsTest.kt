package de.sirywell.handlehints.mhtype

import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import de.sirywell.handlehints.inspection.MethodHandleEditInspection

class MethodHandleInspectionsTest : LightJavaCodeInsightFixtureTestCase() {

    private fun doInspectionTest() {
        myFixture.enableInspections(MethodHandleEditInspection())
        myFixture.testHighlighting(true, false, true, getTestName(false) + ".java")
    }

    private fun doTypeCheckingTest() {
        doTypeCheckingTest(false)
    }

    private fun doTypeCheckingTest(onlyAfterCaret: Boolean) {
        val methodHandleTypeHelperInspection =
            if (onlyAfterCaret) MethodHandleTypeHelperInspection { it.startOffset > myFixture.caretOffset }
            else MethodHandleTypeHelperInspection { true }
        myFixture.enableInspections(methodHandleTypeHelperInspection)
        myFixture.testHighlighting(false, true, false, getTestName(false) + ".java")
    }

    override fun getProjectDescriptor() = JAVA_LATEST_WITH_LATEST_JDK

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    fun testVoidInIdentity() = doInspectionTest()

    fun testWrongArgumentTypeInConstant() = doInspectionTest()

    fun testVoidInConstant() = doInspectionTest()

    fun testLookupFindConstructor() = doTypeCheckingTest()

    fun testLookupFindGetter() = doTypeCheckingTest()

    fun testLookupFindSetter() = doTypeCheckingTest()

    fun testLookupFindSpecial() = doTypeCheckingTest()

    fun testLookupFindStatic() = doTypeCheckingTest()

    fun testLookupFindStaticGetter() = doTypeCheckingTest()

    fun testLookupFindStaticSetter() = doTypeCheckingTest()

    fun testLookupFindVirtual() = doTypeCheckingTest()

    fun testMethodHandlesArrayConstructor() = doTypeCheckingTest()

    fun testMethodHandlesArrayElementGetter() = doTypeCheckingTest()

    fun testMethodHandlesArrayElementSetter() = doTypeCheckingTest()

    fun testMethodHandlesArrayLength() = doTypeCheckingTest()

    fun testMethodHandlesCatchException() = doTypeCheckingTest()

    fun testMethodHandlesCollectArguments() = doTypeCheckingTest(true)

    fun testMethodHandlesDropArguments() = doTypeCheckingTest()

    fun testMethodHandlesDropReturn() = doTypeCheckingTest()

    fun testMethodHandlesEmpty() = doTypeCheckingTest()

    fun testMethodHandlesIdentity() = doTypeCheckingTest()

    fun testMethodHandlesTableSwitch() = doTypeCheckingTest()

    fun testMethodHandlesThrowException() = doTypeCheckingTest()

    fun testMethodHandlesZero() = doInspectionTest()

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