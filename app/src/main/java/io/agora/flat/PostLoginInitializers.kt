package io.agora.flat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.di.interfaces.PostLoginInitializer
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostLoginInitializers @Inject constructor(
    @ApplicationContext val context: Context,
    private val initializers: Set<@JvmSuppressWildcards PostLoginInitializer>,
) {
    private var inited = false

    fun init() {
        if (inited) return
        initializers.forEach {
            it.init(context)
        }
        inited = true
    }
}
