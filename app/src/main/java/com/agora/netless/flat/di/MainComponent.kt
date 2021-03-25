package com.agora.netless.flat.di

import com.agora.netless.flat.http.HeaderProvider
import dagger.Component
import javax.inject.Singleton

@Component(modules = [UserModule::class])
@Singleton
interface MainComponent {
    // 提供Http请求头
    fun headerProviders(): Set<HeaderProvider>
}