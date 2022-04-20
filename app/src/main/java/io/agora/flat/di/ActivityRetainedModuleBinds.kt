package io.agora.flat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import io.agora.flat.common.board.BoardRoom
import io.agora.flat.di.interfaces.IBoardRoom

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class ActivityRetainedModuleBinds {
    @Binds
    abstract fun providerBoardRoom(bind: BoardRoom): IBoardRoom


}