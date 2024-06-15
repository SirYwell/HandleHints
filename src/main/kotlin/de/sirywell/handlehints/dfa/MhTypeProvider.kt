package de.sirywell.handlehints.dfa

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.ParameterizedCachedValue
import com.intellij.psi.util.ParameterizedCachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import de.sirywell.handlehints.TypeData

object MhTypeProvider : ParameterizedCachedValueProvider<TypeData, PsiFile> {
    override fun compute(param: PsiFile): CachedValueProvider.Result<TypeData> {
        return CachedValueProvider.Result.create(MethodHandleElementVisitor().scan(param), PsiModificationTracker.MODIFICATION_COUNT)
    }

    val CACHE_KEY = Key<ParameterizedCachedValue<TypeData, PsiFile>>("HandleHints.Cache")
}
