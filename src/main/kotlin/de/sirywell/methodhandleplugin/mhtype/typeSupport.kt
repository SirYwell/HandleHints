package de.sirywell.methodhandleplugin.mhtype

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType
import de.sirywell.methodhandleplugin.MethodHandleBundle

fun typeMustNotBe(expression: PsiExpression, type: PsiType): MhType {
    return Top.inspect(
        MethodHandleBundle.message(
            "problem.merging.general.typeMustNotBe",
            type.presentableText
        ),
        expression
    )
}

fun unexpectedReturnType(returnType: PsiType, expected: PsiType): MhType {
    return Top.inspect(
        MethodHandleBundle.message(
            "problem.merging.general.otherReturnTypeExpected",
            returnType.presentableText,
            expected.presentableText
        )
    )
}
fun referenceTypeExpected(expression: PsiExpression, type: PsiType): MhType {
    return Top.inspect(
        MethodHandleBundle.message(
            "problem.merging.general.referenceTypeExpected",
            type.presentableText,
        ),
        expression
    )
}