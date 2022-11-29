package de.sirywell.methodhandleplugin.dfa

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.ParameterizedCachedValue
import com.intellij.psi.util.ParameterizedCachedValueProvider
import de.sirywell.methodhandleplugin.TypeData

object MhTypeProvider : ParameterizedCachedValueProvider<TypeData, PsiFile> {
    override fun compute(param: PsiFile): CachedValueProvider.Result<TypeData> {
        return CachedValueProvider.Result.create(MethodHandleElementVisitor().scan(param), ModificationTracker.EVER_CHANGED)
    }

    val CACHE_KEY = Key<ParameterizedCachedValue<TypeData, PsiFile>>("HandleHints.Cache")
}
