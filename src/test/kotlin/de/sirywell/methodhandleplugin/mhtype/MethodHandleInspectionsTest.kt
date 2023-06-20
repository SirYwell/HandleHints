package de.sirywell.methodhandleplugin.mhtype

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import de.sirywell.methodhandleplugin.inspection.MethodHandleCreationInspection

class MethodHandleInspectionsTest : LightJavaCodeInsightFixtureTestCase() {

    private fun doTest() {
        projectDescriptor.sdk
        myFixture.enableInspections(MethodHandleCreationInspection())
        myFixture.testHighlighting(true, false, true, getTestName(false) + ".java")
    }

    override fun getProjectDescriptor() = JAVA_LATEST_WITH_LATEST_JDK

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    fun testVoidInIdentity() {
        doTest()
        fail("DOES THIS FAIL?")
    }


}