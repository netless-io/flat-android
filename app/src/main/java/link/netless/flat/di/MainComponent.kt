package link.netless.flat.di

import dagger.Component
import link.netless.flat.http.HeaderProvider
import javax.inject.Singleton

@Component(modules = [UserModule::class])
@Singleton
interface MainComponent {
    // 提供Http请求头
    fun headerProviders(): Set<HeaderProvider>
}