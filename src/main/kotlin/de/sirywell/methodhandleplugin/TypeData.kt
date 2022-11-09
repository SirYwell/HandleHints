package de.sirywell.methodhandleplugin

import de.sirywell.methodhandleplugin.mhtype.MhType
import com.intellij.psi.PsiElement
import com.jetbrains.rd.util.concurrentMapOf

object TypeData {
    private val map = concurrentMapOf<PsiElement, MhType>()

    operator fun get(element: PsiElement) = map[element]
    operator fun set(element: PsiElement, type: MhType) {
        map[element] = type
    }
}
