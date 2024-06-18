package de.sirywell.handlehints

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValuesManager
import de.sirywell.handlehints.dfa.MhTypeProvider
import de.sirywell.handlehints.type.MethodHandleType
import de.sirywell.handlehints.type.TypeLatticeElement

class TypeData : ModificationTracker by ModificationTracker.NEVER_CHANGED {
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

    private val map = mutableMapOf<PsiElement, TypeLatticeElement<*>>()
    private val problems = mutableMapOf<PsiElement, (ProblemsHolder) -> Unit>()

    inline operator fun <reified T : TypeLatticeElement<*>> invoke(element: PsiElement) = get(element) as? T
    operator fun get(element: PsiElement) = map[element]
    operator fun set(element: PsiElement, type: TypeLatticeElement<*>) {
        map[element] = type
    }

    fun reportProblem(element: PsiElement, reporter: (ProblemsHolder) -> Unit) {
        problems[element] = reporter
    }

    fun problemFor(element: PsiElement): ((ProblemsHolder) -> Unit)? = problems[element]
}
