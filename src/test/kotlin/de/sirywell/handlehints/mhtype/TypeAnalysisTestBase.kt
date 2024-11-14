package de.sirywell.handlehints.mhtype

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import de.sirywell.handlehints.inspection.MethodHandleEditInspection

abstract class TypeAnalysisTestBase : LightJavaCodeInsightFixtureTestCase() {

    protected fun doInspectionAndTypeCheckingTest() {
        myFixture.enableInspections(MethodHandleEditInspection())
        myFixture.enableInspections(MethodHandleTypeHelperInspection { true })
        myFixture.testHighlighting(true, true, true, getTestName(false) + ".java")
    }

    protected fun doInspectionTest() {
        myFixture.enableInspections(MethodHandleEditInspection())
        myFixture.testHighlighting(true, false, true, getTestName(false) + ".java")
    }

    protected fun doTypeCheckingTest() {
        doTypeCheckingTest(false)
    }

    protected fun doTypeCheckingTest(onlyAfterCaret: Boolean) {
        val methodHandleTypeHelperInspection =
            if (onlyAfterCaret) MethodHandleTypeHelperInspection { it.textRange.startOffset > myFixture.caretOffset }
            else MethodHandleTypeHelperInspection { true }
        myFixture.enableInspections(methodHandleTypeHelperInspection)
        myFixture.testHighlighting(false, true, false, getTestName(false) + ".java")
    }

    override fun getProjectDescriptor() = JAVA_LATEST_WITH_LATEST_JDK

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

}