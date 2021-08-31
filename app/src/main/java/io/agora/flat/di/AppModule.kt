package io.agora.flat.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.agora.flat.common.android.AndroidClipboardController
import io.agora.flat.common.android.ClipboardController
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.di.impl.EventBus
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 全局
 */
@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class GlobalData

    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @GlobalData
    @Singleton
    @Provides
    fun provideKVCenter(@ApplicationContext context: Context): AppKVCenter {
        return AppKVCenter(context)
    }

    @Singleton
    @Provides
    fun providerAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "flat-database").build()
    }

    @Singleton
    @Provides
    fun provideRoomConfigDao(db: AppDatabase) = db.roomConfigDao()

    @Singleton
    @Provides
    fun providerAppEnv(@ApplicationContext context: Context): AppEnv {
        return AppEnv(context)
    }

    @Singleton
    @Provides
    fun providerEventBus(): EventBus {
        return EventBus()
    }

    @Singleton
    @Provides
    fun providerClipboardController(@ApplicationContext context: Context): ClipboardController {
        return AndroidClipboardController(context)
    }
}