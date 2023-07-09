package de.sirywell.methodhandleplugin.mhtype

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import de.sirywell.methodhandleplugin.inspection.MethodHandleCreationInspection

class MethodHandleInspectionsTest : LightJavaCodeInsightFixtureTestCase() {

    private fun doInspectionTest() {
        myFixture.enableInspections(MethodHandleCreationInspection())
        myFixture.testHighlighting(true, false, true, getTestName(false) + ".java")
    }

    private fun doTypeCheckingTest() {
        doTypeCheckingTest(false)
    }

    private fun doTypeCheckingTest(onlyAtCaret: Boolean) {
        val methodHandleTypeHelperInspection =
            if (onlyAtCaret) MethodHandleTypeHelperInspection { it == myFixture.elementAtCaret }
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

    fun testSimpleIdentity() = doTypeCheckingTest()

    fun testMethodTypeAppendParameterTypes() = doTypeCheckingTest()

    fun testMethodTypeCreateBasic() = doTypeCheckingTest()

    fun testMethodTypeCreateWithParameters() = doTypeCheckingTest()

    fun testMethodTypeGenericMethodType() = doTypeCheckingTest()

    fun testMethodTypeWrap() = doTypeCheckingTest()

    fun testMethodTypeUnwrap() = doTypeCheckingTest()

}