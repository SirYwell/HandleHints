package de.sirywell.methodhandleplugin

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValuesManager
import de.sirywell.methodhandleplugin.dfa.MhTypeProvider
import de.sirywell.methodhandleplugin.mhtype.MhType

class TypeData: ModificationTracker by ModificationTracker.NEVER_CHANGED {
    companion object {
        fun forFile(file: PsiFile): TypeData {
            return CachedValuesManager.getManager(file.project)
                .getParameterizedCachedValue(
                    file,
                    MhTypeProvider.CACHE_KEY,
                    MhTypeProvider,
                    true,
                    file
                )
        }
    }
    private val map = mutableMapOf<PsiElement, MhType>()

    operator fun get(element: PsiElement) = map[element]
    operator fun set(element: PsiElement, type: MhType) {
        map[element] = type
    }
}
